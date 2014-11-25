/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uma.wdi.fusion.resolution;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Node;

import uma.wdi.fusion.input.Dataset;
import uma.wdi.fusion.utils.Pair;

/* Conflict resolution function that selects all the values from the most recent dataset(s) 
 * (looking at "date" filed in the provenance element)
 * if selectFirst is true, returns the first of the values from the most recent dataset(s)
 * 
 * Same logic as in MostTrusted
 * 
 * @author Volha
 * */
public class MostRecent extends AbstractResolutionFunction
{
	private boolean selectFirst = false; // false by default
	
	public MostRecent ()
	{
		selectFirst = false;
	}

	public MostRecent (boolean _selectFirst)
	{
		selectFirst = _selectFirst;
	}

	// Select all values from the most trusted dataset(s)
	@Override
	public Set<Pair> applyStrategy(Dataset ds, Node node, String path)
	{
		// get data to fuse:
		Set<Pair> pairs = getValueProvevancePairs(node, path);
		
		// select most recent value
		Set<Pair> result = new HashSet<Pair>();
		Date mostRecentDate = null;
		String sMostRecentDate = null;
		for (Pair p : pairs)
		{
			String sDate = ds.getProvenanceAttribute(p.provenance, "date");
			try 
			{
				// NOTE: assumes specific date format (see movie example datasets)
				Date date = new SimpleDateFormat("d MMM yyyy HH:mm:ss Z", Locale.ENGLISH).parse(sDate);
				if ((mostRecentDate == null) || (date.compareTo(mostRecentDate) > 0))
				{
					mostRecentDate = date;
					sMostRecentDate = sDate;
				}				
			} 
			catch (ParseException e) 
			{
				e.printStackTrace();
			}
		}

		// collect all most recent values
		Map<String,String> valueMap= new HashMap<String,String>();
		for (Pair p : pairs)
		{
			String sDate = ds.getProvenanceAttribute(p.provenance, "date");
			if (sDate.equals(sMostRecentDate))
			{
				//than add a value to a value map
				if (valueMap.containsKey(p.value))
					valueMap.put(p.value, valueMap.get(p.value)+","+p.provenance);
				else
					valueMap.put(p.value, p.provenance);			
			}
		}

		// insert the resulting values:
		if (selectFirst)
		{
			// insert the first value
			String v = valueMap.entrySet().iterator().next().getKey();
			result.add(new Pair(v,valueMap.get(v)));
		}
		else
		{
			// insert them all
			for (String v : valueMap.keySet()) result.add(new Pair(v,valueMap.get(v)));
		}

		return result;
	}
}

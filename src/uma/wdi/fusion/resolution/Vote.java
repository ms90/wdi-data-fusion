package uma.wdi.fusion.resolution;

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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Node;

import uma.wdi.fusion.input.Dataset;
import uma.wdi.fusion.utils.Pair;

/* Conflict resolution function that selects the most frequent value among available ones
 * if selectFirst is true, returns the first of the values from the most recent dataset(s)
 * 
 * Similar logic as in MostTrusted
 * 
 * @author Volha
 * */
public class Vote extends AbstractResolutionFunction
{
	private boolean selectFirst = false; // false by default
	
	public Vote ()
	{
		selectFirst = false;
	}

	public Vote (boolean _selectFirst)
	{
		selectFirst = _selectFirst;
	}

	// Select all most frequent values
	@Override
	public Set<Pair> applyStrategy(Dataset ds, Node node, String path)
	{
		Set<Pair> result = new HashSet<Pair>();

		// get data to fuse:
		Set<Pair> pairs = getValueProvenancePairs(node, path);
		
		// get value counts
		Map<String,Integer> counts = new HashMap<String,Integer>();
		for (Pair p : pairs)
		{
			if (counts.containsKey(p.value)) counts.put(p.value, counts.get(p.value)+1);
			else counts.put(p.value, 1);
		}
		int maxCount = 0;
		for (String v : counts.keySet())
		{
			if (counts.get(v) > maxCount) maxCount = counts.get(v);
		}

		// collect all most frequent values
		Map<String,String> valueMap= new HashMap<String,String>();
		for (Pair p : pairs)
		{
			if (counts.get(p.value) == maxCount)
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

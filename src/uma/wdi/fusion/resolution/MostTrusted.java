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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.w3c.dom.Node;

import uma.wdi.fusion.input.Dataset;
import uma.wdi.fusion.utils.Pair;
import uma.wdi.fusion.utils.XMLUtils;

/* Conflict resolution function that takes as input an array of trust values for dataset ("source" filed in the provenance element)
 * and selects all the values from a dataset(s) with the highest trust values
 * if selectFirst is true, returns the first of the values from the most trusted dataset(s)
 * 
 * Same logic as in MostRecent
 * 
 * @author Volha
 * */
public class MostTrusted extends AbstractResolutionFunction
{
	private Map<String,Double> trust = new HashMap<String,Double>();
	private boolean selectFirst = false; // false by default
	
	public MostTrusted (Map<String,Double> _trust)
	{
		trust = _trust;
		selectFirst = false;
	}

	public MostTrusted (Map<String,Double> _trust, boolean _selectFirst)
	{
		selectFirst = _selectFirst;
		trust = _trust;
	}
	
	// Select all values from the most trusted dataset(s)
	@Override
	public Set<Pair> applyStrategy(Dataset ds, Node node, String path)
	{
		// get data to fuse:
		Set<Pair> pairs = getValueProvevancePairs(node, path);
				
		// select most recent value
		Set<Pair> result = new HashSet<Pair>();
		Double maxRank = null;
		for (Pair p : pairs)
		{
			String sSource = ds.getProvenanceAttribute(p.provenance, "source");
			Double r = trust.get(sSource);			
			if ((maxRank == null) || (r > maxRank)) maxRank = r;
		}

		// collect all most recent values
		Map<String,String> valueMap= new HashMap<String,String>();
		for (Pair p : pairs)
		{
			String sSource = ds.getProvenanceAttribute(p.provenance, "source");
			if (trust.get(sSource).equals(maxRank))
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

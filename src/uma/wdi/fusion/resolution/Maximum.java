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

import java.util.HashSet;
import java.util.Set;
import org.w3c.dom.Node;

import uma.wdi.fusion.input.Dataset;
import uma.wdi.fusion.utils.Pair;

/* Conflict resolution function that returns the maximum of initial values, collecting all provenance IDs for which values are the same
 * In case some values are non-numeric, no changes to all values of a node are made
 * 
 * @author Volha
 * */
public class Maximum extends AbstractResolutionFunction
{
	// Computes and return the maximum value with the (list of) respective provenance id(s)
	@Override
	public Set<Pair> applyStrategy(Dataset ds, Node node, String path)
	{
		// get data to fuse:
		Set<Pair> pairs = getValueProvenancePairs(node, path);
		
		// select maximum
		Set<Pair> result = new HashSet<Pair>();
		Double max = null;
		String maxVal = null;
		String maxProv = null;
		for (Pair p : pairs)
		{
			Double v = 0.0;
			try 
			{
				v = Double.valueOf(p.value);
			}
			catch(NumberFormatException e)
			{
				System.out.println("Maximum fusion policy cannot be applied to a non-numeric value " + p.value + " of attribute " + path);
				return pairs;
			}
			if (max == null || max < v) 
			{
				max = v;
				maxVal = p.value;
				maxProv = p.provenance;
			}			
		}		
		
		// collect all provenace values relevant for this value
		maxProv = getProvenanceList(pairs,maxVal);
		// add to a resulting set
		result.add(new Pair(maxVal,maxProv));		

		return result;
	}

}

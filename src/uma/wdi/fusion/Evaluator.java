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

package uma.wdi.fusion;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import uma.wdi.fusion.resolution.AbstractResolutionFunction;
import uma.wdi.fusion.utils.NodeMap;
import uma.wdi.fusion.utils.XMLUtils;
import uma.wdi.fusion.input.Dataset;

/* Applies a set of conflict resolution strategies for different properties to a merged dataset (produced from DataUnion)
*  Evaluate the result with respect to the gold standard, calculates overall and per attribute accuracy	
*  Writes the result to an input file 
* 
*  @author Volha
* */
public class Evaluator 
{
	private Map<String,Double> attrAcc = new HashMap<String,Double>();
	private Double accuracy = 0.0;

	// Getters
	public double GetAccuracy()
	{
		return accuracy;
	}
	public Map<String,Double> GetAccuracyPerAttribute()
	{
		return attrAcc;
	}
	
	
	/* Applies a set of conflict resolution strategies for different properties to a merged dataset (produced from DataUnion)
	*  fp hash map: path-->resolution function) 
	*  Writes the result to an input file
	*  Evaluate the result with respect to the gold standard, calculates overall and per attribute accuracy
	*  */
	public boolean evaluate(Map<String, AbstractResolutionFunction> fp, Dataset ds, String idPath, Set<String> attributes, String fnGold, String fnOutput)
	{
		Document xmlDoc = null;
		for (Entry<String, Node> entryDS : ds.getData().getEntrySet())
		{
			Node node = entryDS.getValue();
			xmlDoc = node.getOwnerDocument();
			for (Entry<String, AbstractResolutionFunction> entryFP : fp.entrySet())
			{
				String path = entryFP.getKey();
				AbstractResolutionFunction policy = entryFP.getValue();
				policy.resolve(ds, node, path);
			}
		}
		// print to file
		XMLUtils.printNode(xmlDoc, fnOutput);

		// compare to gold standard
		NodeMap gold = new NodeMap();
		gold.loadFromFile(fnGold, idPath, true);

		for (String attr : attributes) attrAcc.put(attr, 0.0);

		Double nodeCnt = 0.0;
		Double totalCnt = 0.0;
		for (Entry<String, Node> entryDS : ds.getData().getEntrySet())
		{
			String id = entryDS.getKey();
			Node node = entryDS.getValue();
			Node goldNode = gold.getNode(id);
			if (goldNode != null)
			{
				nodeCnt++;
				for (String attr : attributes)
				{
					// ignore ID attribute
					if (!attr.equals(gold.getIDAttribute()))
					{
						boolean eq = true;
						Collection<String> vals = XMLUtils.getValueUnion(node, attr);
						Collection<String> goldVals = XMLUtils.getValueUnion(goldNode, attr);
						if (vals.size() != goldVals.size()) eq = false; // TODO: remove this condition if you want list intersection to be a match (e.g. if you want "London" and "UK" in your data and "London" in the gold standard to be considered a match) 
						else
						{
							Collection<Double> goldValsNUM = new ArrayList<Double>(); // to compare numeric values
							for (String gv : goldVals)
							{
								Double gvd = toNumber(gv);
								if (goldValsNUM != null && gvd != null) 
									goldValsNUM.add(gvd);
								else
								{
									goldValsNUM.clear();
									goldValsNUM = null;
								}
							}
							for (String s : vals)
							{
								if (goldValsNUM != null)
								{
									if (!goldValsNUM.contains(toNumber(s))) eq = false;
								}
								else if (!goldVals.contains(s)) eq = false;
							}
						}
						if (eq)
						{
							if (attrAcc.containsKey(attr)) attrAcc.put(attr, attrAcc.get(attr)+1.0);
							else attrAcc.put(attr, 1.0);
							totalCnt++;
						}						
						// System.out.println(id + ", " + attr + " : " + eq);
					}
				}
			}
		}
	
		System.out.println("Number of entities that were evaluated with respect to gold standard is " + nodeCnt.intValue() + 
				".\nIf you think it should be higher - check IDs in your gold standard!\n");
		
		// overall accuracy
		if (totalCnt == 0.0 || nodeCnt == 0.0) accuracy = 0.0;
		else accuracy = totalCnt/((attributes.size()-1)*nodeCnt);
		// System.out.println("overall accuracy : " + accuracy);
		// per attribute accuracy
		for (String attr : attrAcc.keySet())
		{
			// ignore ID attribute
			if (!attr.equals(gold.getIDAttribute()))
			{
				if (nodeCnt == 0.0) attrAcc.put(attr,0.0);
				else attrAcc.put(attr, attrAcc.get(attr)/nodeCnt);
				// System.out.println("accuracy for " + attr + " : " + attrAcc.get(attr));
			}
		}
		return true;
	}
	
	private static Double toNumber(String str)  
	{  
	  Double d = null;
	  try  
	  {  
	    d = Double.parseDouble(str);  
	  }  
	  catch(NumberFormatException nfe)  
	  {  
	    return null;  
	  }  
	  return d;  
	}
}

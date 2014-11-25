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

package uma.wdi.fusion.input;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uma.wdi.fusion.utils.NodeMap;
import uma.wdi.fusion.utils.Pair;
import uma.wdi.fusion.utils.XMLUtils;

/* Stores two node maps, for the data and for the respective provenance
 * Used for both input and merged datasets (with one or multiple provenance elements, respectively)
 * Calculates density and consistency values 
 * 
 * @author Volha
 * */
public class Dataset 
{
	private NodeMap data = new NodeMap();
	private NodeMap provenance = new NodeMap();
	
	private double datasetDensity = 0.0;
	private double datasetConsistency = 0.0;
	private Map<String, Integer> density = new HashMap<String, Integer>();
	private Map<String, Double> consistency = new HashMap<String, Double>();

	private Map<String, String> listNodes = new HashMap<String, String>();

	// Getters
	public NodeMap getData()
	{
		return data;
	}
	public NodeMap getProvenance()
	{
		return provenance;
	}
	public Map<String, String> getListNodes()
	{
		return listNodes;
	}
	public Map<String, Double> getAttrConsistency()
	{
		return consistency;
	}
	public Double getConsistency()
	{
		return datasetConsistency;
	}
	public Double getDensity()
	{
		return datasetDensity;
	}
	
	
	// Get the provenance element assuming is just one per dataset (=> not applicable to merged (created from DataUnion) dataset)
	public String getProvenanceID()
	{
		if (provenance.getKeySet().size() == 0) return null;
		return provenance.getKeySet().iterator().next();
	}

	// Load dataset from file, given paths for unique IDs for data elements and provenance elements
	// uniqueProvenance should be true for input datasets and false for merged (created from DataUnion) dataset
	public boolean loadFromFile(String file, String idDataPath, String idProvPath, boolean uniqueProvenance, boolean listID)
	{
		boolean d = data.loadFromFile(file, idDataPath, listID);
		boolean p = provenance.loadFromFile(file, idProvPath);
		
		if (uniqueProvenance && provenance.getKeySet().size() > 1)
		{
			System.out.println("Dataset " + file + " contains more than one provenance element");			
			return false;
		}
		
		return d && p; 
	}
	
	// Get the name of the data element (e.g. "movie" or "physician")
	public String getDataElementName()
	{		
		return data.getEntrySet().iterator().next().getValue().getNodeName();		
	}
	
	// Calculate density counts for all attributes in the dataset
	// Fills density and listNodes maps for further use
	public Map<String, Integer> getDatasetDensityCounts()
	{
		density.clear();
		
		// get counts:
		for (Entry<String, Node> entry : data.getEntrySet())
		{
			Map<String, Integer> nodeCounts = new HashMap<String, Integer>();
			countNodes(entry.getValue(), nodeCounts, "");
			addListInfo(entry.getValue(), "");

			// increase counts that were found in the current node
			for (String s : nodeCounts.keySet())
			{
				if (density.containsKey(s)) density.put(s, density.get(s)+1);
				else density.put(s,1);
			}
		}	
		// TODO: re-consider
		// density.remove(data.getIDAttribute());
		
		return density;
	}

	// Calculate density counts for a dataset
	public double getDatasetDensity(double attr_num)
	{
		double node_num = data.getEntrySet().size();		
		double total = node_num*attr_num;
		if (total == 0.0) return 0.0;

		double count = 0.0;
		String idStr = data.getIDAttribute();
		for (Entry<String, Integer> entry : density.entrySet())
		{	
			if (!entry.getKey().equals(idStr) && !entry.getKey().equals(idStr+"/value")) // don't count id
			{	
				count = count + entry.getValue();
			}
		}		
		datasetDensity = count/total;
		return count/total;
	}
	
	// Calculate density (%of non-null values) of an attribute; path within node (i.e. attribute name)
	public double getAttributetDensity(String path)
	{
		// TODO: is the formula below better/the same?
		// return density.get(path)/data.getEntrySet().size();
		
		// non-nulls/all nodes
		double size = 0.0;
		double count = 0.0;
		for (Entry<String, Node> node : data.getEntrySet())
		{
			boolean nonempty = hasNonEmptyVlaues(node.getValue(),path);
			if (nonempty) count++;
			size++;
		}		
		if (size == 0.0) return 0.0;
		
 		return count/size;
	}	
	
	// Count non-null elements
	// Used for calculating density; recursive
	private boolean countNodes(Node node, Map<String, Integer> nodeCounts, String prefix)
	{		
		NodeList list = node.getChildNodes();
		for (int i = 0; i < list.getLength(); i++)
		{
			String name = list.item(i).getNodeName();
			if (!prefix.isEmpty()) name = prefix+"/"+name;
			if (hasElementNodes(list.item(i)))
			{ 
				addListInfo(list.item(i), name);
				countNodes(list.item(i), nodeCounts, name);				
			}
			else if (list.item(i).getNodeType()==Node.ELEMENT_NODE)
			{
				if (!list.item(i).getTextContent().isEmpty())
				{
					// if (!prefix.isEmpty()) name = prefix+"/"+name;
					nodeCounts.put(name, 1);
					// NOTE: alternatively to nodeCount, you can count ALL non-null value, not one per node: 
					// if (density.containsKey(name)) density.put(name, density.get(name)+1);
					// else density.put(name,1);
				}
			}
		}	
		return true;
	}
	
	// Check whether a node has child nodes of type ELEMENT_NODE 
	private boolean hasElementNodes(Node node)
	{
		NodeList list = node.getChildNodes();
		if (list.getLength() == 0) return false;
		
		boolean hasElementNodes = false;
		for (int i = 0; i < list.getLength(); i++)
		{	
			if (list.item(i).getNodeType()==Node.ELEMENT_NODE) hasElementNodes = true; 
		}			
				
		return hasElementNodes;
	}
	
	// Check whether an attribute is a list, save it
	private boolean addListInfo(Node node, String path)
	{
		String prefix = path;
		if (!prefix.isEmpty()) prefix = prefix+"/";
		
		NodeList list = node.getChildNodes();
		Map<String,Integer> names = new HashMap<String,Integer>();
		int count = 0;
		for (int i = 0; i < list.getLength(); i++)
		{
			if (list.item(i).getNodeType()==Node.ELEMENT_NODE)
			{
				count++;
				String name = list.item(i).getNodeName();
				if (names.containsKey(name)) names.put(name, names.get(name)+1);
				else names.put(name, 1);				
			}
		}
		for (String n : names.keySet())
		{
			if (names.get(n) > 1) listNodes.put(prefix+n, path);
		}
		
		if (count == 1) return false;
		if (names.size() == 1) return true;
		
		return false;
	}
	
	// Check whether there are non-empty values for path in a node
	private boolean hasNonEmptyVlaues(Node node, String path)
	{
		Collection<String> values = XMLUtils.getValue(node,path);
		boolean nonempty = false;
		for (String v : values)
		{
			if (!v.isEmpty()) nonempty = true;
		}
		return nonempty;
	}

	// Get the content of path field of the provenence item
	public String getProvenanceAttribute(String provID, String path)
	{
		Node node = provenance.getNode(provID);
		Collection<String> dates = XMLUtils.getValue(node, path);
		// assume there is only one date
		return dates.iterator().next();
	}
	
	// Calculate data consistency, on a union dataset
	public Map<String, Double> calculateDatasetConsistency(Set<String> attributes, int dsCount)
	{
		// to be sure density map and listNodes are filled
		getDatasetDensityCounts();
		consistency.clear();
		
		Double numOfNodes = (double) data.getKeySet().size();
		
		// go through nodes
		for (Entry<String, Node> entry : data.getEntrySet())
		{
			String id = entry.getKey();
			Node node = entry.getValue();
			int nodeDim = data.getIDSize(id);
			for (String attr : attributes)
			{
				Collection<String> vals = XMLUtils.getValueUnion(node, attr);
				Map<String, Integer> valCnt = new HashMap<String, Integer>();
				for (String s : vals)
				{
					if (valCnt.containsKey(s)) valCnt.put(s, valCnt.get(s)+1);
					else valCnt.put(s,1);
				}
				// if consistent, one value, count == number of contributor to a node:				
				if (valCnt.size() == 1)
				{
					int prvCnt = XMLUtils.getAttrCount(node,attr,XMLUtils.VALUE,XMLUtils.PROV);
					if (prvCnt == nodeDim)
					{
						if (consistency.containsKey(attr)) consistency.put(attr, consistency.get(attr)+1.0);
						else consistency.put(attr,1.0);
					}
				}
			}
		}	
		
		// calculate consistency
		datasetConsistency = 0.0;
		for (String attr : attributes)
		{
			// Uncomment to do not consider ID attribute:
			// if (!attr.equals(data.getIDAttribute()))
			{
				if(consistency.containsKey(attr) ) 
				{
					// datasetConsistency =+ consistency.get(attr); : error, detected by Timo & Jakob
					datasetConsistency = datasetConsistency + consistency.get(attr);
					consistency.put(attr,consistency.get(attr)/numOfNodes);
				}
				else consistency.put(attr,0.0);					
				// System.out.println(attr + " : " + consistency.get(attr));
			}
		}
		datasetConsistency = datasetConsistency/((attributes.size()-1)*data.getKeySet().size());
		// System.out.println("overall consistency : " + datasetConsistency);
				
		return consistency;
	}
}

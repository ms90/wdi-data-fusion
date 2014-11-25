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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uma.wdi.fusion.input.Dataset;
import uma.wdi.fusion.input.Duplicates;
import uma.wdi.fusion.utils.XMLUtils;

/* Create a union of all attribute values from a list of datasets
 *	Uses <value provenance="prov02">...</value> format
 *	Keeps all provenance elements
 *	Fills attributes (with list attributes filtered) allAttributes and listNodes hash maps
 *	
 * @author Volha
 * */
public class DataUnion 
{
	private Node root = null;
	private Set<String> attributes = new HashSet<String>();
	private Set<String> allAttributes = new HashSet<String>();
	private Set<String> listNodes = new HashSet<String>();

	// Getters
	public Set<String> getNonListAttributes()
	{
		return attributes;
	}
	public Set<String> getAllAttributes()
	{
		return allAttributes;
	}
	public Set<String> getListNodes()
	{
		return listNodes;
	}
	
	/* Create a union of all attribute values from a list of datasets
	* Uses <value provenance="prov02">...</value> format
	* Keeps all provenance elements
	*	Fills attributes (with list attributes filtered) allAttributes and listNodes hash maps
	* rootPath : under which both data and provenance elements are
	 */
	public Node createUnion(Set<Dataset> datasets, Duplicates dupl, String rootPath)
	{
		// 0: get element name, assume it is the same in all datasets 
		String elementName = datasets.iterator().next().getDataElementName();
				
		// 1: getDatasetDensityCounts() for each dataset and merge them into "attributes"
		String idAttr = null;
		for(Dataset ds : datasets)
		{
			if (idAttr == null) idAttr = ds.getData().getIDAttribute();
			Map<String, Integer> dsAttr = ds.getDatasetDensityCounts();
			for (String a : dsAttr.keySet())
			{
				if (!allAttributes.contains(a)) allAttributes.add(a);				
			}
			for (String ln: ds.getListNodes().keySet())
			{
				if (!listNodes.contains(ln)) listNodes.add(ln);				
			}
		}
		// if you want ID attribute to not be counted:
		// allAttributes.remove(idAttr);
		
		// filter away list nodes
		attributes.clear();
		Set<String> newListNodes = new HashSet<String>(listNodes);
		for (String attr : allAttributes)
		{
			boolean isList = false;
			for (String ln : listNodes)
			{
				if (attr.length() > ln.length()+1 && ln.equals(attr.substring(0,ln.length()))) isList = true;
				// if (attr.equals(ln)) isList = true; // NOTE: if commented, list nodes without ambiguities are not added
				if (attr.equals(ln))
				{
					newListNodes.remove(ln);
				}
			}
			if (!isList) attributes.add(attr);
			// else System.out.println("filtered " + attr);
		}
		listNodes = newListNodes;
		
		// 2: prepare list of IDs from getIDtoClusterMap 
		// 3: collect provenance nodes from all datasets
		Map<String,Dataset> idToDataset = new HashMap<String,Dataset>();
		Map<String,Node> provenance = new HashMap<String,Node>();
		for (String id : dupl.getIDtoClusterMap().keySet())
		{
			for (Dataset ds : datasets)
			{
				// collect provenance
				for (Entry<String,Node> prv : ds.getProvenance().getEntrySet())
				{
					provenance.put(prv.getKey(), prv.getValue());
				}
				// collect IDs
				if (ds.getData().getNode(id) != null)
				{
					if (idToDataset.containsKey(id))
					{
						// should not be the case!
						System.out.println("Error: object with ID "+id+" is found in multiple datasets");
					}
					idToDataset.put(id, ds);
				}
			}
		}		 

		// check your clusters
		boolean clustersOK = true;
		for (Entry<Integer, Set<String>> entry : dupl.getClusters().entrySet())
		{
			Set<String> clusterIDs = entry.getValue();
			for (String id : clusterIDs) // for each cluster Node
			{
				if (idToDataset.get(id) == null) 
				{
					System.out.println("Error: element "+id+" from the duplicate clusters is not found in your data");
					clustersOK = false;
				}
			}		
		}
		if (!clustersOK) return null;

		
		// 4: create new root node (TODO: path is 2 levels back from the id path)
		// or just "dataUnion"->provenance, "dataUnion"-> data
		root = XMLUtils.createDocument(rootPath);
	 
		// 5: add provenance to the root node
		for (Entry<String,Node> prv : provenance.entrySet())
		{
			root.appendChild(root.getOwnerDocument().adoptNode(prv.getValue().cloneNode(true)));
		}
	
		// for each cluster
		for (Entry<Integer, Set<String>> entry : dupl.getClusters().entrySet())
		{
			// create new object node (e.g. movie)
			Set<String> clusterIDs = entry.getValue();
			Node obj = XMLUtils.createAndAppendNode(root, elementName);			
			// NONLIST: for each non-list attribute
			for (String attr : attributes)
			{
				// create new property node (e.g. title or a sequence of nodes director/name)
				Node prop = XMLUtils.createAndAppendNode(obj, attr);	
				for (String id : clusterIDs) // for each cluster Node
				{
					Collection<String> vals = XMLUtils.getValue(idToDataset.get(id).getData().getNode(id), attr);
					String prv = idToDataset.get(id).getProvenanceID();
					for (String v : vals) // for an attribute, add all its (value, provenance) pairs to the property node
					{
						if (!v.isEmpty())
							XMLUtils.addNewElementWithAttribute(prop, XMLUtils.VALUE, v, XMLUtils.PROV, prv);
					}
				}				
			}
			// LIST: for each list attribute
			for (String ln : listNodes)
			{
				// create new property node - go one level UPPER!)
				Node prop = XMLUtils.createAndAppendNode(obj, XMLUtils.oneLevelUp(ln));
				for (String id : clusterIDs) // for each cluster Node
				{
					NodeList list = XMLUtils.getNodeList(idToDataset.get(id).getData().getNode(id), ln);
					String prv = idToDataset.get(id).getProvenanceID();
					for (int i = 0; i < list.getLength(); i++)
					{
						// wrap all list elements into "list element" node:
						// Node valueNode = XMLUtils.createAndAppendNode(prop, "listelement");	
						// XMLUtils.addAttribute(valueNode, "provenance", prv);
						// Node newNode = valueNode.appendChild(root.getOwnerDocument().adoptNode(list.item(i).cloneNode(true)));
						// or, alternatively:
						XMLUtils.addAttribute(list.item(i), "provenance", prv);
						prop.appendChild(root.getOwnerDocument().adoptNode(list.item(i).cloneNode(true)));
					}
				}				
			}				
		}		
		return root;
	}

	// Write the result of the createUnion() method to file (or System.out if fn is null)
	public boolean writeUnionToFile(String fn)
	{
		if (root == null) return false;
		XMLUtils.printNode(root, fn);
		return true;
	}
}

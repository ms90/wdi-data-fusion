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

import java.util.Set;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import uma.wdi.fusion.input.Dataset;
import uma.wdi.fusion.utils.Pair;
import uma.wdi.fusion.utils.XMLUtils;

/* Abstract conflict resolution function that all resolution functions should extend
 * 
 * applyStrategy method : implements a specific strategy to transform value(s) of an attribute  
 * resolve method : given a node of data union, replaces the values according to the conflict resolution strategy
 * 
 *  * @author Volha
 * */
public abstract class AbstractResolutionFunction 
{
	// Replace value and provenance in node n with (value,provenance) pairs from newValue;	
	// node n is (should be) a node of a DatasetUnion (loaded into a dataset)
	public boolean resolve(Dataset ds, Node node, String path) 
	{
		Set<Pair> newValue = applyStrategy(ds, node,path);
		Document xmlDoc = node.getOwnerDocument();		
		
	    NodeList list = XMLUtils.getNodeList(node,path);
	    Node oldNode = list.item(0); // node to replace
	    Node parent = oldNode.getParentNode(); // node parent (accounts for any path length)
		String nodeName = oldNode.getNodeName(); // node name (not path) 
	    Node newNode = xmlDoc.createElement(nodeName); // new node
		// go through new values, add them to a new node
		for (Pair p : newValue)
		{
			XMLUtils.addNewElementWithAttribute(xmlDoc, newNode, XMLUtils.VALUE, p.value, XMLUtils.PROV, p.provenance);
		}
		// replace the old node with the new one
		parent.replaceChild(newNode, oldNode);		

		return true;		
	}
	
	// Get comma-separated provenance id list of all values == val
	public String getProvenanceList(Set<Pair> pairs, String val)
	{
		String provenance = null;
		for (Pair p : pairs)
		{
			if (p.value.equals(val))
			{
				if (provenance == null) provenance = p.provenance;
				else provenance = provenance+","+p.provenance;
			}
		}	
		return provenance;
	}
	
	// Get all set of value-provenance pairs from a node; to use in applyStrategy methods
	public Set<Pair> getValueProvevancePairs(Node node, String path)
	{
		return XMLUtils.getValueAttributePairs(node, path, XMLUtils.VALUE, XMLUtils.PROV);
	}
	
	// To override:
	public abstract Set<Pair> applyStrategy(Dataset ds, Node node, String path);
}

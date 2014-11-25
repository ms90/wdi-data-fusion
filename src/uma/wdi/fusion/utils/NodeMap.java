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

package uma.wdi.fusion.utils;

import java.util.*;
import java.util.Map.Entry;
import org.w3c.dom.*;

/*
 * Class for loading, storing and navigating input datasets.
 * Important assumption: ID uniqueness.
 *  @author Volha
 */
public class NodeMap 
{
	// Stores XML nodes by IDs ("id" tag)
	private Map<String, Node> map = new HashMap<String, Node>();
	private String idAttribute = null;
	private String separator = "---";
	
	public NodeMap()
	{
		idAttribute = null;
	}
	
	// Get entry set, to iterate through the map
	public Set<Entry<String, Node>> getEntrySet()
	{
		return map.entrySet();
	}
	
	// Get key set, to iterate through the map
	public Set<String> getKeySet()
	{
		return map.keySet();
	}
	
	// Get xml node by ID
	public Node getNode(String id) 
	{
		if (!map.containsKey(id)) return null;
		return map.get(id);
	}
	
	// Get ID attribute (path with a node)
	public String getIDAttribute()
	{
		return idAttribute;
	}
	
	/* Load data from .xml file
	 * idPath (e.g. "/movies/movie/id") is used as an entity string ID
	 * If called several times with different files, reads all in one map 
	 */		
	public boolean loadFromFile(String file, String idPath)
	{
		return loadFromFile(file, idPath, false);
	}
	public boolean loadFromFile(String file, String idPath, boolean listID)
	{
	    NodeList list = XMLUtils.loadNodeList(file, idPath);
	    if (list == null) return false;
	    if (list.getLength() == 0)
	    {
	    	System.out.println("ERROR: no ids (" + idPath +") found in the input file " + file);
	    }
        for (int i = 0; i < list.getLength(); i++)
        {
        	if (idAttribute == null) 
        		idAttribute = list.item(i).getNodeName();
        	String id = getID(list.item(i),listID);        	
      	   	map.put(id,list.item(i).getParentNode()); 
        }
        return true;
	}	
	
	// Get either a single string ID or a concatenation of ordered IDs 
	public String getID(Node node, boolean listID)
	{
		String id = null;
		if (listID)
		{
			Set<String> ids = new HashSet<String>();
			NodeList list = node.getChildNodes();
			for (int i = 0; i < list.getLength(); i++)
			{
				String name = list.item(i).getNodeName();
				if (name.equals("value"))
				{
					ids.add(list.item(i).getTextContent());
				}
			}
			int size = ids.size();
			for (int i = 0; i < size; i++)
			{
				String minID = getMinimum(ids);
				ids.remove(minID);
				if (id == null) id = minID;
				else id = id + separator + minID;
			}
		}
		else id = node.getTextContent();
		
		return id;
	}
	
	public int getIDSize(String id)
	{
		String[] ids = id.split(separator);
		return ids.length;
	}
	
	// Get the minimum string (wrt lexicographic order)
	private String getMinimum(Set<String> ids)
	{
		String min = null;
		for (String s : ids)
		{
			if (min == null) min = s;
			else
			{
				if (min.compareTo(s) > 0) min = s;
			}			
		}
		return min;
	}
}


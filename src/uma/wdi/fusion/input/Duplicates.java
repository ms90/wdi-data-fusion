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

import java.io.*;
import java.util.*;
import java.util.Map.Entry;

/* Reads lists of comma-separated pairs of IDs from a list of input files
 * Two objects corresponding to an ID pair are considered duplicates 
 * Makes transitive closure and creates
 * (1) a hash map IDtoClusterMap from an ID to the number of the respective cluster
 * (2) a hash map from a cluster (identified by the above number) to a set of IDs
 * Cluster numbers might not be sequential.
 * 
 * @author Volha
 * */
public class Duplicates 
{
	private Map<String, Integer> IDtoClusterMap = new HashMap<String, Integer>();
	private Map<Integer, Set<String>> clusters = new HashMap<Integer, Set<String>>();
	private Integer numClusters = 0; // is not the number of clusters, can be greater in case clusters were merged!

	// Getters
	public Map<Integer, Set<String>> getClusters()
	{
		return clusters;
	}	
	public Map<String, Integer> getIDtoClusterMap()
	{
		return IDtoClusterMap;
	}
	
	public int size()
	{
		return clusters.size();
	}
	public double avgClusterSize()
	{
		double result = 0.0;
		for (Entry<Integer, Set<String>> cluster : clusters.entrySet())
		{
			result += cluster.getValue().size();
		}		
		return result/clusters.size();
	}
	
	// Read ID pairs, create clusters
	public boolean read(Set<String> filesDuplicates)
	{
		clusters.clear();
		IDtoClusterMap.clear();
		
		for(String fn : filesDuplicates) 
		{
			try 
			{
				File file = new File(fn);
				FileReader fileReader = new FileReader(file);
				BufferedReader bufferedReader = new BufferedReader(fileReader);
				String line;
				while ((line = bufferedReader.readLine()) != null)
				{
					if (!line.isEmpty())
					{
						String[] ids = line.split(",");
						if (ids.length != 2)
						{
							System.out.println("ERROR: wrong format of the input cvs file " + fn + " near " + line);
							return false;	
						}						
						// cases 1 and 2: one of the IDs is in the map
						addOneNew(ids, 0, 1);
						addOneNew(ids, 1, 0);
						// case 3: none of the IDs is in the map 
						if (!IDtoClusterMap.containsKey(ids[0]) && !IDtoClusterMap.containsKey(ids[1]))							
						{
							IDtoClusterMap.put(ids[0],numClusters);
							IDtoClusterMap.put(ids[1],numClusters);
							numClusters++;									
						}
						// case 4: both IDs are in the map, but clusters are different: replace 2nd cluster number by the 1st
						if (IDtoClusterMap.containsKey(ids[0]) && IDtoClusterMap.containsKey(ids[1]) && (IDtoClusterMap.get(ids[0]) != IDtoClusterMap.get(ids[1])))
						{
							Integer newCl = IDtoClusterMap.get(ids[0]);
							Integer oldCl = IDtoClusterMap.get(ids[1]);
							for (String id: IDtoClusterMap.keySet())
							{
								if (IDtoClusterMap.get(id) == oldCl) 
								{
									IDtoClusterMap.put(id, newCl);
								}
							}
						}
						
					}
				}
				fileReader.close();
			} 
			catch (IOException e) 
			{
				// e.printStackTrace();
				System.out.println("ERROR: cvs input file " + fn + " not found");
				return false;
			}			
		}
		
		// form {cluster id} -> {set of element ids} map
		for (Entry<String, Integer> entry : IDtoClusterMap.entrySet())		
		{
			Integer cl_id = entry.getValue();
			String id =  entry.getKey();
			if (clusters.containsKey(cl_id))
			{				
				clusters.get(cl_id).add(id);
			}
			else
			{
				Set<String> ids = new HashSet<String>();
				ids.add(id);
				clusters.put(cl_id, ids);
			}
		}
		
		// print clusters:
		boolean print = false;
		if (print)
		{
			for (Entry<Integer, Set<String>> cluster : clusters.entrySet())
			{
				System.out.print(cluster.getKey() + " :");
				for (String id : cluster.getValue())
				{
					System.out.print(" " + id);
				}
				System.out.println();
			}
		}		
		return true;
	}

	// Add a string to the IDtoClusterMap in case one of the IDs is in and the other is not
	private void addOneNew(String[] ids, int i, int j)
	{
		if (ids.length != 2) return;
		if (IDtoClusterMap.containsKey(ids[i]) && !IDtoClusterMap.containsKey(ids[j]))							
		{
			IDtoClusterMap.put(ids[j], IDtoClusterMap.get(ids[i]));
		}

	}
}

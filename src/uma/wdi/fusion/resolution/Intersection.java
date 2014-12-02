package uma.wdi.fusion.resolution;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.w3c.dom.Node;

import uma.wdi.fusion.input.Dataset;
import uma.wdi.fusion.utils.Pair;

public class Intersection extends AbstractResolutionFunction {

	@Override
	public Set<Pair> applyStrategy(Dataset ds, Node node, String path) {
		// TODO Auto-generated method stub
		
			List<String> list = null;
			Map<String, String> listOfNodes = ds.getListNodes();
			for(Entry<String, String> nodeInner : listOfNodes.entrySet()){
				if (nodeInner.getValue() == node.getNodeName()){
					list.add(node.getNodeName());
				}
			}
					
			
			return null;
	}
	
	public List<String> getIntersection(List<String> list1, List<String> list2){
		
		List<String> list1_1 = list1; //list1
		list1.removeAll(list2);  
		List<String> list1_list2 = list1; //list1_ = list1 - list2
		
		List<String> list2_2 = list2; //list2
		list2.removeAll(list1_1);
		List<String> list2_list1 = list2; //list2_ = list2 - list1
		
		list1_1.addAll(list2_2); //list1 + list2
		
		list2_list1.addAll(list1_list2);//list1_ + list2_
		
		list1_1.removeAll(list2_list1); //(list1 + list2) - (list1_ + list2_)
	
		return list1_1;
	}

}

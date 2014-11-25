package uma.wdi.fusion.utils;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class SimpleNodeList implements NodeList
{
    List<Node> nodes = new ArrayList<>();

    public void addNode(Node node)
    {
        nodes.add(node);
    }
    public Node item(int index)
    {
        return nodes.get(index);
    }

    public int getLength()
    {
        return nodes.size();
    }	
}

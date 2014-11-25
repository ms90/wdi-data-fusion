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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/*
 * Collection of methods for working with xml
 * 
 *   @author Volha
 * */
public class XMLUtils 
{
	public static String VALUE = "value";
	public static String PROV = "provenance";
	
	public static Integer fastGetValue = 0;
	public static Integer slowGetValue = 0;
	
	// Print a node to a file (fn is a path); if fn is null, print into System.out 
	// can be called with node being the whole xml document
	public static void printNode(Node node, String fn)
	{
		try 
		{
			TransformerFactory tFactory = TransformerFactory.newInstance();
			Transformer transformer;
			transformer = tFactory.newTransformer();
			transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");       
			DOMSource source = new DOMSource(node);
			PrintStream out = System.out;
			if (fn != null) out = new PrintStream(new FileOutputStream(fn));
			StreamResult result = new StreamResult(out);
			transformer.transform(source, result);
			out.close();
		} 
		catch (TransformerConfigurationException e) 
		{
			e.printStackTrace();
		} 
		catch (TransformerException e) 
		{

			e.printStackTrace();
		} 
		catch (FileNotFoundException e) 
		{
			e.printStackTrace();
		}
	}

	// [faster, used for fusion] Get node list by tag (within node) from a node of a loaded document
	// by Jakob Huber, Timo Sztyler
	public static NodeList getNodeListFast(Node node, String path)
	{
		// faster implementation:
		NodeList children = node.getChildNodes();
		
		fastGetValue++;
		// System.out.println("fast : " + fastGetValue + " - " + path);
		
		SimpleNodeList mnl = new SimpleNodeList();
        for(int i = 0; i < children.getLength(); i++) 
        {
            Node child = children.item(i);
            String tagname = child.getNodeName();
            
            if(tagname.equals(path)) 
            {
                mnl.addNode(child);
            } 
            else if(path.startsWith(tagname) && child.hasChildNodes()) // one level down?
            {
                for(int j = 0; j < child.getChildNodes().getLength(); j++) 
                {
                    Node childchild = child.getChildNodes().item(j); // get value-nodes
                    
                    if (path.contains("/value") && childchild.hasAttributes() && childchild.getAttributes().getNamedItem("provenance") != null)  
                    {
                    	mnl.addNode(childchild);
                    }                    
                    /*if(childchild.hasAttributes()) 
                    {
                        if(childchild.getAttributes().getNamedItem("provenance") != null) 
                        {
                        	mnl.addNode(childchild);
                        }
                    }*/
                    else if (childchild.hasChildNodes())
                    {
                       	// System.out.println("path too long : " + path);
                       	return null;
                    }         
                }
            }            
        }
        return mnl;   
	}	
	
	// [slower, used for input - you are free to use it for all operations] Get node list by path (within node) from a node of a loaded document
	public static NodeList getNodeList(Node node, String path)
	{
		try 
		{
			slowGetValue++;
			// System.out.println("slow : " + slowGetValue + " - " + path);

			XPathFactory xPathFactory = XPathFactory.newInstance();
		    XPath xpath = xPathFactory.newXPath();
			XPathExpression expr;
			expr = xpath.compile(path);		
		    NodeList list = (NodeList) expr.evaluate(node, XPathConstants.NODESET);
		    
		    return list;
		} 
		catch (XPathExpressionException e) 
		{
			e.printStackTrace();
		}
	    return null;
	}
	
	// Get node list by path from a loaded document
	public static NodeList getDocNodeList(Document xmlDoc, String path)
	{
	    // NodeList list = (NodeList) expr.evaluate(xmlDoc, XPathConstants.NODESET);
	    return getNodeList(xmlDoc, path);
	}
	
	// Load document and get node list by path
	public static NodeList loadNodeList(String file, String path)
	{
	    try 
	    {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		    DocumentBuilder builder;
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(file);

			return getDocNodeList(doc,path);
		} 
	    catch (ParserConfigurationException e) 
	    {
			e.printStackTrace();
		} 
	    catch (SAXException e) 
		{
	    	System.out.println("ERROR: your xml file " + file + " is not well-formed");
	    	// e.printStackTrace();			
		} 
	    catch (IOException e) 
	    {
	    	System.out.println("ERROR: input file " + file + " not found");
			// e.printStackTrace();
		}
	    	    
	    return null;
	}
	
	// Get a set of (element,attribute) values (e.g. value,provenance)
	public static Set<Pair> getValueAttributePairs(Node node, String path, String elementName, String attrName)
	{
		Set<Pair> result = new HashSet<Pair>();		
		NodeList list = XMLUtils.getNodeList(node, path+"/" + elementName + "[@" + attrName + "]"); // /value[@provenance]
	
		for (int i = 0; i < list.getLength(); i++)
		{
			String val = list.item(i).getTextContent();
			String pr = list.item(i).getAttributes().getNamedItem(attrName).getTextContent();
			
			result.add(new Pair(val,pr));
		}		
		return result;
	}
	
	// Get a number of distinct attribute value)s (e.g. provenance ids per node/attribute)
	public static int getAttrCount(Node node, String path, String elementName, String attrName)
	{
		NodeList list = XMLUtils.getNodeList(node, path+"/" + elementName + "[@" + attrName + "]"); // /value[@provenance]
		Map<String,Integer> prvc = new HashMap<String,Integer>();
		for (int i = 0; i < list.getLength(); i++)
		{
			String pr = list.item(i).getAttributes().getNamedItem(attrName).getTextContent();
			if (!prvc.containsKey(pr)) prvc.put(pr, 1);
		}		
		return prvc.size();
	}

	// Get set of values of the attribute defined by path (path is defined within node, not the whole document) 
	public static Collection<String> getValue(Node node, String xpath)
	{
		Collection<String> result = new LinkedList<String>();
	
		// XXX: attempts to get value fast, if not, use xpath-based function (which is much slow)
		// NodeList list = XMLUtils.getNodeList(node, xpath);
		NodeList list = XMLUtils.getNodeListFast(node, xpath);
		if (list == null) list = XMLUtils.getNodeList(node, xpath);
			
		for (int i = 0; i < list.getLength(); i++) 
			result.add(list.item(i).getTextContent());
		
		return result;
	}
	
	// Get set of values of the attribute defined by path, but for the case additional "value" level is present 
	// (to be used with for DataUnion, for a merged dataset) 
	public static Collection<String> getValueUnion(Node node, String xpath)
	{
		return getValue(node, xpath+"/value");
	}
	
	// Create a new document document; returns root node
	public static Node createDocument(String rootElementName)
	{
		try 
		{
			DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder docBuilder;
			docBuilder = docFactory.newDocumentBuilder();
	
			Document doc = docBuilder.newDocument();
	
			Element rootElement = doc.createElement(rootElementName);
			doc.appendChild(rootElement);
			
			return rootElement;
		} 
		catch (ParserConfigurationException e) 
		{
			e.printStackTrace();
		}		
		return null;
	}	
	
	// Create a new node and appends it to root
	// if path is complex (e.g. "director/name" and some nodes on the path already exist, create only necessary nodes
	public static Node createAndAppendNode(Node root, String path)
	{
		if (path == null) return root;
		
		Element node = null;
		if (path.contains("/"))	// check if path is complex
		{
			String[] steps = path.split("/");
			Node upper = root;
			Node lower = null;
			for (int i = 0; i < steps.length; i++)
			{
				// check if element exists
				lower = ifNodeExists(upper, steps[i]);
				if (lower == null)
				{
					lower = upper.getOwnerDocument().createElement(steps[i]);
					upper.appendChild(lower);
				}
				upper = lower;
			}
			return lower;
		}		
		// simple path:
		node = root.getOwnerDocument().createElement(path);
		root.appendChild(node);
		return node;
	}	
	
	// Check if a subnode exists
	private static Node ifNodeExists(Node upper, String path)
	{
		NodeList list = upper.getChildNodes();
		for (int i = 0; i < list.getLength(); i++)
		{
			if (list.item(i).getNodeName().equals(path)) return list.item(i);
		}
		return null;	
	}
	
	// Add a new element to a node, with text value and attribute
	public static void addNewElementWithAttribute(Document xmlDoc, Node node, String elementName, String elementValue, String attrName, String attrValue)
	{
		// element
		Node newVal = xmlDoc.createElement(elementName);
		Node newValText = xmlDoc.createTextNode(elementValue);
		newVal.appendChild(newValText);
		// attribute
		addAttribute(xmlDoc, newVal, attrName, attrValue);
		// add to node
		node.appendChild(newVal);	
	}
	
	// Add a new element to a node, with text value and attribute
	public static void addNewElementWithAttribute(Node node, String elementName, String elementValue, String attrName, String attrValue)
	{
		Document xmlDoc = node.getOwnerDocument(); 
		addNewElementWithAttribute(xmlDoc,node,elementName,elementValue,attrName,attrValue);
	}

	// Add an attribute to a node
	public static void addAttribute(Document xmlDoc, Node node, String attrName, String attrValue)
	{
		Attr newAtt = xmlDoc.createAttribute(attrName);			
		newAtt.setNodeValue(attrValue);
		NamedNodeMap attrs = node.getAttributes();
		attrs.setNamedItem(newAtt);
	}
	// Add an attribute to a node
	public static void addAttribute(Node node, String attrName, String attrValue)
	{
		Document xmlDoc = node.getOwnerDocument(); 
		addAttribute(xmlDoc, node, attrName, attrValue);
	}

	
	public static String oneLevelUp(String path)
	{
		String newPath = "";
		String[] steps = path.split("/");
		if (steps.length <= 1) return path;
		for (int i = 0; i < steps.length-1; i++)
		{
			if (newPath.isEmpty()) newPath = steps[i];
			else newPath = newPath + "/" + steps[i];
		}
		return newPath;
	}
}

package com.tere.registry;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.tere.TereException;
import com.tere.logging.LogManager;
import com.tere.logging.Logger;

public class Registry
{
	private static final String KEY_SEPARATOR = ".";

	private Logger log = LogManager.getLogger(Registry.class);

	private RegNode rootNode = null;

	public class RegNode
	{
		private String key;
		private String value;
		private List<RegNode> children;
		private RegNode parent;
		private RegNode sibling;

		public RegNode(RegNode parent, String key, String value)
		{
			super();
			this.key = key;
			this.value = value;
			this.parent = parent;
			this.children = new ArrayList<Registry.RegNode>();
		}

		public RegNode getChild(int pos)
		{
			return children.get(pos);
		}

		public boolean hasChildren()
		{
			return 0 != children.size();
		}

		public List<RegNode> getChildren()
		{
			return new ArrayList(children);
		}

		public String getValue()
		{
			return value;
		}

		public void setValue(String value)
		{
			this.value = value;
		}

		public String getRelativeKey()
		{
			return key;
		}

		void setSibling(RegNode sibling)
		{
			this.sibling = sibling;
		}

		public String getKey()
		{
			RegNode currentNode = this;
			StringBuffer key = new StringBuffer(getRelativeKey());
			currentNode = currentNode.getParent();
			while (null != currentNode)
			{
				key.insert(0, ".");
				key.insert(0, currentNode.getRelativeKey());
				currentNode = currentNode.getParent();
			}
			return key.toString();
		}

		public RegNode getParent()
		{
			return parent;
		}

		public void addChild(RegNode regNode)
		{
			children.add(regNode);

		}

		public List<RegNode> getChildren(String relativeKey)
		{
			List<RegNode> results = new ArrayList<Registry.RegNode>();
			for (RegNode child : children)
			{
				if (child.getRelativeKey().equals(relativeKey))
				{
					results.add(child);
				}
			}
			return results;
		}

		public RegNode getFirstChild(String relativeKey)
		{
			for (RegNode child : children)
			{
				if (child.getRelativeKey().equals(relativeKey))
				{
					return child;
				}
			}
			return null;
		}


		@Override
		public String toString()
		{
			return "RegNode [key=" + key + ", value=" + value + "]";
		}
	}

	public Registry()
	{
		rootNode = new RegNode(null, "root", null);

	}

	private String createKey(Node node, Document doc)
	{
		StringBuffer key = new StringBuffer(node.getNodeName());
		Node currentNode = node.getParentNode();
		while ((null != currentNode) && (currentNode != doc))
		{
			key.insert(0, KEY_SEPARATOR);
			key.insert(0, currentNode.getNodeName());
			currentNode = currentNode.getParentNode();
		}
		key.insert(0, "root" + KEY_SEPARATOR);
		return key.toString();
	}

	// private String createKey(Node node, int index, Document doc)
	// {
	// return createKey(node, doc) + KEY_SEPARATOR + index;
	// }
	//
	// private String createAttributeKey(Node node, Node attribute, Document
	// doc)
	// {
	// return createKey(node, doc) + KEY_SEPARATOR + attribute.getNodeName();
	// }

	private void addNodesByKey(List<RegNode> outNodes, RegNode regNode,
			String key)
	{
		int childCount = regNode.getChildren().size();
		if (regNode.getKey().equals(key))
		{
			outNodes.add(regNode);
		}
		for (int nodeNo = 0; nodeNo < childCount; nodeNo++)
		{
			RegNode child = regNode.getChild(nodeNo);
			addNodesByKey(outNodes, child, key);
		}
	}

	private void traverseAllNodes(Node parent, Node node, RegNode parentRegNode)
	{
		log.debug("Traversing node %s(%d)", node.getNodeName(),
				node.getNodeType());

		switch (node.getNodeType())
		{
		case Node.ELEMENT_NODE:
		{
			RegNode regNode = new RegNode(parentRegNode, node.getNodeName(),
					node.getNodeValue());
			log.debug("Adding node %s=%s...", node.getNodeName(),
					node.getNodeValue());
			// values.put(regNode.getKey(), regNode);
			parentRegNode.addChild(regNode);
			NamedNodeMap attributes = node.getAttributes();

			for (int attNo = 0; attNo < attributes.getLength(); attNo++)
			{
				traverseAllNodes(node, attributes.item(attNo), regNode);
			}
			if (node.hasChildNodes())
			{
				NodeList NL = node.getChildNodes();
				for (int i = 0; i < NL.getLength(); i++)
				{
					Node childnode = NL.item(i);

					traverseAllNodes(node, childnode, regNode);
				}
			}
			break;
		}
		case Node.TEXT_NODE:
		{
			String value = node.getNodeValue();
			if (null != value)
			{
				value = value.trim();
				log.debug("Adding text %s to %s", value,
						parentRegNode.getRelativeKey());
				parentRegNode.setValue(value);
			}
			break;
		}
		case Node.ATTRIBUTE_NODE:
		{
			log.debug("Attribute %s=%s...", node.getNodeName(),
					node.getNodeValue());
			RegNode regNode = new RegNode(parentRegNode, node.getNodeName(),
					node.getNodeValue());
			parentRegNode.addChild(regNode);
			break;
		}
		}
	}

	private void appendNode(StringBuffer buf, int level, RegNode node)
	{
		buf.append(node.getKey());
		if (null != node.getValue())
		{
			buf.append(" value = ");
			buf.append(node.getValue());
		}
		buf.append("\n");

		if (node.hasChildren())
		{
			for (RegNode childNode : node.getChildren())
			{
				appendNode(buf, level + 1, childNode);
			}
		}
	}

	public String toString()
	{
		StringBuffer buf = new StringBuffer();

		appendNode(buf, 0, rootNode);
		return buf.toString();

	}

	public List<RegNode> getNodes(String key)
	{
		List<RegNode> outNodes = new ArrayList<Registry.RegNode>();

		addNodesByKey(outNodes, rootNode, key);
		return outNodes;
	}

	public void read(String file) throws TereException
	{
		DocumentBuilderFactory factory = null;
		DocumentBuilder builder = null;
		Document doc = null;

		try
		{
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			factory.setIgnoringElementContentWhitespace(true);
			doc = builder.parse(file);
			doc.getDocumentElement().normalize();
			log.debug("Adding root node...");
			traverseAllNodes(null, doc, rootNode);
			NodeList children = doc.getChildNodes();

			for (int i = 0; i < children.getLength(); i++)
			{
				Node childNode = children.item(i);
				traverseAllNodes(doc, childNode, rootNode);
			}

			log.debug(toString());
		}
		catch (ParserConfigurationException e)
		{
			throw new TereException(e.getMessage(), e);
		}
		catch (SAXException e)
		{
			throw new TereException(e.getMessage(), e);
		}
		catch (IOException e)
		{
			throw new TereException(e.getMessage(), e);
		}
	}

	public void read(InputStream inputStream) throws TereException
	{
		DocumentBuilderFactory factory = null;
		DocumentBuilder builder = null;
		Document doc = null;

		try
		{
			factory = DocumentBuilderFactory.newInstance();
			builder = factory.newDocumentBuilder();
			factory.setIgnoringElementContentWhitespace(true);
			doc = builder.parse(inputStream);
			doc.getDocumentElement().normalize();
			log.debug("Adding root node...");
			traverseAllNodes(null, doc, rootNode);
			NodeList children = doc.getChildNodes();

			for (int i = 0; i < children.getLength(); i++)
			{
				Node childNode = children.item(i);
				traverseAllNodes(doc, childNode, rootNode);
			}

			log.debug(toString());
		}
		catch (ParserConfigurationException e)
		{
			throw new TereException(e.getMessage(), e);
		}
		catch (SAXException e)
		{
			throw new TereException(e.getMessage(), e);
		}
		catch (IOException e)
		{
			throw new TereException(e.getMessage(), e);
		}
	}

	public void store(String file) throws TereException
	{
	}

	public RegNode getRootNode()
	{
		return rootNode;
	}
}

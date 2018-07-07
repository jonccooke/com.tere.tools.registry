package com.tere.registry;

import com.tere.registry.Registry.RegNode;

public class RegistryBuilder
{

	private Registry registry;
	private RegNode currentNode;

	public void RegistryBuilder(Registry registry)
	{
		this.registry = registry;
		currentNode = registry.getRootNode();

	}

	public RegistryBuilder firstChild(String key)
	{
		if (null != currentNode)
		{
			if (currentNode.hasChildren())
			{
				currentNode = currentNode.getChild(0);
			}
		}
		return this;
	}

}

/**
 * Copyright (c) 2005-2006 Aptana, Inc.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html. If redistributing this code,
 * this entire header must remain intact.
 */
package com.aptana.editor.php.internal.parser.nodes;

import java.util.ArrayList;

/**
 * Represents PHP Function
 * 
 * @author Pavel Petrochenko
 */
public class PHPFunctionParseNode extends PHPBaseParseNode
{

	boolean isMethod;
	private ArrayList<Object> parameters;

	/**
	 * @param modifiers
	 * @param startOffset
	 * @param endOffset
	 * @param className
	 */
	public PHPFunctionParseNode(int modifiers, int startOffset, int endOffset, String className)
	{
		super(PHPBaseParseNode.FUNCTION_NODE, modifiers, startOffset, endOffset, className);
		// super.setNodeName("function"); //$NON-NLS-1$
	}

	/**
	 * @return is this function class method or global function
	 */
	public boolean isMethod()
	{
		return isMethod;
	}

	/**
	 * @param isMethod
	 */
	public void setMethod(boolean isMethod)
	{
		this.isMethod = isMethod;
	}

	/**
	 * @param parameters
	 */
	public void setParameters(ArrayList<Object> parameters)
	{
		this.parameters = parameters;
	}

	/**
	 * @return method signature
	 */
	public String getSignature()
	{
		StringBuffer bf = new StringBuffer();
		bf.append(getNodeName());
		bf.append('(');
		int size = parameters.size();

		for (int a = 0; a < size; a++)
		{
			Parameter p = (Parameter) parameters.get(a);
			p.addLabel(bf);
			if (a != size - 1)
			{
				bf.append(", "); //$NON-NLS-1$
			}
		}
		bf.append(')');
		return bf.toString();
	}

	/**
	 * @return method/function parameters
	 */
	public Parameter[] getParameters()
	{
		Parameter[] result = new Parameter[parameters.size()];
		parameters.toArray(result);
		return result;
	}

}

/**
 * This file Copyright (c) 2005-2008 Aptana, Inc. This program is
 * dual-licensed under both the Aptana Public License and the GNU General
 * Public license. You may elect to use one or the other of these licenses.
 * 
 * This program is distributed in the hope that it will be useful, but
 * AS-IS and WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE, TITLE, or
 * NONINFRINGEMENT. Redistribution, except as permitted by whichever of
 * the GPL or APL you select, is prohibited.
 *
 * 1. For the GPL license (GPL), you can redistribute and/or modify this
 * program under the terms of the GNU General Public License,
 * Version 3, as published by the Free Software Foundation.  You should
 * have received a copy of the GNU General Public License, Version 3 along
 * with this program; if not, write to the Free Software Foundation, Inc., 51
 * Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 * 
 * Aptana provides a special exception to allow redistribution of this file
 * with certain other free and open source software ("FOSS") code and certain additional terms
 * pursuant to Section 7 of the GPL. You may view the exception and these
 * terms on the web at http://www.aptana.com/legal/gpl/.
 * 
 * 2. For the Aptana Public License (APL), this program and the
 * accompanying materials are made available under the terms of the APL
 * v1.0 which accompanies this distribution, and is available at
 * http://www.aptana.com/legal/apl/.
 * 
 * You may view the GPL, Aptana's exception and additional terms, and the
 * APL in the file titled license.html at the root of the corresponding
 * plugin containing this source file.
 * 
 * Any modifications to this file must keep this entire header intact.
 */
package com.aptana.editor.php.internal.indexer;

import gnu.trove.THashSet;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.aptana.editor.php.indexer.IPHPIndexConstants;

/**
 * PHP entry value for lambda functions.
 * 
 * @author Shalom Gibly <sgibly@aptana.com>
 */
public class LambdaFunctionPHPEntryValue extends AbstractPHPEntryValue implements IPHPFunctionEntryValue
{
	/**
	 * Parameters names.
	 */
	private String[] parameterNames;

	/**
	 * Parameter types.
	 */
	private Object[] parameterTypes;

	private int[] parameterStartPositions;

	/**
	 * Indicates whether the parameter is a mandatory one.
	 */
	private boolean[] parameterMandatories;

	/**
	 * Function return types. Might be string value in case of direct type value or reference that might be used to
	 * count the type indirectly.
	 */
	private Object returnTypes;

	/**
	 * LambdaFunctionPHPEntryValue constructor.
	 * 
	 * @param modifiers
	 *            - modifiers.
	 * @param startPosition
	 *            - declaration start position.
	 * @param nameSpace
	 */
	public LambdaFunctionPHPEntryValue(int modifiers, int startPosition, String nameSpace)
	{
		super(modifiers, nameSpace);
		this.setStartOffset(startPosition);
	}

	/**
	 * LambdaFunctionPHPEntryValue constructor.
	 * 
	 * @param modifiers
	 *            - modifiers.
	 * @param parameters
	 *            - function parameters.
	 * @param parameterStartPositions
	 *            - start position of each parameter.
	 * @param parameterMandatories
	 *            - boolean array in the size of the parameters that indicates which of the params is a mandatory one.
	 * @param startPosition
	 *            - declaration start position.
	 * @param nameSpace
	 * @throws IllegalArgumentException
	 *             in any case where the parameterMandatories parameter length is different from the expected.
	 */
	public LambdaFunctionPHPEntryValue(int modifiers, LinkedHashMap<String, Set<Object>> parameters,
			int[] parameterStartPositions, boolean[] parameterMandatories, int startPosition, String nameSpace)
	{
		super(modifiers, nameSpace);

		if (parameters != null)
		{
			if (parameters.size() != 0)
			{
				if (parameterStartPositions == null || parameters.size() != parameterStartPositions.length)
				{
					throw new IllegalArgumentException("Illegal parameter start positions: " + parameterStartPositions); //$NON-NLS-1$
				}
			}

			this.parameterStartPositions = parameterStartPositions;

			if (parameterMandatories == null || parameters.size() != parameterMandatories.length)
			{
				// IdeLog.log(PHPPlugin.getDefault(), IStatus.WARNING,
				// "Optionals array length is not as expected. Expected " + parameters.size() + " and got "
				// + ((parameterMandatories == null) ? "null" : parameterMandatories.length), null);
				// try to recover from that
				if (parameterMandatories == null)
				{
					parameterMandatories = new boolean[parameters.size()];
				}
				else if (parameters.size() > parameterMandatories.length)
				{
					boolean[] newMandatories = new boolean[parameters.size()];
					System.arraycopy(parameterMandatories, 0, newMandatories, 0, parameterMandatories.length);
					parameterMandatories = newMandatories;
				}
				else
				// parameters.size() < parameterMandatories.length
				{
					// reduce the parameters mandatories size, as this is less important
					boolean[] newMandatories = new boolean[parameters.size()];
					System.arraycopy(parameterMandatories, 0, newMandatories, 0, newMandatories.length);
					parameterMandatories = newMandatories;
				}
			}
			this.parameterMandatories = parameterMandatories;
			parameterNames = new String[parameters.size()];
			parameterTypes = new Object[parameters.size()];

			int i = 0;
			for (Entry<String, Set<Object>> entry : parameters.entrySet())
			{
				parameterNames[i] = entry.getKey();
				Set<Object> types = entry.getValue();
				if (types != null && types.size() != 0)
				{
					if (types.size() == 1)
					{
						Object type = types.iterator().next();
						parameterTypes[i] = type;
					}
					else
					{
						THashSet<Object> typesToSave = new THashSet<Object>(types.size());
						typesToSave.addAll(types);
						parameterTypes[i] = typesToSave;
					}
				}
				i++;
			}
		}
		this.setStartOffset(startPosition);
	}

	/**
	 * Constructs LambdaFunctionPHPEntryValue by loading it from an input steam.
	 * 
	 * @param di
	 *            {@link DataInputStream}
	 * @throws IOException
	 */
	public LambdaFunctionPHPEntryValue(DataInputStream di) throws IOException
	{
		super(di);
		internalRead(di);
	}

	/**
	 * Sets function return type.
	 * 
	 * @param type
	 *            - might be string value in case of direct type value or reference that might be used to count the type
	 *            indirectly.
	 */
	public void setReturnType(Object type)
	{
		returnTypes = type;
	}

	/**
	 * Sets function return types.
	 * 
	 * @param type
	 *            - might be string value in case of direct type value or reference that might be used to count the type
	 *            indirectly.
	 */
	public void setReturnTypes(Set<Object> types)
	{
		if (types != null && types.size() != 0)
		{
			if (types.size() == 1)
			{
				setReturnType(types.iterator().next());
			}
			else
			{
				Object[] typesArray = new Object[types.size()];
				types.toArray(typesArray);
				this.returnTypes = typesArray;
			}
		}
	}

	/**
	 * Gets return type.
	 * 
	 * @return string value in case of direct type value or reference that might be used to count the type indirectly.
	 *         null means unknown type.
	 */
	public Set<Object> getReturnTypes()
	{
		if (returnTypes == null)
		{
			return Collections.emptySet();
		}

		if (returnTypes instanceof Object[])
		{
			Object[] returnTypesArray = (Object[]) returnTypes;
			THashSet<Object> result = new THashSet<Object>(returnTypesArray.length);
			for (int i = 0; i < returnTypesArray.length; i++)
			{
				result.add(returnTypesArray[i]);
			}

			return result;
		}
		else
		{
			THashSet<Object> result = new THashSet<Object>(1);
			result.add(returnTypes);
			return result;
		}
	}

	/**
	 * Returns an array of boolean describing which of the parameters is mandatory and which is optional.
	 * 
	 * @return an array of boolean describing which of the parameters is mandatory and which is optional; Returns an
	 *         empty array in case there are no parameters.
	 */
	public boolean[] getMandatoryParams()
	{
		if (parameterMandatories == null)
		{
			return new boolean[0];
		}
		boolean[] toReturn = new boolean[parameterMandatories.length];
		System.arraycopy(parameterMandatories, 0, toReturn, 0, parameterMandatories.length);
		return toReturn;
	}

	/**
	 * Gets function parameters.
	 * 
	 * @return function parameters.
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Set<Object>> getParameters()
	{
		if (parameterNames != null)
		{
			LinkedHashMap<String, Set<Object>> result = new LinkedHashMap<String, Set<Object>>(parameterNames.length);
			for (int i = 0; i < parameterNames.length; i++)
			{
				HashSet<Object> types = new HashSet<Object>();
				Object typeObj = parameterTypes[i];

				if (typeObj != null)
				{
					if (typeObj instanceof Set)
					{
						types.addAll((Set) typeObj);
					}
					else
					{
						types.add(typeObj);
					}
				}
				result.put(parameterNames[i], types);
			}

			return result;
		}

		return Collections.emptyMap();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = super.hashCode();
		result = prime * result;
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (getClass() != obj.getClass())
			return false;
		if (!super.equals(obj))
			return false;
		return true;
	}

	@Override
	public int getKind()
	{
		return IPHPIndexConstants.LAMBDA_FUNCTION_CATEGORY;
	}

	/**
	 * Gets parameter start positions.
	 * 
	 * @return parameter start positions.
	 */
	public int[] getParameterStartPositions()
	{
		return parameterStartPositions;
	}

	@Override
	protected void internalWrite(DataOutputStream da) throws IOException
	{
		IndexPersistence.writeType(returnTypes, da);
		int len = parameterNames == null ? 0 : parameterNames.length;
		da.writeInt(len);
		for (int a = 0; a < len; a++)
		{
			da.writeUTF(parameterNames[a]);
			da.writeBoolean(parameterMandatories[a]);
			da.writeInt(parameterStartPositions[a]);
			IndexPersistence.writeType(parameterTypes[a], da);
		}
	}

	private static String[] NO_PARAM_S = new String[0];
	private static boolean[] NO_PARAM_B = new boolean[0];
	private static Object[] NO_PARAM_O = new Object[0];
	private static int[] NO_PARAM_P = new int[0];

	@Override
	protected void internalRead(DataInputStream di) throws IOException
	{
		returnTypes = IndexPersistence.readType(di);
		int pc = di.readInt();
		if (pc > 0)
		{
			parameterNames = new String[pc];
			parameterMandatories = new boolean[pc];
			parameterTypes = new Object[pc];
			parameterStartPositions = new int[pc];
			for (int a = 0; a < pc; a++)
			{
				parameterNames[a] = di.readUTF();
				parameterMandatories[a] = di.readBoolean();
				parameterStartPositions[a] = di.readInt();
				parameterTypes[a] = IndexPersistence.readType(di);
			}
		}
		else
		{
			parameterNames = NO_PARAM_S;
			parameterMandatories = NO_PARAM_B;
			parameterStartPositions = NO_PARAM_P;
			parameterTypes = NO_PARAM_O;
		}
	}

}

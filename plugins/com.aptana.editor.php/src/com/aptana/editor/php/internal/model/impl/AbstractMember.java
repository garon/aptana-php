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
package com.aptana.editor.php.internal.model.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

import com.aptana.editor.php.core.model.IMember;
import com.aptana.editor.php.core.model.IModelElement;
import com.aptana.editor.php.core.model.ISourceModule;
import com.aptana.editor.php.core.model.ISourceRange;
import com.aptana.editor.php.core.model.IType;
import com.aptana.editor.php.indexer.EntryUtils;
import com.aptana.editor.php.indexer.IElementEntry;
import com.aptana.editor.php.indexer.IElementsIndex;
import com.aptana.editor.php.indexer.PHPGlobalIndexer;
import com.aptana.editor.php.internal.core.builder.IModule;
import com.aptana.editor.php.internal.indexer.AbstractPHPEntryValue;
import com.aptana.editor.php.internal.indexer.ElementsIndexingUtils;
import com.aptana.editor.php.internal.model.utils.ModelUtils;
import com.aptana.editor.php.internal.model.utils.TypeUtils;

/**
 * Abstract member.
 * 
 * @author Denis Denisenko
 */
public abstract class AbstractMember extends AbstractModelElement implements IMember
{
	/**
	 * Entry.
	 */
	private IElementEntry entry;

	/**
	 * AbstractMember constructor.
	 * 
	 * @param entry
	 *            - entry.
	 */
	public AbstractMember(IElementEntry entry)
	{
		this.entry = entry;
	}

	/**
	 * {@inheritDoc}
	 */
	public ISourceModule getSourceModule()
	{
		return ModelUtils.convertModule(entry.getModule());
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean exists()
	{
		IElementsIndex index = PHPGlobalIndexer.getInstance().getIndex();
		List<IElementEntry> entries = index.getEntries(entry.getCategory(), entry.getEntryPath());
		if (entries == null || entries.size() == 0)
		{
			return false;
		}

		for (IElementEntry currentEntry : entries)
		{
			if (entry.equals(currentEntry))
			{
				return true;
			}
		}

		return false;
	}

	/**
	 * {@inheritDoc}
	 */
	public String getElementName()
	{
		return ElementsIndexingUtils.getLastNameInPath(entry.getEntryPath());
	}

	public IType getDeclaringType()
	{
		if (EntryUtils.isType(entry))
		{
			return null;
		}

		IElementEntry typeEntry = TypeUtils.getDeclaringType(entry);
		if (typeEntry == null)
		{
			return null;
		}

		return (IType) ModelUtils.convertEntry(typeEntry);
	}

	/**
	 * {@inheritDoc}
	 */
	public IModelElement getParent()
	{
		// return module for top-level entries
		if (!entry.getEntryPath().contains(Character.toString(IElementsIndex.DELIMITER)))
		{
			return ModelUtils.convertModule(entry.getModule());
		}

		// looking for a parent
		String entryPathStr = entry.getEntryPath();
		if (entryPathStr == null || entryPathStr.length() == 0)
		{
			return ModelUtils.convertModule(entry.getModule());
		}
		IPath entryPath = new Path(entryPathStr);
		if (entryPath.segmentCount() < 2)
		{
			return ModelUtils.convertModule(entry.getModule());
		}

		IPath parentPath = entryPath.removeLastSegments(1);
		List<IElementEntry> entries = getModuleEntries(-1, pathToString(parentPath));
		if (entries.size() == 0)
		{
			return ModelUtils.convertModule(entry.getModule());
		}

		return ModelUtils.convertEntry(entries.iterator().next());
	}

	/**
	 * {@inheritDoc}
	 */
	public List<IModelElement> getChildren()
	{
		// //return top-level entries
		// if (!entry.getEntryPath().contains(Character.toString(IElementsIndex.DELIMITER)))
		// {
		// return ModelUtils.convertEntries(getModuleEntries());
		// }

		// looking for the children
		List<IElementEntry> entries = getModuleEntriesStartingWith(-1, entry.getEntryPath());
		return ModelUtils.convertEntries(entries);
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean hasChildren()
	{
		// return top-level entries
		if (!entry.getEntryPath().contains(Character.toString(IElementsIndex.DELIMITER)))
		{
			return getModuleEntries().size() != 0;
		}

		// looking for the children
		return getModuleEntriesStartingWith(-1, entry.getEntryPath()).size() != 0;
	}

	/**
	 * {@inheritDoc}
	 */
	public ISourceRange getSourceRange()
	{
		if (getEntry().getValue() instanceof AbstractPHPEntryValue)
		{
			// TODO add length when it is available during indexing
			return new SourceRange(((AbstractPHPEntryValue) getEntry().getValue()).getStartOffset());
		}

		return new SourceRange(0);
	}

	/**
	 * Gets entry.
	 * 
	 * @return entry.
	 */
	public IElementEntry getEntry()
	{
		return entry;
	}

	/**
	 * Filters entries by module and returns only those that are declared in a module specified.
	 * 
	 * @param entries
	 *            - entries to filter.
	 * @return filtered entries.
	 */
	protected List<IElementEntry> filterByModule(List<IElementEntry> entries, IModule module)
	{
		if (entries == null || entries.size() == 0)
		{
			return Collections.emptyList();
		}

		List<IElementEntry> result = new ArrayList<IElementEntry>();
		for (IElementEntry currentEntry : entries)
		{
			if (module.equals(currentEntry.getModule()))
			{
				result.add(currentEntry);
			}
		}

		return result;
	}

	/**
	 * Gets current module entries starting with the entry path specified excluding the current entry.
	 * 
	 * @param category
	 *            - category. -1 for any category.
	 * @param entryPath
	 *            - entry path. null or empty path means no filtering.
	 * @return entries
	 */
	protected List<IElementEntry> getModuleEntries(int category, String entryPath)
	{
		List<IElementEntry> moduleEntries = getModuleEntries();

		List<IElementEntry> correctPathEntries = new ArrayList<IElementEntry>();
		if (entryPath == null || entryPath.length() == 0)
		{
			correctPathEntries = moduleEntries;
		}
		else
		{
			for (IElementEntry currentEntry : moduleEntries)
			{
				if (entryPath.equals(currentEntry.getEntryPath()) && !entry.equals(currentEntry))
				{
					correctPathEntries.add(currentEntry);
				}
			}
		}

		if (category == -1)
		{
			return correctPathEntries;
		}

		List<IElementEntry> result = new ArrayList<IElementEntry>();
		for (IElementEntry currentEntry : correctPathEntries)
		{
			if (currentEntry.getCategory() == category)
			{
				result.add(currentEntry);
			}
		}

		return result;
	}

	/**
	 * Gets current module entries starting with the entry path specified, excluding the current entry.
	 * 
	 * @param category
	 *            - category. -1 for any category.
	 * @param entryPath
	 *            - entry path. null or empty path means no filtering.
	 * @return entries
	 */
	protected List<IElementEntry> getModuleEntriesStartingWith(int category, String entryPath)
	{
		List<IElementEntry> moduleEntries = getModuleEntries();

		List<IElementEntry> correctPathEntries = new ArrayList<IElementEntry>();
		if (entryPath == null || entryPath.length() == 0)
		{
			correctPathEntries = moduleEntries;
		}
		else
		{
			for (IElementEntry currentEntry : moduleEntries)
			{
				if (currentEntry.getEntryPath().startsWith(entryPath) && !entry.equals(currentEntry))
				{
					correctPathEntries.add(currentEntry);
				}
			}
		}

		if (category == -1)
		{
			return correctPathEntries;
		}

		List<IElementEntry> result = new ArrayList<IElementEntry>();
		for (IElementEntry currentEntry : correctPathEntries)
		{
			if (currentEntry.getCategory() == category)
			{
				result.add(currentEntry);
			}
		}

		return result;
	}

	/**
	 * Serializes path to string.
	 * 
	 * @param path
	 *            - path.
	 * @return path string
	 */
	protected String pathToString(IPath path)
	{
		if (path == null || path.segmentCount() == 0)
		{
			return ""; //$NON-NLS-1$
		}

		StringBuilder builder = new StringBuilder();
		String[] segments = path.segments();
		for (int i = 0; i < path.segmentCount() - 1; i++)
		{
			builder.append(segments[i]);
			builder.append(IElementsIndex.DELIMITER);
		}

		builder.append(segments[segments.length - 1]);
		return builder.toString();
	}

	/**
	 * Gets current module entries.
	 * 
	 * @return current module entries.
	 */
	private List<IElementEntry> getModuleEntries()
	{
		IModule module = entry.getModule();
		if (module == null)
		{
			return Collections.emptyList();
		}

		IElementsIndex index = PHPGlobalIndexer.getInstance().getIndex();
		return index.getModuleEntries(module);
	}

	/**
	 * {@inheritDoc}
	 */
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((entry == null) ? 0 : entry.hashCode());
		return result;
	}

	/**
	 * {@inheritDoc}
	 */
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AbstractMember other = (AbstractMember) obj;
		if (entry == null)
		{
			if (other.entry != null)
				return false;
		}
		else if (!entry.equals(other.entry))
			return false;
		return true;
	}

}

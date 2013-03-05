/*
Copyright (c) 2011 James Ahlborn

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307
USA
*/

package com.healthmarketscience.jackcess;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Utility for finding rows based on pre-defined, foreign-key table
 * relationships.
 *
 * @author James Ahlborn
 */
public class Joiner 
{
  private final IndexImpl _fromIndex;
  private final List<IndexData.ColumnDescriptor> _fromCols;
  private final IndexCursorImpl _toCursor;
  private final Object[] _entryValues;
  
  private Joiner(IndexImpl fromIndex, IndexCursorImpl toCursor)
  {
    _fromIndex = fromIndex;
    _fromCols = _fromIndex.getColumns();
    _entryValues = new Object[_fromCols.size()];
    _toCursor = toCursor;
  }

  /**
   * Creates a new Joiner based on the foreign-key relationship between the
   * given "from"" table and the given "to"" table.
   *
   * @param fromTable the "from" side of the relationship
   * @param toTable the "to" side of the relationship
   * @throws IllegalArgumentException if there is no relationship between the
   *         given tables
   */
  public static Joiner create(TableImpl fromTable, TableImpl toTable)
    throws IOException
  {
    return create(fromTable.getForeignKeyIndex(toTable));
  }
  
  /**
   * Creates a new Joiner based on the given index which backs a foreign-key
   * relationship.  The table of the given index will be the "from" table and
   * the table on the other end of the relationship will be the "to" table.
   *
   * @param fromIndex the index backing one side of a foreign-key relationship
   */
  public static Joiner create(IndexImpl fromIndex)
    throws IOException
  {
    IndexImpl toIndex = fromIndex.getReferencedIndex();
    IndexCursorImpl toCursor = IndexCursorImpl.createCursor(
        toIndex.getTable(), toIndex);
    // text lookups are always case-insensitive
    toCursor.setColumnMatcher(CaseInsensitiveColumnMatcher.INSTANCE);
    return new Joiner(fromIndex, toCursor);
  }

  /**
   * Creates a new Joiner that is the reverse of this Joiner (the "from" and
   * "to" tables are swapped).
   */ 
  public Joiner createReverse()
    throws IOException
  {
    return create(getToTable(), getFromTable());
  }
  
  public TableImpl getFromTable() {
    return getFromIndex().getTable();
  }
  
  public IndexImpl getFromIndex() {
    return _fromIndex;
  }
  
  public TableImpl getToTable() {
    return getToCursor().getTable();
  }
  
  public IndexImpl getToIndex() {
    return getToCursor().getIndex();
  }
  
  public IndexCursorImpl getToCursor() {
    return _toCursor;
  }

  public List<IndexData.ColumnDescriptor> getColumns() {
    // note, this list is already unmodifiable, no need to re-wrap
    return _fromCols;
  }

  /**
   * Returns {@code true} if the "to" table has any rows based on the given
   * columns in the "from" table, {@code false} otherwise.
   */
  public boolean hasRows(Map<String,?> fromRow) throws IOException {
    toEntryValues(fromRow);
    return _toCursor.findFirstRowByEntry(_entryValues);
  }

  /**
   * Returns {@code true} if the "to" table has any rows based on the given
   * columns in the "from" table, {@code false} otherwise.
   */
  boolean hasRows(Object[] fromRow) throws IOException {
    toEntryValues(fromRow);
    return _toCursor.findFirstRowByEntry(_entryValues);
  }

  /**
   * Returns the first row in the "to" table based on the given columns in the
   * "from" table if any, {@code null} if there is no matching row.
   *
   * @param fromRow row from the "from" table (which must include the relevant
   *                columns for this join relationship)
   */
  public Map<String,Object> findFirstRow(Map<String,?> fromRow)
    throws IOException
  {
    return findFirstRow(fromRow, null);
  }
  
  /**
   * Returns selected columns from the first row in the "to" table based on
   * the given columns in the "from" table if any, {@code null} if there is no
   * matching row.
   *
   * @param fromRow row from the "from" table (which must include the relevant
   *                columns for this join relationship)
   * @param columnNames desired columns in the from table row
   */
  public Map<String,Object> findFirstRow(Map<String,?> fromRow,
                                         Collection<String> columnNames)
    throws IOException
  {
    return (hasRows(fromRow) ? _toCursor.getCurrentRow(columnNames) : null);
  }

  /**
   * Returns an Iterator over all the rows in the "to" table based on the
   * given columns in the "from" table.
   *
   * @param fromRow row from the "from" table (which must include the relevant
   *                columns for this join relationship)
   */
  public Iterator<Map<String,Object>> findRows(Map<String,?> fromRow)
  {
    return findRows(fromRow, null);
  }
  
  /**
   * Returns an Iterator with the selected columns over all the rows in the
   * "to" table based on the given columns in the "from" table.
   *
   * @param fromRow row from the "from" table (which must include the relevant
   *                columns for this join relationship)
   * @param columnNames desired columns in the from table row
   */
  public Iterator<Map<String,Object>> findRows(Map<String,?> fromRow,
                                               Collection<String> columnNames)
  {
    toEntryValues(fromRow);
    return _toCursor.entryIterator(columnNames, _entryValues);
  }

  /**
   * Returns an Iterator with the selected columns over all the rows in the
   * "to" table based on the given columns in the "from" table.
   *
   * @param fromRow row from the "from" table (which must include the relevant
   *                columns for this join relationship)
   * @param columnNames desired columns in the from table row
   */
  Iterator<Map<String,Object>> findRows(Object[] fromRow,
                                        Collection<String> columnNames)
  {
    toEntryValues(fromRow);
    return _toCursor.entryIterator(columnNames, _entryValues);
  }

  /**
   * Returns an Iterable whose iterator() method returns the result of a call
   * to {@link #findRows(Map)}
   * 
   * @param fromRow row from the "from" table (which must include the relevant
   *                columns for this join relationship)
   * @throws IllegalStateException if an IOException is thrown by one of the
   *         operations, the actual exception will be contained within
   */
  public Iterable<Map<String,Object>> findRowsIterable(Map<String,?> fromRow)
  {
    return findRowsIterable(fromRow, null);
  }
  
  /**
   * Returns an Iterable whose iterator() method returns the result of a call
   * to {@link #findRows(Map,Collection)}
   * 
   * @param fromRow row from the "from" table (which must include the relevant
   *                columns for this join relationship)
   * @param columnNames desired columns in the from table row
   * @throws IllegalStateException if an IOException is thrown by one of the
   *         operations, the actual exception will be contained within
   */
  public Iterable<Map<String,Object>> findRowsIterable(
      final Map<String,?> fromRow, final Collection<String> columnNames)
  {
    return new Iterable<Map<String, Object>>() {
      public Iterator<Map<String, Object>> iterator() {
        return findRows(fromRow, columnNames);
      }
    };
  }

  /**
   * Deletes any rows in the "to" table based on the given columns in the
   * "from" table.
   * 
   * @param fromRow row from the "from" table (which must include the relevant
   *                columns for this join relationship)
   * @return {@code true} if any "to" rows were deleted, {@code false}
   *         otherwise
   */
  public boolean deleteRows(Map<String,?> fromRow) throws IOException {
    return deleteRowsImpl(findRows(fromRow, Collections.<String>emptySet()));
  }

  /**
   * Deletes any rows in the "to" table based on the given columns in the
   * "from" table.
   * 
   * @param fromRow row from the "from" table (which must include the relevant
   *                columns for this join relationship)
   * @return {@code true} if any "to" rows were deleted, {@code false}
   *         otherwise
   */
  boolean deleteRows(Object[] fromRow) throws IOException {
    return deleteRowsImpl(findRows(fromRow, Collections.<String>emptySet()));
  }

  /**
   * Deletes all the rows and returns whether or not any "to"" rows were
   * deleted.
   */
  private static boolean deleteRowsImpl(Iterator<Map<String,Object>> iter)
    throws IOException 
  {
    boolean removed = false;
    while(iter.hasNext()) {
      iter.next();
      iter.remove();
      removed = true;
    }
    return removed;    
  }

  /**
   * Fills in the _entryValues with the relevant info from the given "from"
   * table row.
   */
  private void toEntryValues(Map<String,?> fromRow) {
    for(int i = 0; i < _entryValues.length; ++i) {
      _entryValues[i] = _fromCols.get(i).getColumn().getRowValue(fromRow);
    }
  }

  /**
   * Fills in the _entryValues with the relevant info from the given "from"
   * table row.
   */
  private void toEntryValues(Object[] fromRow) {
    for(int i = 0; i < _entryValues.length; ++i) {
      _entryValues[i] = _fromCols.get(i).getColumn().getRowValue(fromRow);
    }
  }

  /**
   * Returns a pretty string describing the foreign key relationship backing
   * this Joiner.
   */
  public String toFKString() {
    StringBuilder sb = new StringBuilder();
    sb.append("Foreign Key from ");

    String fromType = "] (primary)";
    String toType = "] (secondary)";
    if(!_fromIndex.getReference().isPrimaryTable()) {
      fromType = "] (secondary)";
      toType = "] (primary)";
    }

    sb.append(getFromTable().getName()).append("[");

    sb.append(_fromCols.get(0).getName());
    for(int i = 1; i < _fromCols.size(); ++i) {
      sb.append(",").append(_fromCols.get(i).getName());
    }
    sb.append(fromType);
    
    sb.append(" to ").append(getToTable().getName()).append("[");
    List<IndexData.ColumnDescriptor> toCols = _toCursor.getIndex().getColumns();
    sb.append(toCols.get(0).getName());
    for(int i = 1; i < toCols.size(); ++i) {
      sb.append(",").append(toCols.get(i).getName());
    }
    sb.append(toType);
    
    return sb.toString();
  }
}

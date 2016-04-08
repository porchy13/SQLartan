package sqlartan.core;

import sqlartan.core.util.RowStreamOps;
import sqlartan.core.util.Streamable;
import java.sql.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Result implements QueryStructure<GeneratedColumn>, Iterable<Row>, Streamable<Row>, RowStreamOps, AutoCloseable {
	/** Type of the Results object */
	private enum Type {
		Update, Query
	}

	private Statement statement;
	private Result.Type type;

	private int updateCount = 0;

	private ArrayList<GeneratedColumn> columns;
	private HashMap<String, GeneratedColumn> columnsIndex;

	private ResultSet resultSet;
	private boolean consumed = false;
	private boolean done = false;
	private int currentRowIdx = 0;
	private Row currentRow;
	private ArrayList<Row> rows;

	Result(Connection connection, String query) throws SQLException {
		statement = connection.createStatement();
		readResult(statement.execute(query));
	}

	Result(PreparedStatement preparedStatement) throws SQLException {
		statement = preparedStatement;
		readResult(preparedStatement.execute());
	}

	/**
	 *
	 * @param executeResult
	 * @throws SQLException
	 */
	private void readResult(boolean executeResult) throws SQLException {
		if (executeResult) {
			type = Type.Query;
			resultSet = statement.getResultSet();
			readMetadata();
		} else {
			type = Type.Update;
			updateCount = statement.getUpdateCount();
			close();
		}
	}

	/**
	 * Reads metadata information from the ResultSet.
	 *
	 * @throws SQLException
	 */
	private void readMetadata() throws SQLException {
		ResultSetMetaData meta = resultSet.getMetaData();
		int count = meta.getColumnCount();

		columns = new ArrayList<>(count);
		columnsIndex = new HashMap<>();

		for (int i = 1; i <= count; i++) {
			String name = meta.getColumnName(i);
			String table = meta.getTableName(i);
			String type = meta.getColumnTypeName(i);

			GeneratedColumn col = new GeneratedColumn(new GeneratedColumn.Properties() {
				public String name() { return name; }
				public String type() {
					return type;
				}
				public String sourceTable() { return table; }
				public String sourceExpr() { throw new UnsupportedOperationException("Not implemented"); }
			});

			columns.add(col);
			columnsIndex.put(name, col);
		}
	}

	/**
	 * Returns if this object is a result of an UPDATE or DELETE query.
	 * Also returns true if the query was a DDL one, but updateCount() will always return 0 in this case.
	 *
	 * @return
	 */
	public boolean isUpdateResult() {
		return type == Type.Update;
	}

	/**
	 * Returns if this object is a list of results from a SELECT query.
	 *
	 * @return
	 */
	public boolean isQueryResult() {
		return type == Type.Query;
	}

	private void requireQueryResult() {
		if (type != Type.Query) throw new IllegalStateException("Result must be of Query type");
	}

	/**
	 * Closes the Results, freeing the underlying ResultSet if applicable.
	 *
	 * If this Results is fully consumed by an Iterator or a Stream, this function will
	 * automatically be called. You should not rely on this behavior if it is possible for
	 * the iteration to be stopped before having consumed the whole data set.
	 *
	 * @throws SQLException
	 */
	@Override
	public void close() {
		if (resultSet != null) {
			try {
				resultSet.close();
			} catch (SQLException e) {
				// Ignore
			}
			resultSet = null;
		}

		if (statement != null) {
			try {
				statement.close();
			} catch (SQLException e) {
				// Ignore
			}
			statement = null;
		}
	}

	//###################################################################
	// QueryStructure implementation
	//###################################################################

	/**
	 * Returns the sources used to generate these results.
	 *
	 * @return
	 */
	@Override
	public List<PersistentStructure<GeneratedColumn>> sources() {
		requireQueryResult();
		throw new UnsupportedOperationException("Not implemented");
	}

	/**
	 * Returns an unmodifiable list of the columns composing these results.
	 * Only application if this Results is a Query result.
	 *
	 * @return
	 */
	@Override
	public List<GeneratedColumn> columns() {
		requireQueryResult();
		return Collections.unmodifiableList(columns);
	}

	/**
	 * Returns the column with the given name
	 * Only application if this Results is a Query result.
	 *
	 * @param name
	 * @return
	 */
	@Override
	public Optional<GeneratedColumn> column(String name) {
		requireQueryResult();
		return Optional.ofNullable(columnsIndex.get(name));
	}

	/**
	 * Returns the column at a given index.
	 * Only application if this Results is a Query result.
	 *
	 * @param idx
	 * @return
	 */
	@Override
	public Optional<GeneratedColumn> column(int idx) {
		requireQueryResult();
		return (idx < 0 || idx >= columns.size()) ? Optional.empty() : Optional.of(columns.get(idx));
	}

	//###################################################################
	// Update result methods
	//###################################################################

	/**
	 * Returns the number of rows updated by the query.
	 *
	 * @return
	 */
	public int updateCount() {
		return updateCount;
	}

	//###################################################################
	// Query result methods
	//###################################################################

	/**
	 * @return
	 */
	public boolean canBeConsumed() {
		return type == Type.Query && (!consumed || rows != null);
	}

	/**
	 *
	 */
	private void consume() {
		requireQueryResult();
		if (isClosed()) throw new IllegalStateException("Result object is closed");
		if (!canBeConsumed()) throw new IllegalStateException("Stream has already been consumed");
		consumed = true;
	}

	/**
	 *
	 */
	public void enableStorage() {
		consume();
		rows = new ArrayList<>();
	}

	/**
	 *
	 */
	private synchronized Row row(int idx) {
		if (idx == currentRowIdx) {
			return currentRow;
		} else if (!done && idx == currentRowIdx + 1) {
			try {
				currentRowIdx++;
				if (resultSet.next()) {
					currentRow = new Row(this, resultSet);
					if (rows != null) rows.add(currentRow);
					return currentRow;
				}
			} catch (SQLException ignored) {}
			currentRow = null;
			done = true;
			close();
			return null;
		} else if (idx < currentRowIdx && rows != null) {
			return rows.get(idx - 1);
		} else {
			throw new IllegalStateException("Unordered access to result rows without storage enabled");
		}
	}

	/**
	 * Constructs an iterator allowing to iterate over the rows of this Results set.
	 *
	 * @return
	 */
	@Override
	public Iterator<Row> iterator() {
		consume();
		return new ResultIterator();
	}

	/**
	 * Constructs a stream of rows.
	 *
	 * @return
	 */
	public Stream<Row> stream() {
		int characteristics = Spliterator.IMMUTABLE | Spliterator.ORDERED | Spliterator.NONNULL | Spliterator.DISTINCT;
		Spliterator<Row> split = Spliterators.spliteratorUnknownSize(iterator(), characteristics);
		return StreamSupport.stream(split, false);
	}

	/**
	 * @param mapper
	 * @param <R>
	 * @return
	 */
	@Override
	public <R> Optional<R> mapFirstOptional(Function<? super Row, ? extends R> mapper) {
		Optional<R> res = RowStreamOps.super.mapFirstOptional(mapper);
		close();
		return res;
	}

	/**
	 * Checks if this Results object has been properly closed and does no longer hold
	 * any internal Closable objects.
	 *
	 * @return
	 */
	public boolean isClosed() {
		return resultSet == null;
	}

	/**
	 * Iterator over the Rows of this result set.
	 */
	private class ResultIterator implements Iterator<Row> {
		private int current = 0;

		@Override
		public boolean hasNext() {
			return row(current + 1) != null;
		}

		@Override
		public Row next() {
			return row(++current);
		}
	}
}
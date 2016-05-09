package sqlartan.core;

import sqlartan.core.util.RuntimeSQLException;
import java.sql.SQLException;

/**
 * TODO
 * @param <T>
 */
public abstract class PersistentStructure<T extends Column> implements Structure<T> {
	protected final Database database;
	protected String name;

	protected PersistentStructure(Database database, String name) {
		this.database = database;
		this.name = name;
	}

	/**
	 * Returns the name of this structure
	 */
	public String name() {
		return name;
	}

	/**
	 * Returns the database that owns this structure.
	 */
	public Database database() {
		return database;
	}

	/**
	 * Returns the safe full name of this persistent structure for use in queries.
	 */
	public String fullName() {
		return "[" + database.name() + "].[" + name() + "]";
	}

	/**
	 * TODO
	 *
	 * @param newName
	 */
	public abstract void rename(String newName);

	/**
	 * TODO
	 *
	 * @param newName
	 */
	public abstract PersistentStructure<T> duplicate(String newName);

	/**
	 * TODO
	 */
	public abstract void drop();

	/**
	 * Returns a result set over all entries in the table
	 */
	public Result selectAll() {
		try {
			return database.assemble("SELECT * FROM ", fullName()).execute();
		} catch (SQLException e) {
			throw new RuntimeSQLException(e);
		}
	}
}

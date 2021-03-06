package sqlartan.core.alter;

import sqlartan.core.Table;
import sqlartan.core.TableColumn;
import sqlartan.core.ast.ColumnDefinition;
import sqlartan.core.ast.parser.ParseException;
import sqlartan.core.ast.token.TokenizeException;
import java.sql.SQLException;
import java.util.List;

/**
 * An alter action structure representing an action which can modify
 * a column from a table structure.
 */
public class ModifyColumnAction extends UpdateColumnAction {
	/**
	 * The original name of the column
	 */
	private final String originalName;

	/**
	 * @param table the table of the column
	 * @param column the column to modify
	 * @param originalName the original name of the column
	 * @throws TokenizeException
	 */
	ModifyColumnAction(Table table, TableColumn column, String originalName) throws TokenizeException {
		super(table, column);
		this.originalName = originalName;
	}

	/**
	 * Executes action, modify actual definition and save it in database.
	 *
	 * @throws ParseException
	 * @throws SQLException
	 */
	@Override
	public void executeAction() throws ParseException, SQLException {
		List<ColumnDefinition> columns = getTableDefinition().columns;
		ColumnDefinition definition = columns.stream().filter(col -> col.name.equals(originalName)).findFirst().get();
		columns.set(columns.indexOf(definition), columnDefinition());

		update(columns);
	}
}

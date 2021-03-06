package sqlartan.core.ast;

import sqlartan.core.ast.gen.Builder;
import sqlartan.core.ast.parser.ParserContext;
import static sqlartan.core.ast.Keyword.VACUUM;

/**
 * https://www.sqlite.org/lang_vacuum.html
 */
public class VacuumStatement implements Statement {
	public static final VacuumStatement instance = new VacuumStatement();

	/**
	 * @see sqlartan.core.ast.parser.Parser
	 */
	public static VacuumStatement parse(ParserContext context) {
		context.consume(VACUUM);
		return instance;
	}

	private VacuumStatement() {}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void toSQL(Builder sql) {
		sql.append(VACUUM);
	}
}

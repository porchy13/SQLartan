package sqlartan.core.ast;

import sqlartan.core.ast.gen.Builder;
import sqlartan.core.ast.parser.ParserContext;
import java.util.Optional;
import static sqlartan.core.ast.Keyword.COLLATE;

/**
 * Term for ORDER BY clause
 * https://www.sqlite.org/syntax/ordering-term.html
 */
@SuppressWarnings({ "OptionalUsedAsFieldOrParameterType", "WeakerAccess" })
public class OrderingTerm implements Node {
	public Expression expression;
	public Optional<String> collation = Optional.empty();
	public Ordering ordering = Ordering.None;

	/**
	 * @see sqlartan.core.ast.parser.Parser
	 */
	static OrderingTerm parse(ParserContext context) {
		OrderingTerm ordering = new OrderingTerm();
		ordering.expression = Expression.parse(context);
		if (context.tryConsume(COLLATE)) {
			ordering.collation = Optional.of(context.consumeIdentifier());
		}
		ordering.ordering = Ordering.parse(context);
		return ordering;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void toSQL(Builder sql) {
		sql.append(expression);
		collation.ifPresent(c -> sql.append(COLLATE).appendIdentifier(c));
		sql.append(ordering);
	}
}

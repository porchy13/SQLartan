package sqlartan.core.ast;

import sqlartan.core.ast.gen.Builder;
import sqlartan.core.ast.parser.ParseException;
import sqlartan.core.ast.parser.Parser;
import sqlartan.core.ast.parser.ParserContext;
import sqlartan.core.ast.token.Token;
import sqlartan.core.ast.token.Tokenizable;
import java.util.Optional;
import static sqlartan.core.ast.Keyword.*;
import static sqlartan.core.ast.Operator.*;

/**
 * https://www.sqlite.org/lang_expr.html
 */
@SuppressWarnings("WeakerAccess")
public abstract class Expression implements Node {
	public static Expression parse(ParserContext context) {
		return parseOrStep.parse(context);
	}

	private static Expression parseFinal(ParserContext context) {
		return context.alternatives(
			LiteralValue::parse,
			Placeholder::parse,
			ColumnReference::parse,
			UnaryOperator::parse
		);
	}

	@SafeVarargs
	private static Parser<Expression> parseStep(Parser<Expression> parser, Tokenizable<? extends Token.Wrapper<? extends KeywordOrOperator>>... tokens) {
		return context -> {
			Expression lhs = parser.parse(context);
			KeywordOrOperator op;
			while ((op = context.optParse(consumeAny(tokens)).orElse(null)) != null) {
				Expression rhs = parser.parse(context);
				lhs = new BinaryOperator(lhs, op, rhs);
			}
			return lhs;
		};
	}

	@SafeVarargs
	private static Parser<KeywordOrOperator> consumeAny(Tokenizable<? extends Token.Wrapper<? extends KeywordOrOperator>>... tokens) {
		return ctx -> {
			for (Tokenizable<? extends Token.Wrapper<? extends KeywordOrOperator>> token : tokens) {
				Optional<? extends Token.Wrapper<? extends KeywordOrOperator>> consumed = ctx.optConsume(token);
				if (consumed.isPresent()) {
					return consumed.get().node();
				}
			}
			throw ParseException.UnexpectedCurrentToken((Tokenizable[]) tokens);
		};
	}

	private static Parser<Expression> parseConcatStep = parseStep(Expression::parseFinal, CONCAT);
	private static Parser<Expression> parseMulStep = parseStep(parseConcatStep, MUL, DIV, MOD);
	private static Parser<Expression> parseAddStep = parseStep(parseMulStep, PLUS, MINUS);
	private static Parser<Expression> parseBitsStep = parseStep(parseAddStep, SHIFT_LEFT, SHIFT_RIGHT, BIT_AND, BIT_OR);
	private static Parser<Expression> parseCompStep = parseStep(parseBitsStep, LT, LTE, GT, GTE);
	private static Parser<Expression> parseEqStep = parseStep(parseCompStep, EQ, NOT_EQ, IS, IS_NOT, IN, LIKE, GLOB, MATCH, REGEXP);
	private static Parser<Expression> parseAndStep = parseStep(parseEqStep, AND);
	private static Parser<Expression> parseOrStep = parseStep(parseAndStep, OR);

	/**
	 * (unary-operator) [expr]
	 */
	public static class UnaryOperator extends Expression {
		public KeywordOrOperator op;
		public Expression operand;

		public UnaryOperator(KeywordOrOperator op, Expression operand) {
			this.op = op;
			this.operand = operand;
		}

		public static UnaryOperator parse(ParserContext context) {
			return new UnaryOperator(
				consumeAny(MINUS, PLUS, BIT_NOT, NOT).parse(context),
				Expression.parse(context)
			);
		}

		@Override
		public void toSQL(Builder sql) {
			sql.appendUnary(op).append(operand);
		}
	}

	/**
	 * [expr] (binary-operator) [expr]
	 */
	public static class BinaryOperator extends Expression {
		public Expression lhs;
		public KeywordOrOperator op;
		public Expression rhs;

		public BinaryOperator(Expression lhs, KeywordOrOperator op, Expression rhs) {
			this.lhs = lhs;
			this.op = op;
			this.rhs = rhs;
		}

		@Override
		public void toSQL(Builder sql) {
			sql.append(lhs).append(op).append(rhs);
		}
	}

	/**
	 * [bind-parameter]
	 */
	public static class Placeholder extends Expression {
		public Token.Placeholder placeholder;

		public Placeholder(Token.Placeholder placeholder) {
			this.placeholder = placeholder;
		}

		public static Placeholder parse(ParserContext context) {
			return new Placeholder(context.consume(Token.Placeholder.class));
		}

		@Override
		public void toSQL(Builder sql) {
			sql.appendRaw(placeholder.stringValue());
		}
	}

	/**
	 * { { schema . } table . } column
	 */
	@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
	public static class ColumnReference extends Expression {
		public Optional<String> schema;
		public Optional<String> table;
		public String column;

		public static ColumnReference parse(ParserContext context) {
			Optional<String> table, schema = Optional.empty();

			if ((table = context.optConsumeSchema()).isPresent()) {
				if ((schema = context.optConsumeSchema()).isPresent()) {
					Optional<String> t = schema;
					schema = table;
					table = t;
				}
			}

			String column = context.consumeIdentifier();

			ColumnReference ref = new ColumnReference();
			ref.schema = schema;
			ref.table = table;
			ref.column = column;
			return ref;
		}

		@Override
		public void toSQL(Builder sql) {
			sql.appendSchema(schema).appendSchema(table).appendIdentifier(column);
		}
	}

	public abstract static class RaiseFunction extends Expression {
		public static RaiseFunction parse(ParserContext context) {
			throw new UnsupportedOperationException();
		}
	}
}

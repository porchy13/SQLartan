package sqlartan.util;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Pattern matching helper
 *
 * To use this class:
 * import static sqlartan.util.Matching.match;
 * import static sqlartan.util.Matching.dispatch;
 *
 * @param <T> Type of the value matched against
 */
public class Matching<T> {
	/**
	 * Match expression against the given value.
	 * The return type of the expression will be given by the first .when() case.
	 *
	 * @param value the value to match against
	 * @param <T>   The type of the value
	 */
	public static <T> Matching<T> match(T value) {
		return new Matching<>(value);
	}

	/**
	 * Match expression against the given value.
	 * The return type of the expression is given by the target argument.
	 *
	 * @param value  the value to match against
	 * @param target the type of the result to return
	 * @param <T>    The type of the value
	 * @param <R>    The type of the result
	 */
	public static <T, R> Matching<T>.Returning<R> match(T value, Class<R> target) {
		return new Matching<>(value).new Returning<>();
	}

	/**
	 * Dispatch expression against the given value.
	 * Does not return a value.
	 *
	 * @param value the value to match against
	 * @param <T>   The type of the value
	 */
	public static <T> Matching<T>.Void dispatch(T value) {
		return new Matching<>(value).new Void();
	}

	private final T value;
	private final Class<?> valueClass;

	private Matching(T value) {
		this.value = value;
		this.valueClass = value.getClass();
	}

	private boolean isMatch(Class<?> matchClass) {
		return matchClass.isAssignableFrom(valueClass);
	}

	/**
	 * Matching case
	 * If the value matches the given class, expr is executed and its returned value
	 * is the result of the match expression.
	 *
	 * @param matchClass the class to match against
	 * @param expr       expression to execute if this case is a match
	 * @param <M>        Type of the match case
	 * @param <R>        Return type of the expression
	 *                   Will be the return type of the match expression
	 */
	@SuppressWarnings("unchecked")
	public <M extends T, R> Returning<R> when(Class<M> matchClass, Function<? super M, R> expr) {
		if (isMatch(matchClass)) {
			return new MatchedReturning<>(expr.apply((M) value));
		} else {
			return new Returning<>();
		}
	}

	/**
	 * A matching expression returning a value
	 *
	 * @param <R> The return type of this matching
	 */
	public class Returning<R> {
		// I HATE YOU JAVA!
		// Y U NO <S super R>?
		//public <M extends T, S super R> Returning<S> when(Class<M> matchClass, Function<? super M, ? extends S> expr)

		/**
		 * Matching case
		 * If the value matches the given class, expr is executed and its returned value
		 * is the result of the match expression.
		 *
		 * @param matchClass the class to match against
		 * @param expr       expression to execute if this case is a match
		 * @param <M>        Type of the match case
		 */
		@SuppressWarnings("unchecked")
		public <M extends T> Returning<R> when(Class<M> matchClass, Function<? super M, R> expr) {
			if (isMatch(matchClass)) {
				return new MatchedReturning<>(expr.apply((M) value));
			} else {
				return this;
			}
		}

		/**
		 * Returns the value of this match expression or an empty optional if none of the cases matched.
		 */
		public Optional<R> get() {
			return Optional.empty();
		}

		/**
		 * Returns the value of this match expression or `other` if none of the cases matched.
		 */
		public R orElse(R other) {
			return other;
		}

		/**
		 * Returns the value of this match expression or the value from the supplier if none of the cases matched.
		 */
		public R orElse(Supplier<R> other) {
			return other.get();
		}

		/**
		 * Returns the value of this match expression or throws an exception if none of the cases matched.
		 */
		public <Z extends Throwable> R orElseThrow(Supplier<Z> supplier) throws Z {
			throw supplier.get();
		}
	}

	private class MatchedReturning<R> extends Returning<R> {
		private final R result;

		private MatchedReturning(R result) {
			this.result = result;
		}

		@Override
		public <M extends T> Returning<R> when(Class<M> matchClass, Function<? super M, R> expr) {
			return this;
		}

		@Override
		public Optional<R> get() {
			return Optional.ofNullable(result);
		}

		@Override
		public R orElse(R other) {
			return result;
		}

		@Override
		public R orElse(Supplier<R> other) {
			return result;
		}

		@Override
		public <Z extends Throwable> R orElseThrow(Supplier<Z> supplier) throws Z {
			return result;
		}
	}

	/**
	 * A dispatch expression
	 */
	public class Void {
		/**
		 * Dispatch case
		 * If the value matches the given class, expr is executed
		 *
		 * @param matchClass the class to match against
		 * @param expr       expression to execute if this case is a match
		 * @param <M>        Type of the match case
		 */
		@SuppressWarnings("unchecked")
		public <M extends T> Void when(Class<M> matchClass, Consumer<M> expr) {
			if (isMatch(matchClass)) {
				expr.accept((M) value);
				return new MatchedVoid();
			} else {
				return this;
			}
		}

		/**
		 * If none of the previous cases were a match, executes action
		 */
		public void orElse(Runnable action) {
			action.run();
		}

		/**
		 * If none of the previous cases were a match, throws the exception returned by the supplied
		 */
		public <Z extends Throwable> void orElseThrow(Supplier<Z> supplier) throws Z {
			throw supplier.get();
		}
	}

	/**
	 * A dummy dispatch expression that was already matched
	 */
	private class MatchedVoid extends Void {
		@Override
		public <M extends T> Void when(Class<M> matchClass, Consumer<M> expr) {
			return this;
		}

		@Override
		public void orElse(Runnable action) {}

		@Override
		public <Z extends Throwable> void orElseThrow(Supplier<Z> supplier) throws Z {}
	}
}

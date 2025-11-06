package nl.rslot.ssx.connector;

@FunctionalInterface
public interface BiFunction<T,U,R> {

	public R apply(T t, U u);

}

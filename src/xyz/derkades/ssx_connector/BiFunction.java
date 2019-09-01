package xyz.derkades.ssx_connector;

@FunctionalInterface
public interface BiFunction<T,U,R> {

	public R apply(T t, U u);

}

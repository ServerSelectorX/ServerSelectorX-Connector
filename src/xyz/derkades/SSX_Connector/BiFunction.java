package xyz.derkades.SSX_Connector;

@FunctionalInterface
public interface BiFunction<T,U,R> {

	public R apply(T t, U u);

}

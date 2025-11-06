package nl.rslot.ssx.connector;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

public class PingLogger {

	// Needs to be concurrent, because it may be accessed by the command while
	// the pinger is running
	private static final Map<String, PingStatus> map = new ConcurrentHashMap<>();

	public static void logSuccess(final String address) {
		map.put(address, new PingSuccess(System.currentTimeMillis()));
	}

	public static void logFail(final String address, final String message) {
		map.put(address, new PingFail(System.currentTimeMillis(), message));
	}

	public static Set<Map.Entry<String, PingStatus>> entries() {
		return map.entrySet();
	}

	public static boolean isEmpty() {
		return map.isEmpty();
	}

	public static void forEach(final BiConsumer<String, PingStatus> consumer) {
		map.forEach(consumer);
	}

	public static void clear() {
		map.clear();
	}

	public static final class PingFail extends PingStatus {

		private final String message;

		private PingFail(final long time, final String message) {
			super(time);
			this.message = message;
		}

		public String getMessage() {
			return this.message;
		}

	}

	public static final class PingSuccess extends PingStatus {

		private PingSuccess(final long time) {
			super(time);
		}

	}

	public static abstract class PingStatus {

		private final long time;

		private PingStatus(final long time) {
			this.time = time;
		}

		public long getTime() {
			return this.time;
		}

	}

}

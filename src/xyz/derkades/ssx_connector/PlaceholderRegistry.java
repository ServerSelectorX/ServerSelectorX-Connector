package xyz.derkades.ssx_connector;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.bukkit.Bukkit;

import xyz.derkades.derkutils.caching.Cache;

public class PlaceholderRegistry {
	
	// This list does not support concurrent modifications. That shouldn't be a problem, because
	// it is only modified when placeholders are registered which only happens at server startup.
	// The list is only read using commands (after server startup)
	private static final List<Placeholder> placeholders = new ArrayList<>();
	
	public static void registerPlaceholder(final Optional<Addon> addon, final String key, final BiFunction<UUID, String, String> valueFunction) {
		placeholders.add(new PlayerPlaceholder(key, addon, valueFunction));
	}
	
	public static void registerPlaceholder(final Optional<Addon> addon, final String key, final Supplier<String> valueSupplier) {
		placeholders.add(new GlobalPlaceholder(key, addon, valueSupplier));
	}
	
	public static void unregisterAll(final Addon addon) {
		final List<Placeholder> keysToRemove = new ArrayList<>();
		placeholders.stream()
			.filter(p -> p.getAddon() == addon)
			.forEach(keysToRemove::add);
		keysToRemove.forEach(placeholders::remove);
	}
	
	public static void clear() {
		placeholders.clear();
	}
	
	public static void forEach(final Consumer<Placeholder> consumer) {
		placeholders.forEach(consumer);
	}
	
	public static Stream<Placeholder> stream() {
		return placeholders.stream();
	}
	
	static void collectPlaceholders(final Map<UUID, String> players, final Consumer<Map<String, Object>> consumer) {
		final List<String> async = Main.instance.getConfig().getStringList("async");
		
		final Map<String, Object> placeholders = new HashMap<>();
		
		Bukkit.getScheduler().runTaskAsynchronously(Main.instance, () -> {
			stream().filter((p) -> async.contains(p.getKey())).forEach(p ->
					placeholders.put(p.getKey(), getValue(p, players)));
			
			Bukkit.getScheduler().runTask(Main.instance, () -> {
				stream().filter((p) -> !placeholders.containsKey(p.getKey())).forEach(p ->
						placeholders.put(p.getKey(), getValue(p, players)));
				
				consumer.accept(placeholders);
			});
		});
	}
	
	static Object getValue(final Placeholder placeholder, final Map<UUID, String> players) {
		final String key = placeholder.getKey();
		
		final Object cache = Cache.getCachedObject("ssxcplaceholder" + key);
		if (cache != null) {
			if (placeholder instanceof PlayerPlaceholder) {
				Main.placeholdersCached += players.size();
			} else {
				Main.placeholdersCached++;
			}
			
			return cache;
		}
		
		final Object value;
		
		if (placeholder instanceof PlayerPlaceholder) {
			Main.placeholders += players.size();
			value = players.entrySet().stream().collect(Collectors.toMap(
							e -> e.getKey(),
							e -> ((PlayerPlaceholder) placeholder).getValue(e.getKey(), e.getValue())));
		} else {
			Main.placeholders++;
			value = ((GlobalPlaceholder) placeholder).getValue();
		}
		
		final boolean cachingEnabled = Main.instance.getConfig().getBoolean("enable-caching", true);
		final int timeout =  Main.instance.getConfig().getInt("cache." + key, 1);
		if (cachingEnabled && timeout > 0) {
			Cache.addCachedObject("ssxcplaceholder" + key, value, timeout);
		}
		return value;
	}
	
	public static class PlayerPlaceholder extends Placeholder {
		
		private final BiFunction<UUID, String, String> function;
		
		private PlayerPlaceholder(final String key, final Optional<Addon> addon, final BiFunction<UUID, String, String> function){
			super(key, addon);
			this.function = function;
		}
		
		public String getValue(final UUID uuid, final String name) {
			return this.function.apply(uuid, name);
		}
		
	}
	
	public static class GlobalPlaceholder extends Placeholder {
		
		private final Supplier<String> valueSupplier;
		
		private GlobalPlaceholder(final String key, final Optional<Addon> addon, final Supplier<String> valueSupplier){
			super(key, addon);
			this.valueSupplier = valueSupplier;
		}
		
		public String getValue() {
			return this.valueSupplier.get();
		}
		
	}
	
	public static abstract class Placeholder {
		
		private final String key;
		private final Optional<Addon> addon;
		
		private Placeholder(final String key, final Optional<Addon> addon){
			this.key = key;
			this.addon = addon;
		}
		
		public String getKey() {
			return this.key;
		}
		
		public boolean isFromAddon() {
			return this.addon.isPresent();
		}
		
		public Addon getAddon() {
			return this.addon.get();
		}
		
	}

}

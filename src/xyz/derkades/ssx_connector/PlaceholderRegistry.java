package xyz.derkades.ssx_connector;

import org.bukkit.Bukkit;
import xyz.derkades.derkutils.caching.Cache;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

	@Deprecated
	public static void forEach(final Consumer<Placeholder> consumer) {
		placeholders.forEach(consumer);
	}

	public static Stream<Placeholder> stream() {
		return placeholders.stream();
	}
	
	static void collectPlaceholders(final Map<UUID, String> players, final Consumer<Map<String, Object>> consumer) {
		final Set<String> async = new HashSet<>(Main.instance.getConfig().getStringList("async"));

		final Map<String, Object> placeholders = new HashMap<>();

		Main.instance.getScheduler().async().runNow(() -> {
			stream().filter((p) -> async.contains(p.getKey())) // Async placeholders only
					.forEach(p -> {
						Object value = getValue(p, players);
						if (value != null) {
							placeholders.put(p.getKey(), value);
						}
					});

			Main.instance.getScheduler().global().run(() -> {
				stream().filter((p) -> !async.contains(p.getKey())) // Sync placeholders only
						.forEach(p -> {
							Object value = getValue(p, players);
							if (value != null) {
								placeholders.put(p.getKey(), value);
							}
						});

				consumer.accept(placeholders);
			});
		});
	}
	
	static Object getValue(final Placeholder placeholder, final Map<UUID, String> players) {
		Object value;

		try {
			if (placeholder instanceof PlayerPlaceholder) {
				value = players.entrySet().stream().collect(Collectors.toMap(
						Map.Entry::getKey,
						e -> ((PlayerPlaceholder) placeholder).getValue(e.getKey(), e.getValue())));
			} else {
				value = ((GlobalPlaceholder) placeholder).getValue();
			}

			if (value == null) {
				if (placeholder.isFromAddon()) {
					Main.instance.getLogger().warning(String.format("Placeholder %s from addon %s is null! This is either an addon bug or addon configuration issue.", placeholder.getKey(), placeholder.getAddon()));
				} else {
					Main.instance.getLogger().warning(String.format("Placeholder %s is null! This is either a bug in the plugin that registered it or a configuration issue.", placeholder.getKey()));
				}
			}
		} catch (Exception e) {
			Main.instance.getLogger().warning("An error occured while retrieving placeholder " + placeholder.getKey() + ". This is probably a bug " +
					"in the plugin or expansion that added this placeholder (not in SSX-Connector).");
			Main.instance.getLogger().warning("Note that not all plugins work properly when no players are online on a server.");
			e.printStackTrace();
			value = null;
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
			if (Main.cacheEnabled) {
				final Optional<String> cache = Cache.get("ssxplaceholder" + name + this.getKey());
				if (cache.isPresent()) {
					Main.placeholdersCached++;
					return cache.get();
				} else {
					Main.placeholdersUncached++;
					final String value = this.function.apply(uuid, name);
					final int timeout = Main.instance.getConfig().getInt("cache." + this.getKey(), Main.instance.getConfig().getInt("default-cache-time", 1));
					Cache.set("ssxplaceholder" + name + this.getKey(), value, timeout);
					return value;
				}
			} else {
				Main.placeholdersUncached++;
				return this.function.apply(uuid, name);
			}
		}
		
	}
	
	public static class GlobalPlaceholder extends Placeholder {
		
		private final Supplier<String> valueSupplier;
		
		private GlobalPlaceholder(final String key, final Optional<Addon> addon, final Supplier<String> valueSupplier){
			super(key, addon);
			this.valueSupplier = valueSupplier;
		}
		
		public String getValue() {
			
			if (Main.cacheEnabled) {
				final Optional<String> cache = Cache.get("ssxplaceholder" + this.getKey());
				if (cache.isPresent()) {
					Main.placeholdersCached++;
					return cache.get();
				} else {
					Main.placeholdersUncached++;
					final String value = this.valueSupplier.get();
					final int timeout = Main.instance.getConfig().getInt("cache." + this.getKey(), Main.instance.getConfig().getInt("default-cache-time", 1));
					Cache.set("ssxplaceholder" + this.getKey(), value, timeout);
					return value;
				}
			} else {
				Main.placeholdersUncached++;
				return this.valueSupplier.get();
			}
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

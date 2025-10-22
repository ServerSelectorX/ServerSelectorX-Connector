package xyz.derkades.ssx_connector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

public class PlaceholderRegistry {

	// This list does not support concurrent modifications. That shouldn't be a problem, because
	// it is only modified when placeholders are registered which only happens at server startup.
	// The list is only read using commands (after server startup)
	private static final List<Placeholder> PLACEHOLDERS = new ArrayList<>();

	public static void registerPlaceholder(final Optional<Addon> addon, final String key, final Function<UUID, String> valueFunction) {
		PLACEHOLDERS.add(new PlayerPlaceholder(key, addon, valueFunction));
	}

	public static void registerPlaceholder(final Optional<Addon> addon, final String key, final Supplier<String> valueSupplier) {
		PLACEHOLDERS.add(new GlobalPlaceholder(key, addon, valueSupplier));
	}

	public static void unregisterAddonPlaceholders(final Addon addon) {
		final List<Placeholder> keysToRemove = new ArrayList<>();
		PLACEHOLDERS.stream()
			.filter(p -> p.getAddon() == addon)
			.forEach(keysToRemove::add);
		keysToRemove.forEach(PLACEHOLDERS::remove);
	}

	public static void clear() {
		PLACEHOLDERS.clear();
	}

	public static List<Placeholder> getPlaceholders() {
		return PLACEHOLDERS;
	}

	public static class PlayerPlaceholder extends Placeholder {

		private final Function<UUID, String> function;

		private PlayerPlaceholder(final String key, final Optional<Addon> addon, final Function<UUID, String> function){
			super(key, addon);
			this.function = function;
		}

		public String getValue(final UUID uuid) {
			return this.function.apply(uuid);
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

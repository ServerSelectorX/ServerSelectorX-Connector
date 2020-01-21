package xyz.derkades.ssx_connector;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

import org.spongepowered.api.Sponge;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.hocon.HoconConfigurationLoader;

public abstract class Addon {

//	protected final JavaPlugin plugin = Main.instance;
	protected CommentedConfigurationNode config;

	public String getName() {
		return this.getClass().getSimpleName();
	}

	public abstract String getDescription();

	public abstract String getAuthor();

	public abstract String getVersion();

	public abstract String getLicense();

	public abstract void onLoad();

	protected void registerListeners() {
		Sponge.getEventManager().registerListeners(Main.instance, this);
	}

	protected void addPlaceholder(final String key, final Supplier<String> placeholder) {
		Main.placeholders.put(key, placeholder);
		this.addToList(key);
	}

	protected void addPlayerPlaceholder(final String key, final BiFunction<UUID, String, String> placeholder) {
		Main.playerPlaceholders.put(key, placeholder);
		this.addToList(key);
	}

	private void addToList(final String placeholder) {
		if (Main.addonPlaceholders.containsKey(this)) {
			Main.addonPlaceholders.get(this).add(placeholder);
		} else {
			final List<String> list = new ArrayList<>();
			list.add(placeholder);
			Main.addonPlaceholders.put(this, list);
		}
	}

	public void reloadConfig() throws IOException {
		final File file = new File(Main.instance.getAddonsFolder(), this.getName() + ".conf");

		if (!file.exists()) {
			return;
		}

		this.config = HoconConfigurationLoader.builder().setPath(file.toPath()).build().load();
	}

}

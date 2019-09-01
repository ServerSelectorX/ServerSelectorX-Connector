package xyz.derkades.ssx_connector;

import java.io.File;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

public abstract class Addon implements Listener {

	protected final JavaPlugin plugin = Main.instance;
	protected FileConfiguration config;

	public abstract String getName();

	public abstract String getDescription();

	public abstract String getAuthor();

	public abstract String getVersion();

	public abstract String getLicense();

	public abstract void onLoad();

	protected void registerListeners() {
		Bukkit.getPluginManager().registerEvents(this, this.plugin);
	}

	protected void addPlaceholder(final String key, final Supplier<String> placeholder) {
		Main.placeholders.put(key, placeholder);
	}

	protected void addPlayerPlaceholder(final String key, final BiFunction<UUID, String, String> placeholder) {
		Main.playerPlaceholders.put(key, placeholder);
	}

	void load() {
		this.reloadConfig();
		this.onLoad();
	}

	void reloadConfig() {
		final File file = new File(Main.instance.addonsFolder, "config.yml");
		if (file.exists()) {
			this.config = YamlConfiguration.loadConfiguration(file);
		}
	}

}

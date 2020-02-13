package xyz.derkades.ssx_connector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {
	
	static Main instance;

	final File addonsFolder = new File(this.getDataFolder(), "addons");

	List<Addon> addons;

	@Override
	public void onEnable() {
		instance = this;

		super.saveDefaultConfig();

		this.addonsFolder.mkdir();

		this.addons = this.loadAddons();

		this.getCommand("ssxc").setExecutor(new ConnectorCommand());

		final int sendIntervalSeconds = this.getConfig().getInt("send-interval", 4);

		Bukkit.getScheduler().runTaskTimerAsynchronously(this, new PlaceholderSender(),
				sendIntervalSeconds * 20, sendIntervalSeconds * 20);

		final Metrics metrics = new Metrics(this, 3000);

		metrics.addCustomChart(new Metrics.SimplePie("data_send_interval", () -> sendIntervalSeconds + ""));

		metrics.addCustomChart(new Metrics.SimplePie("hub_servers", () ->
			this.getConfig().getStringList("addresses").size() + ""));

		metrics.addCustomChart(new Metrics.SimplePie("default_password", () ->
			this.getConfig().getString("password").equals("a") + ""));

		metrics.addCustomChart(new Metrics.AdvancedPie("addons", () -> {
			final Map<String, Integer> map = new HashMap<>();
			this.addons.forEach((a) -> map.put(a.getName(), 1));
			return map;
		}));
		
		registerCorePlaceholders();
	}

	private List<Addon> loadAddons() {
		final List<Addon> addons = new ArrayList<>();

		final File addonsFolder = new File(this.getDataFolder() + File.separator + "addons");

		addonsFolder.mkdirs();

		for (final File addonFile : addonsFolder.listFiles()) {
			if (addonFile.isDirectory()) {
				this.getLogger().warning("Skipped directory " + addonFile.getPath() + "in addons directory. There should not be any directories in the addon directory.");
				continue;
			}

			if (!addonFile.getName().endsWith(".class")) {
				if (!addonFile.getName().endsWith(".yml")) {
					this.getLogger().warning("The file " + addonFile.getAbsolutePath() + " does not belong in the addons folder.");
				}
				continue;
			}

			Addon addon;

			try (URLClassLoader loader = new URLClassLoader(new URL[]{addonsFolder.toURI().toURL()}, this.getClassLoader())){
				final Class<?> clazz = loader.loadClass(addonFile.getName().replace(".class", ""));
				addon = (Addon) clazz.getConstructor().newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException |
					IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				continue;
			}

			addon.reloadConfig();
			addon.onLoad();
			addons.add(addon);
		}

		return addons;
	}

	void reloadAddons() {
		final List<Addon> addons2 = new ArrayList<>(this.addons);
		PlaceholderRegistry.clear();
		this.addons.clear();
		for (final Addon addon : addons2) {
			addon.reloadConfig();
			addon.onLoad();
			this.addons.add(addon);
		}
		registerCorePlaceholders();
	}
	
	void registerCorePlaceholders() {
		PlaceholderRegistry.registerPlaceholder(Optional.empty(), "online",
				() -> String.valueOf(Bukkit.getOnlinePlayers().size()));
		
		PlaceholderRegistry.registerPlaceholder(Optional.empty(), "max",
				() -> String.valueOf(Bukkit.getMaxPlayers()));
	}

}

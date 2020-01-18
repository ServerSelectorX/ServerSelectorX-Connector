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
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	// TODO store all this stuff in a better way

	static final Map<UUID, String> players = new ConcurrentHashMap<>();
	static final Map<String, BiFunction<UUID, String, String>> playerPlaceholders = new ConcurrentHashMap<>();
	static final Map<String, Supplier<String>> placeholders = new ConcurrentHashMap<>();

	static final Map<String, Long> lastPingTimes = new ConcurrentHashMap<>();
	static final Map<String, Optional<String>> lastPingErrors = new ConcurrentHashMap<>();
	static final Map<String, Long> lastPlayerRetrieveTimes = new ConcurrentHashMap<>();
	static final Map<String, Optional<String>> lastPlayerRetrieveErrors = new ConcurrentHashMap<>();

	static final Map<Addon, List<String>> addonPlaceholders = new ConcurrentHashMap<>();

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

		Bukkit.getScheduler().runTaskTimerAsynchronously(this, new RetrievePlayersTask(), 10*20, 10*20);

		final Metrics metrics = new Metrics(this);

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

		placeholders.put("online", () -> String.valueOf(Bukkit.getOnlinePlayers().size()));
		placeholders.put("max", () -> String.valueOf(Bukkit.getMaxPlayers()));
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

}

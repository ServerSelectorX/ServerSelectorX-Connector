package xyz.derkades.ssx_connector;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.bstats.bukkit.Metrics;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

	static final Map<UUID, String> players = new HashMap<>();
	static final Map<String, BiFunction<UUID, String, String>> playerPlaceholders = new HashMap<>();
	static final Map<String, Supplier<String>> placeholders = new HashMap<>();

	static Main instance;

	final File addonsFolder = new File(this.getDataFolder(), "addons");

	List<Addon> addons;

	@Override
	public void onEnable() {
		instance = this;

		super.saveDefaultConfig();

		this.addonsFolder.mkdir();

		this.addons = this.loadAddons();

		this.getCommand("ssxc").setExecutor((sender, command, label, args) -> {
			if (args.length == 1 && args[0].equals("reload")) {
				Main.this.reloadConfig();
				sender.sendMessage("The configuration file has been reloaded");
				return true;
			} else {
				return false;
			}
		});

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

		placeholders.put("online", () -> String.valueOf(Bukkit.getOnlinePlayers().size()));
		placeholders.put("max", () -> String.valueOf(Bukkit.getMaxPlayers()));
	}

	private List<Addon> loadAddons() {
		final List<Addon> addons = new ArrayList<>();

		final File addonsFolder = new File(this.getDataFolder() + File.separator + "addons");

		addonsFolder.mkdirs();

		for (final File addonFile : addonsFolder.listFiles()) {
			if (!addonFile.getName().endsWith(".class")) {
				if (!addonFile.getName().endsWith(".yml") && !addonFile.getName().endsWith(".yaml")) {
					this.getLogger().warning("The file " + addonFile.getAbsolutePath() + " does not belong in the addons folder.");
				}
				continue;
			}

			Addon addon;

			try (URLClassLoader loader = new URLClassLoader(new URL[]{addonFile.toURI().toURL()}, this.getClassLoader())){
				final Class<?> clazz = loader.loadClass(addonFile.getName().replace(".class", ""));
				addon = (Addon) clazz.newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
				e.printStackTrace();
				continue;
			}

			if (!(addon.getName() + ".class").equals(addonFile.getName())) {
				throw new RuntimeException(String.format("Addon name mismatch (%s / %s", addon.getName(), addonFile.getName().replace(".class", "")));
			}

			addon.load();
			addons.add(addon);
		}

		return addons;
	}

}

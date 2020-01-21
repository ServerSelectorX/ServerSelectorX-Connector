package xyz.derkades.ssx_connector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.logging.Logger;

import org.bstats.sponge.Metrics2;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.ConfigDir;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.event.game.state.GamePostInitializationEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;

import com.google.inject.Inject;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import xyz.derkades.ssx_connector.commands.AddonsCommand;
import xyz.derkades.ssx_connector.commands.PlaceholdersCommand;
import xyz.derkades.ssx_connector.commands.ReloadCommand;
import xyz.derkades.ssx_connector.commands.StatusCommand;

@Plugin(id = "ssxconnector", name = "SSX-Connector", version = "beta", description = "Connector plugin for ServerSelectorX")
public class Main {

	// TODO store all this stuff in a better way
	public static final Map<UUID, String> players = new ConcurrentHashMap<>();
	public static final Map<String, BiFunction<UUID, String, String>> playerPlaceholders = new ConcurrentHashMap<>();
	public static final Map<String, Supplier<String>> placeholders = new ConcurrentHashMap<>();

	public static final Map<String, Long> lastPingTimes = new ConcurrentHashMap<>();
	public static final Map<String, Optional<String>> lastPingErrors = new ConcurrentHashMap<>();
	public static final Map<String, Long> lastPlayerRetrieveTimes = new ConcurrentHashMap<>();
	public static final Map<String, Optional<String>> lastPlayerRetrieveErrors = new ConcurrentHashMap<>();

	public static final Map<Addon, List<String>> addonPlaceholders = new ConcurrentHashMap<>();

	public static Main instance;

	public List<Addon> addons;

	@Inject
	Logger logger;

	@Inject
	@DefaultConfig(sharedRoot = false)
	private Path configPath;

	@Inject
	@DefaultConfig(sharedRoot = false)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path privateConfigDir;

	//@Inject
	private final Metrics2 metrics;

	CommentedConfigurationNode config;

	@Inject
	public Main(final Metrics2.Factory metricsFactory) {
		this.metrics = metricsFactory.make(3000);
	}

	public File getAddonsFolder() {
		return new File(this.privateConfigDir.toString(), "addons");
	}

	public void reloadConfig() throws IOException {
		if (!this.configPath.toFile().exists()) {
			this.config = this.configManager.load();

			this.config.getNode("addresses")
			.setValue(Arrays.asList("localhost:9782"))
			.setComment("Addresses to ServerSelectorX instances, in ip:port format.");

			this.config.getNode("server-name")
			.setValue("")
			.setComment("Set this to the name of this server EXACTLY as specified in the BungeeCord config.");

			this.config.getNode("send-interval")
			.setValue(4)
			.setComment("How often the connector plugin sends placeholders (4 means once every 4 seconds)\n"
					+ "You need to restart your server completely after changing this, /ssxc reload is not enough.");

			this.config.getNode("password")
			.setValue("a")
			.setComment("Don't touch this option unless you know you need to.");

			this.configManager.save(this.config);
		} else {
			this.config = this.configManager.load();
		}
	}

    @Listener
    public void preInit(final GamePreInitializationEvent event) {
		instance = this;

		this.getAddonsFolder().mkdirs();

		try {
			reloadConfig();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		this.addons = this.loadAddons();
    }

    @Listener
    public void init(final GameInitializationEvent event) {
    	final CommandSpec command = CommandSpec.builder()
    	.description(Text.of(""))
    	.permission("ssx.connector.command")
    	.child(new AddonsCommand(), "addons")
    	.child(new PlaceholdersCommand(), "placeholders")
    	.child(new ReloadCommand(), "reload")
    	.child(new StatusCommand(), "status")
    	.build();

    	Sponge.getCommandManager().register(this, command, "ssxc");

    	final int sendIntervalSeconds = this.config.getNode("send-interval").getInt(4);
    	final int addresses = this.config.getNode("addresses").getList((t) -> t).size();
    	final boolean defaultPassword = this.config.getNode("password").getString().equals("a");

		this.metrics.addCustomChart(new Metrics2.SimplePie("data_send_interval", () -> sendIntervalSeconds + ""));

		this.metrics.addCustomChart(new Metrics2.SimplePie("hub_servers", () -> addresses + ""));

		this.metrics.addCustomChart(new Metrics2.SimplePie("default_password", () -> defaultPassword + ""));

		this.metrics.addCustomChart(new Metrics2.AdvancedPie("addons", () -> {
			final Map<String, Integer> map = new HashMap<>();
			this.addons.forEach((a) -> map.put(a.getName(), 1));
			return map;
		}));

		placeholders.put("online", () -> String.valueOf(Sponge.getServer().getOnlinePlayers().size()));
		placeholders.put("max", () -> String.valueOf(Sponge.getServer().getOnlinePlayers().size()));
  }

  @Listener
  public void postInit(final GamePostInitializationEvent event) {
    	this.addons.forEach(Addon::onLoad);

    	final int sendIntervalSeconds = this.config.getNode("send-interval").getInt(4);

    	Task.builder().execute(new PlaceholderSender())
    	.async().interval(sendIntervalSeconds, TimeUnit.SECONDS)
    	.submit(this);

    	Task.builder().execute(new RetrievePlayersTask())
    	.async().interval(sendIntervalSeconds, TimeUnit.SECONDS)
		.submit(this);
  }

	private List<Addon> loadAddons() {
		final List<Addon> addons = new ArrayList<>();

		for (final File addonFile : this.getAddonsFolder().listFiles()) {
			if (addonFile.isDirectory()) {
				this.logger.warning("Skipped directory " + addonFile.getPath() + "in addons directory. There should not be any directories in the addon directory.");
				continue;
			}

			if (!addonFile.getName().endsWith(".class")) {
				if (!addonFile.getName().endsWith(".yml")) {
					this.logger.warning("The file " + addonFile.getAbsolutePath() + " does not belong in the addons folder.");
				}
				continue;
			}

			Addon addon;

			try (URLClassLoader loader = new URLClassLoader(new URL[]{addonFile.toURI().toURL()})){
				final Class<?> clazz = loader.loadClass(addonFile.getName().replace(".class", ""));
				addon = (Addon) clazz.getConstructor().newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException |
					IllegalArgumentException | InvocationTargetException | NoSuchMethodException | SecurityException e) {
				e.printStackTrace();
				continue;
			}

			try {
				addon.reloadConfig();
				addon.onLoad();
				addons.add(addon);
			} catch (final IOException e) {
				this.logger.warning("Failed to load addon " + addonFile.getPath());
				e.printStackTrace();
				continue;
			}

		}

		return addons;
	}

}

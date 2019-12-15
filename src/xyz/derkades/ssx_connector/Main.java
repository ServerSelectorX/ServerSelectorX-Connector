package xyz.derkades.ssx_connector;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
import xyz.derkades.ssx_connector.commands.PlaceholdersCommand;
import xyz.derkades.ssx_connector.commands.StatusCommand;

@Plugin(id = "ssxconnector", name = "SSX-Connector", version = "beta", description = "Connector plugin for ServerSelectorX")
public class Main {

	static final Map<UUID, String> players = new HashMap<>();
	static final Map<String, BiFunction<UUID, String, String>> playerPlaceholders = new HashMap<>();
	static final Map<String, Supplier<String>> placeholders = new HashMap<>();

	public static final Map<String, Long> lastPingTimes = new HashMap<>();
	public static final Map<String, String> lastPingErrors = new HashMap<>();
	public static final Map<String, Long> lastPlayerRetrieveTimes = new HashMap<>();
	public static final Map<String, String> lastPlayerRetrieveErrors = new HashMap<>();

	static Main instance;

	List<Addon> addons;

	@Inject
	Logger logger;

	@Inject
	@DefaultConfig(sharedRoot = true)
	private Path defaultConfig;

	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;

	@Inject
	@ConfigDir(sharedRoot = false)
	private Path privateConfigDir;

	@Inject
	private Metrics2 metrics;

//	final File addonsFolder = new File(this.getDataFolder(), "addons");

	CommentedConfigurationNode config;

	public File getAddonsFolder() {
		return new File(this.privateConfigDir.toString(), "addons");
	}

    @Listener
    public void preInit(final GamePreInitializationEvent event) {
		instance = this;

		try {
			this.config = this.configManager.load();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		this.getAddonsFolder().mkdir();
		this.addons = this.loadAddons();
    }

    @Listener
    public void init(final GameInitializationEvent event) {
    	final CommandSpec command = CommandSpec.builder()
    	.description(Text.of(""))
    	.permission("ssx.connector.command")
    	.child(new StatusCommand(), "status")
    	.child(new PlaceholdersCommand(), "placeholders")
    	.build();

    	Sponge.getCommandManager().register(this, command, "ssxc");

    	final int sendIntervalSeconds = this.config.getNode("send-interval").getInt(4);
    	final int addresses = this.config.getNode("addresses").getList((t) -> t).size();
    	final boolean defaultPassword = this.config.getNode("password").getString().equals("a");

		this.metrics.addCustomChart(new Metrics2.SimplePie("data_send_interval", () -> sendIntervalSeconds + ""));

		this.metrics.addCustomChart(new Metrics2.SimplePie("hub_servers", () -> addresses + ""));

		this.metrics.addCustomChart(new Metrics2.SimplePie("default_password", () -> defaultPassword + ""));

		placeholders.put("online", () -> String.valueOf(Sponge.getServer().getOnlinePlayers().size()));
		placeholders.put("max", () -> String.valueOf(Sponge.getServer().getOnlinePlayers().size()));
    }


    @Listener
    public void postInit(final GamePostInitializationEvent event) {
    	this.addons.forEach(Addon::onLoad);

    	final int sendIntervalSeconds = this.config.getNode("send-interval").getInt(4);

    	Task.builder()
    	.async()
    	.interval(sendIntervalSeconds, TimeUnit.SECONDS)
    	.execute(new PlaceholderSender())
    	.delay((long) (sendIntervalSeconds / 2000.0), TimeUnit.MILLISECONDS) // run player retriever offset from data sender
		.execute(new RetrievePlayersTask());
    }

	private List<Addon> loadAddons() {
		final List<Addon> addons = new ArrayList<>();

		for (final File addonFile : this.getAddonsFolder().listFiles()) {
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

//			if (!(addon.getName() + ".class").equals(addonFile.getName())) {
//				throw new RuntimeException(String.format("Addon name mismatch (%s / %s", addon.getName(), addonFile.getName().replace(".class", "")));
//			}

			addon.reloadConfig();
//			addon.onLoad();
			addons.add(addon);
		}

		return addons;
	}

}

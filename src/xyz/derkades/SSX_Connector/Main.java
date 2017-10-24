package xyz.derkades.SSX_Connector;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.gson.Gson;
import com.mitch528.sockets.Sockets.Client;
import com.mitch528.sockets.events.MessageReceivedEvent;
import com.mitch528.sockets.events.MessageReceivedEventListener;
import com.mitch528.sockets.events.SocketConnectedEvent;
import com.mitch528.sockets.events.SocketConnectedEventListener;
import com.mitch528.sockets.events.SocketDisconnectedEvent;
import com.mitch528.sockets.events.SocketDisconnectedEventListener;

public class Main extends JavaPlugin /*implements PluginMessageListener*/ {
	
	public static Main plugin;
	
	private List<Addon> addons;
	
	private Client client;
	
	Gson gson;
	
	@Override
	public void onEnable() {
		plugin = this;
		
		gson = new Gson();
		
		this.addons = loadAddons();
		
		Bukkit.getScheduler().runTaskTimerAsynchronously(Main.this, () -> {
			if (client == null || !client.isConnected()) {
				try {
					initClient();
					
				} catch (Exception e) {
					getLogger().warning("Couldn't connect to server");
					getLogger().warning(e.getMessage());
					return;
				}
			}
			
			getLogger().info("Client is already initialized, sending message...");
			
			try {
				client.sendMessage(getPlaceholdersString());
			} catch (Exception e) {
				getLogger().warning("Cannot send information to server. Is it down?");
				getLogger().warning(e.getMessage());
			}
		}, 5*20, 5*20);
	}
	
	private void initClient() throws Exception {
		String ip = getConfig().getString("ip");
		int port = getConfig().getInt("port");
		
		if (client != null && client.isConnected()) client.disconnect();
			
		client = new Client(ip, port);

		client.getHandler().getConnected().addSocketConnectedEventListener(new SocketConnectedEventListener() {
			public void socketConnected(SocketConnectedEvent evt) {
				getLogger().info("Client - Connected to server!");
			}
		});

		client.getHandler().getMessage().addMessageReceivedEventListener(new MessageReceivedEventListener() {
			public void messageReceived(MessageReceivedEvent evt) {
				getLogger().info("Client - I got the following message: " + evt.getMessage());
				try {
					client.disconnect();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		});

		client.getHandler().getDisconnected().addSocketDisconnectedEventListener(new SocketDisconnectedEventListener() {
			public void socketDisconnected(SocketDisconnectedEvent evt) {
				getLogger().info("Client - Disconnected");
			}
		});

		client.connect();
	}
	
	private String getPlaceholdersString() {
		Map<String, String> placeholders = new HashMap<>();
		
		placeholders.put("server", getConfig().getString("server-name"));
		
		for (Addon addon : addons) {
			placeholders.putAll(addon.getPlaceholders());
		}
		
		return gson.toJson(placeholders);
	}
	
	private List<Addon> loadAddons() {
		List<Addon> addons = new ArrayList<>();
		
		getLogger().info("Loading addons...");
		
		File addonsFolder = new File(getDataFolder() + File.separator + "addons");
		
		addonsFolder.mkdirs();
		
		getLogger().info("[debug] " + addonsFolder.getAbsolutePath());
		
		for (File addonFolder : addonsFolder.listFiles()) {
			getLogger().info("[debug] scanned" + addonFolder.getAbsolutePath());
			
			if (!addonFolder.isDirectory()) {
				getLogger().warning("Non-addon file detected in addons folder: " + addonFolder.getName());
				continue;
			}
			
			File infoFile = new File(addonFolder, "info.yml");
			if (!infoFile.exists()) {
				getLogger().warning("Addon with name " + addonFolder.getName() + " does not have a info.yml file");
				continue;
			}
			
			FileConfiguration infoConfig = YamlConfiguration.loadConfiguration(infoFile);
			String name = infoConfig.getString("name");
			String description = infoConfig.getString("description");
			String author = infoConfig.getString("author");
			String version = infoConfig.getString("version");
			String license = infoConfig.getString("license");
			List<String> requiredPlugins = infoConfig.getStringList("depends");
			
			if (name == null || description == null || author == null || version == null || license == null) {
				getLogger().warning("Addon with name " + addonFolder.getName() + " could not be loaded due to missing information in info.yml");
				continue;
			}
			
			if (!requiredPlugins.isEmpty()) {
				for (String requiredPlugin : requiredPlugins) {
					Plugin plugin = Bukkit.getPluginManager().getPlugin(requiredPlugin);
					if (plugin == null) {
						getLogger().warning("Addon with name " + addonFolder.getName() + " could not be loaded, because it requires " + requiredPlugin + " which you do not have installed.");
						continue;
					}
				}
			}
			
			File codeFile = new File(addonFolder, "code.class");

			if (!codeFile.exists()) {
				getLogger().warning("Addon with name " + addonFolder.getName() + " could not be loaded because it does not contain a code file.");
				continue;
			}
			
			AddonClass addonClass;
			
			try (URLClassLoader loader = new URLClassLoader(new URL[]{addonFolder.toURI().toURL()})){
				getLogger().info("[debug] loading " + loader.getURLs()[0].toString());
				Class<?> clazz = loader.loadClass("code");
				addonClass = (AddonClass) clazz.newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
				e.printStackTrace();
				continue;
			}
			
			Addon addon = new Addon(addonClass, name, description, author, version, license);
			getLogger().info(String.format("Successfully loaded addon %s by %s version %s", name, author, version));
			addons.add(addon);
		}
		
		return addons;
	}

}

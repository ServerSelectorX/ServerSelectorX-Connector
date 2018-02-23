package xyz.derkades.SSX_Connector;

import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import com.google.gson.Gson;

public class Main extends JavaPlugin /*implements PluginMessageListener*/ {
	
	public static Main plugin;
	
	private List<Addon> addons;
	
	private Gson gson;
	
	private BukkitTask sender;
	
	@Override
	public void onEnable() {
		plugin = this;
		
		super.saveDefaultConfig();
		
		gson = new Gson();
		
		this.addons = loadAddons();
		
		getCommand("ssxc").setExecutor(new CommandExecutor() {

			@Override
			public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
				if (args.length == 1 && args[0].equals("reload")) {
					Main.this.reloadConfig();
					sender.sendMessage("The configuration file has been reload");
					return true;
				} else {
					return false;
				}
			}
			
		});
		
		int sendIntervalSeconds = getConfig().getInt("send-interval", 4);
		
		sender = Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> {
			for (String address : getConfig().getStringList("addresses")) {
				sendToServer(address, getPlaceholdersString());
			}
		}, sendIntervalSeconds*20, sendIntervalSeconds*20);

	}
	
	private void sendToServer(String address, String placeholders) {
		try {
			address = "http://" + address;
			
			final String key = getConfig().getString("key");
			final String parameters = String.format("key=%s&data=%s", encode(key), encode(placeholders));
			
			HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Length", parameters.length() + "");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			connection.setDoOutput(true);
			connection.addRequestProperty("User-Agent", "Mozilla/4.0 (compatible; MSIE 6.0; Windows NT 5.0)");
			
			DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
			outputStream.writeBytes(parameters);
			
			if (connection.getResponseCode() == 401) {
				getLogger().severe("The provided key is invalid (" + key + ")");
			}
			
			if (connection.getResponseCode() == 400) {
				getLogger().severe("An error occured. Please report this error.");
				getLogger().severe("Parameters: " + parameters);
			}
		} catch (MalformedURLException e) {
			getLogger().severe("Could not parse URL, is it valid?");
			getLogger().severe(e.getMessage());
		} catch (IOException e) {
			if (getConfig().getBoolean("log-ping-fail", true)) {
				getLogger().warning("Cannot send information to server. Is it down?");
				getLogger().warning(e.getMessage());
			}
		}
	}
	
	@Override
	public void onDisable() {
		sender.cancel();
	}
	
	private String getPlaceholdersString() {
		Map<String, String> placeholders = new HashMap<>();
		placeholders.put("online", Bukkit.getOnlinePlayers() + "");
		placeholders.put("max", Bukkit.getMaxPlayers() + "");
		
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
		
		for (File addonFolder : addonsFolder.listFiles()) {			
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
			
			try (URLClassLoader loader = new URLClassLoader(new URL[]{addonFolder.toURI().toURL()}, this.getClassLoader())){
				Class<?> clazz = loader.loadClass("code");
				addonClass = (AddonClass) clazz.newInstance();
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException | IOException e) {
				e.printStackTrace();
				continue;
			}
			
			Addon addon = new Addon(this, addonClass, addonFolder, name, description, author, version, license);
			getLogger().info(String.format("Successfully loaded addon %s by %s version %s", name, author, version));
			addons.add(addon);
		}
		
		return addons;
	}
	
	private static String encode(Object object) {
		try {
			return URLEncoder.encode(object.toString(), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

}

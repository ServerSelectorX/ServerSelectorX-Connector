package xyz.derkades.serverselectorx.connector;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import com.google.common.collect.Iterables;

public class Main extends JavaPlugin {
	
	public static Main plugin;
	
	private List<Addon> addons;
	
	@Override
	public void onEnable() {
		plugin = this;
		
		this.addons = loadAddons();
		
		getServer().getMessenger().registerOutgoingPluginChannel(this, "ServerSelectorX");
		
		Bukkit.getScheduler().runTaskTimer(this, () -> {
			try {
				sendPlaceholdersToBungee();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, 20, 5*20);
	}
	
	private List<Addon> loadAddons() {
		List<Addon> addons = new ArrayList<>();
		
		File addonsFolder = new File(getDataFolder() + "/addons");
		
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
			
			try (URLClassLoader loader = new URLClassLoader(new URL[]{codeFile.toURI().toURL()});){
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
	
	private void sendPlaceholdersToBungee() throws IOException {
		for (Addon addon : addons) {
			for (Map.Entry<String, String> entry : addon.getPlaceholders().entrySet()) {
				sendToBungee(entry.getKey(), entry.getValue());
			}
		}
	}
	
	private void sendToBungee(String placeholder, String output) throws IOException {
		Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null); //We don't care about which player the message is sent from
		ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(baos);		
	    dos.writeUTF("Placeholder");
	    dos.writeUTF(placeholder);
	    dos.writeUTF(output);
	    player.sendPluginMessage(this, "ServerSelectorX", baos.toByteArray());
	}

}

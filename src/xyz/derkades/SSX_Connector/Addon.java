package xyz.derkades.SSX_Connector;

import java.io.File;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Addon {
	
	private AddonClass addonClass;
	private File directory;
	private String name;
	private String description;
	private String author;
	private String version;
	private String license;
	
	public Addon(Main plugin, AddonClass addonClass, File directory, String name, String description, String author, String version, String license){
		this.addonClass = addonClass;
		this.directory = directory;
		this.name = name;
		this.description = description;
		this.author = author;
		this.version = version;
		this.license = license;
		
		loadConfig();
		Bukkit.getPluginManager().registerEvents(addonClass, plugin);
		addonClass.onLoad();
	}
	
	String getName() {
		return name;
	}
	
	String getDescription() {
		return description;
	}
	
	String getAuthor() {
		return author;
	}
	
	String getVersion() {
		return version;
	}
	
	String getLicense() {
		return license;
	}
	
	Map<String, String> getPlaceholders(){
		return addonClass.getPlaceholders();
	}
	
	FileConfiguration loadConfig() {		
		if (addonClass.getConfigDefaults() == null) {
			throw new UnsupportedOperationException("Can't request config if it has not been created. If you want a configuration file to be created, return config defaults.");
		}
		
		File file = new File(directory, "config.yml");
		if (!file.exists()) {
			addonClass.config = new YamlConfiguration();
			addonClass.config.addDefault("info", "If you don't understand how to configure this addon, maybe the developer has put instructions in info.yml");
			for (Map.Entry<String, String> entry : addonClass.getConfigDefaults().entrySet()) {
				addonClass.config.addDefault(entry.getKey(), entry.getValue());
			}	
		} else {
			addonClass.config = YamlConfiguration.loadConfiguration(file);
		}
		
		return addonClass.config;
	}

}

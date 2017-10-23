package xyz.derkades.serverselectorx.connector;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class Addon {
	
	//private Class<? extends AddonClass> classFile;
	private AddonClass addonClass;
	private String name;
	//private List<String> requiredPlugins;
	private String description;
	private String author;
	private String version;
	private String license;
	
	private FileConfiguration config;
	
	public Addon(AddonClass addonClass, String name, String description, String author, String version, String license){
		//this.classFile = classFile;
		this.addonClass = addonClass;
		//this.name = name;
		//this.requiredPlugins = requiredPlugins;
		this.name = name;
		this.description = description;
		this.author = author;
		this.version = version;
		this.license = license;
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
	
	FileConfiguration getConfig() {
		if (config != null) {
			return config;
		}
		
		File file = new File(Main.plugin.getDataFolder(), name + ".yml");
		if (!file.exists()) {
			config = new YamlConfiguration();
			config.addDefault("info", "If you don't understand how to configure this addon, maybe the developer has put instructions in info.yml");
			for (Map.Entry<String, String> entry : addonClass.getConfigDefaults().entrySet()) {
				config.addDefault(entry.getKey(), entry.getValue());
			}	
		} else {
			config = YamlConfiguration.loadConfiguration(file);
		}
		
		return config;
	}
	
	void reloadConfig() {
		config = null;
	}
	
	void saveConfig() {
		try {
			File file = new File(Main.plugin.getDataFolder(), name + ".yml");
			config.save(file);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}

package xyz.derkades.SSX_Connector;

import java.io.File;
import java.util.Map;

import org.bukkit.Bukkit;
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
	
	void loadConfig() {		
		File file = new File(directory, "config.yml");
		addonClass.config = YamlConfiguration.loadConfiguration(file);
	}

}

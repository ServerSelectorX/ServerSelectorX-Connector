package xyz.derkades.SSX_Connector;

import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.Listener;

public abstract class AddonClass implements Listener {
	
	protected FileConfiguration config;
	
	public abstract Map<String, String> getPlaceholders();
	
	public abstract Map<String, String> getConfigDefaults();
	
	public void onLoad() {
		
	}

}

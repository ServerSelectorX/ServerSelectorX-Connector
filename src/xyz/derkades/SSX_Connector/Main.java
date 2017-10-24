package xyz.derkades.SSX_Connector;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

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
	
	@Override
	public void onEnable() {
		plugin = this;
		
		this.addons = loadAddons();
		
		String ip = getConfig().getString("ip");
		int port = getConfig().getInt("port");
		
		client = new Client(ip, port);
		
		client.getHandler().getConnected().addSocketConnectedEventListener(new SocketConnectedEventListener(){
			public void socketConnected(SocketConnectedEvent evt){
				getLogger().info("Client - Connected to server!");
				getLogger().info("Client - Sending message to server.");
				
				Bukkit.getScheduler().runTaskTimer(Main.this, () -> {
					client.sendMessage("Hello World!");
				}, 20, 20);
			}
		});
		
		client.getHandler().getMessage().addMessageReceivedEventListener(new MessageReceivedEventListener(){
			public void messageReceived(MessageReceivedEvent evt){
				getLogger().info("Client - I got the following message: " + evt.getMessage());
				client.disconnect();
			}
		});
		
		client.getHandler().getDisconnected().addSocketDisconnectedEventListener(new SocketDisconnectedEventListener(){
			public void socketDisconnected(SocketDisconnectedEvent evt){
				getLogger().info("Client - Disconnected");
			}
		});
		
		connect();
		
		/*getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");
		getServer().getMessenger().registerIncomingPluginChannel(this, "BungeeCord", this);
		
		askBungeeForServerName();
		
		Bukkit.getScheduler().runTaskTimer(this, () -> {
			if (serverName == null) {
				getLogger().warning("Server name not yet recieved from BungeeCord");
				askBungeeForServerName();
				return;
			}
			
			try {
				sendPlaceholdersToBungee();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}, 20, 20);*/
	}
	
	private void connect() {
		try {
			client.connect();
			getLogger().info("Connected!");
		} catch (Exception e) {
			getLogger().info("Couldn't connect to server, retrying in 5 seconds.");
			Bukkit.getScheduler().runTaskLater(this, () -> {
				connect();
			}, 5* 20);
		}
	}
	
	/*private void askBungeeForServerName() {
		//Send request for server name to bungee, received down below.
		ByteArrayDataOutput out = ByteStreams.newDataOutput();
		out.writeUTF("GetServer");
		//Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null); // We don't care about the player
		Bukkit.getServer().sendPluginMessage(this, "BungeeCord", out.toByteArray());
	}
	
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();
		if (subchannel.equals("GetServer")) {
			String serverName = in.readUTF();
			this.serverName = serverName;
		}
	}*/
	
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
	
	/*private void sendPlaceholdersToBungee() throws IOException {
		for (Addon addon : addons) {
			for (Map.Entry<String, String> entry : addon.getPlaceholders().entrySet()) {
				sendToBungee(entry.getKey(), entry.getValue());
			}
		}
	}*/
	
	//private void sendToBungee(String placeholder, String output) throws IOException {
		//Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null); //We don't care about which player the message is sent from
		/*ByteArrayOutputStream baos = new ByteArrayOutputStream(); 
		DataOutputStream dos = new DataOutputStream(baos);			    
		dos.writeUTF("Forward");
	    dos.writeUTF("ALL");
	    dos.writeUTF("ServerSelectorX-Placeholder");

	    ByteArrayOutputStream msgbytes = new ByteArrayOutputStream();
	    DataOutputStream out = new DataOutputStream(msgbytes);
	    out.writeUTF(serverName);
	    out.writeUTF(placeholder);
	    out.writeUTF(output);

	    dos.writeShort(msgbytes.toByteArray().length);
	    dos.write(msgbytes.toByteArray());
	    Bukkit.getServer().sendPluginMessage(this, "BungeeCord", baos.toByteArray());*/
	//}

}

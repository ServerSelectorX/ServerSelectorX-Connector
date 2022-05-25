package xyz.derkades.ssx_connector;

import com.google.gson.Gson;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.*;

public class PlaceholderSender implements Runnable {
	
	private final Deque<String> addresses = new ArrayDeque<>();

	@Override
	public void run() {
		final FileConfiguration config = Main.instance.getConfig();
		
		// Only send request to one address every time. When requests have been
		// sent to all addresses, repopulate the stack so the cycle can start over.
		
		if (this.addresses.isEmpty()) {
			Main.instance.addresses().forEach(this.addresses::push);
			
			// If the user did not configure any addresses
			if (this.addresses.isEmpty()) {
				return;
			}
		}
		
		final String address = this.addresses.pop();
		
		debug(address, "Preparing to send data");
		
		final String serverName = config.getString("server-name");
		
		debug(address, "Using server name '" + serverName + "'");
		
		if (serverName.isEmpty()) {
			debug(address, "Server name is empty! Not sending data");
			return;
		}
		
		debug(address, "Retrieving player list..");
		
		// First get a list of players so we know which player placeholders to send
		final Map<UUID, String> players;
		
		try {
			players = getPlayerList(address);
		} catch (final MalformedURLException e) {
			PingLogger.logFail(address, "Invalid address");
			debug(e);
			return;
		} catch (final IOException e) {
			PingLogger.logFail(address, "IOException: " + e.getMessage());
			debug(e);
			return;
		}
		
		debug(address, "Done. (" + players.size() + " players)");
		players.forEach((uuid, name) -> debug(address, " - " + uuid + ":" + name));
	
		debug(address, "Collecting placeholders..");
		
		PlaceholderRegistry.collectPlaceholders(players, placeholders ->
			// This callback is executed synchronously, go async to send placeholders
			Bukkit.getScheduler().runTaskAsynchronously(Main.instance, () -> {
				try {
					sendPlaceholders(address, serverName, placeholders);
				} catch (final MalformedURLException e) {
					PingLogger.logFail(address, "Invalid address");
					debug(e);
					return;
				} catch (final IOException e) {
					PingLogger.logFail(address, "IOException: " + e.getMessage());
					debug(e);
					return;
				}
	
				PingLogger.logSuccess(address);
				
				Main.sendAmount++;
				
				debug(address, "Data sent!");
			})
		);
	}
	
	private void sendPlaceholders(final String address, final String serverName,
			final Map<String, Object> placeholders) throws IOException {
		final String json = new Gson().toJson(placeholders).toString();
		debug(address, "Placeholders json: " + json);
		final String parameters = String.format("server=%s&data=%s", this.encode(serverName), this.encode(json));

		final HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setConnectTimeout(1000);
		connection.setReadTimeout(1000);

		final DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
		outputStream.writeBytes(parameters);
		
		if (connection.getResponseCode() != 200) {
			throw new IOException("Response code " + connection.getResponseCode());
		}
	}
	
	private Map<UUID, String> getPlayerList(final String address) throws IOException {
		final HttpURLConnection connection = (HttpURLConnection) new URL(address + "/players").openConnection();
		connection.setConnectTimeout(1000);
		connection.setReadTimeout(1000);

		if (connection.getResponseCode() != 200) {
			throw new IOException("Response code " + connection.getResponseCode());
		}
		
		final InputStream inputStream = connection.getInputStream();
		final BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		final StringBuilder responseBuilder = new StringBuilder();

		String responseString;
		while ((responseString = streamReader.readLine()) != null) {
			responseBuilder.append(responseString);
		}
		
		inputStream.close();

		final Map<?, ?> map = new Gson().fromJson(responseBuilder.toString(), Map.class);
		final Map<UUID, String> map2 = new HashMap<>();
		map.forEach((k, v) -> map2.put(UUID.fromString(String.valueOf(k)), String.valueOf(v)));
		return map2;
	}

	private String encode(final Object object) {
		try {
			return URLEncoder.encode(object.toString(), "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}
	
	private void debug(final String address, final String message) {
		if (Main.instance.getConfig().getBoolean("debug", false)) {
			Main.instance.getLogger().info("[Debug] " + address + " - " + message);
		}
	}
	
	private void debug(final Exception e) {
		if (Main.instance.getConfig().getBoolean("debug", false)) {
			e.printStackTrace();
		}
	}

}

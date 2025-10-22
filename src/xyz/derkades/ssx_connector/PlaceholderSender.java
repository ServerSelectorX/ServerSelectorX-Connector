package xyz.derkades.ssx_connector;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import xyz.derkades.ssx_connector.PlaceholderRegistry.GlobalPlaceholder;
import xyz.derkades.ssx_connector.PlaceholderRegistry.Placeholder;
import xyz.derkades.ssx_connector.PlaceholderRegistry.PlayerPlaceholder;

public class PlaceholderSender implements Runnable {

	/**
	 * List of player uuids to send player-specific placeholders for.
	 */
	private Set<UUID> playerUuids;

	@Override
	public void run() {
		final FileConfiguration config = Main.instance.getConfig();

		// Only send request to one address every time. When requests have been
		// sent to all addresses, repopulate the stack so the cycle can start over.

		final String networkId = config.getString("network-id");
		final String serverName = config.getString("server-name");
		final List<String> addresses = config.getStringList("placeholder-servers");

		if (networkId == null || networkId.isEmpty()) {
			this.debug("network-id is not configured");
			return;
		}

		if (serverName == null || serverName.isEmpty()) {
			this.debug("server-name is not configured");
			return;
		}

		this.debug("server-name = " + serverName);

		// First get a list of players so we know which player placeholders to send
		this.debug("players: " + String.join(", " + this.playerUuids));

		// Collect placeholders
		final JsonObject placeholdersJson = new JsonObject();

		for (final Placeholder placeholder : PlaceholderRegistry.getPlaceholders()) {
			final String key = placeholder.getKey();

			try {
				if (placeholder instanceof PlayerPlaceholder) {
					final JsonObject valuesObject = new JsonObject();
					for (final UUID uuid : this.playerUuids) {
						final String value = ((PlayerPlaceholder) placeholder).getValue(uuid);
						if (value != null) {
							valuesObject.addProperty(key, value);
						}
					}
					placeholdersJson.add(key, valuesObject);
				} else {
					final String value = ((GlobalPlaceholder) placeholder).getValue();
					if (value != null) {
						placeholdersJson.addProperty(key, value);
					}
				}
			} catch (final Exception e) {
				Main.instance.getLogger().warning("An error occured while retrieving placeholder " + key + ". This is probably a bug " +
					"in the plugin or expansion that added this placeholder (not in SSX-Connector).");
				Main.instance.getLogger().warning("Note that not all plugins work properly when no players are online on a server.");
				e.printStackTrace();
			}
		}

		final JsonObject json = new JsonObject();
		json.addProperty("network", networkId);
		json.addProperty("server", serverName);
		json.add("placeholders", placeholdersJson);

		final String jsonString = json.toString();

		this.debug("sending json: " + jsonString);

		final byte[] data = jsonString.getBytes();

		// Go async to send placeholders
		Bukkit.getScheduler().runTaskAsynchronously(Main.instance, () -> {
			this.sendPlaceholders(addresses, data);
		});
	}

	private void sendPlaceholders(final List<String> addresses, final byte[] data) {
		final Set<UUID> players = new HashSet<>();
		for (final String address : addresses) {
			try {
				final String[] partialPlayers = this.sendPlaceholdersTo(address, data);
				for (final String uuidString : partialPlayers) {
					players.add(UUID.fromString(uuidString));
				}
				PingLogger.logSuccess(address);
				this.debug("data sent to: " + address);
			} catch (final IOException e) {
				PingLogger.logFail(address, "IOException: " + e.getMessage());
				this.debug(e);
			}
		}
		this.playerUuids = players;
	}

	private String[] sendPlaceholdersTo(final String address, final byte[] data) throws IOException {
		final HttpURLConnection connection = (HttpURLConnection) URI.create(address + "/connector").toURL().openConnection();
		connection.setRequestMethod("POST");
		connection.setDoOutput(true);
		connection.setConnectTimeout(1000);
		connection.setReadTimeout(1000);

		try (OutputStream out = connection.getOutputStream()) {
			out.write(data);
		}

		if (connection.getResponseCode() != 200) {
			throw new IOException("Response code " + connection.getResponseCode());
		}

		byte[] responseData;
		try (InputStream in = connection.getInputStream()) {
			responseData = in.readAllBytes();
		}
		final JsonObject responseJson = new JsonParser().parse(new String(responseData)).getAsJsonObject();
		final JsonArray jsonPlayersArray = responseJson.get("players").getAsJsonArray();
		final String[] players = new String[jsonPlayersArray.size()];
		for (int i = 0; i < jsonPlayersArray.size(); i++) {
			players[i] = jsonPlayersArray.get(i).getAsString();
		}
		return players;
	}

	private void debug(final String message) {
		if (Main.instance.getConfig().getBoolean("debug", false)) {
			Main.instance.getLogger().info("[Debug] " + message);
		}
	}

	private void debug(final Exception e) {
		if (Main.instance.getConfig().getBoolean("debug", false)) {
			e.printStackTrace();
		}
	}

}

package xyz.derkades.ssx_connector;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.Gson;

public class PlaceholderSender implements Runnable {

	@Override
	public void run() {
		final FileConfiguration config = Main.instance.getConfig();
		final Logger logger = Main.instance.getLogger();

		final Map<String, Object> placeholders = new HashMap<>();

		Main.placeholders.forEach((k, v) -> placeholders.put(k, v.get()));

		Main.playerPlaceholders.forEach((k, v) -> {
			final Map<String, String> playerValues = new HashMap<>();
			Main.players.forEach((uuid, name) -> {
				playerValues.put(uuid.toString(), v.apply(uuid, name));
			});
			placeholders.put(k, playerValues);
		});

		final String json = new Gson().toJson(placeholders).toString();

		for (String address : config.getStringList("addresses")) {
			try {
				address = "http://" + address;

				final String password = config.getString("password");
				final String name = config.getString("server-name");
				final String parameters = String.format("password=%s&server=%s&data=%s", this.encode(password), this.encode(name), this.encode(json));

				final HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
				connection.setRequestMethod("POST");
				connection.setRequestProperty("Content-Length", parameters.length() + "");
				connection.setDoOutput(true);

				final DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
				outputStream.writeBytes(parameters);

				if (connection.getResponseCode() == 401) {
					Main.lastPingErrors.put(address, "Invalid password");
					logger.severe("[PlaceholderSender] The provided password is invalid (" + password + ")");
					return;
				}

				if (connection.getResponseCode() == 400) {
					Main.lastPingErrors.put(address, "Error 400 (plugin bug)");
					logger.severe("[PlaceholderSender] An error 400 occured. Please report this error.");
					logger.severe("[PlaceholderSender] " + address);
					logger.severe("[PlaceholderSender] Parameters: " + parameters);
					continue;
				}

				Main.lastPingErrors.put(address, null);
				Main.lastPingTimes.put(address, System.currentTimeMillis());
			} catch (final MalformedURLException e) {
				Main.lastPingErrors.put(address, "[PlaceholderSender] Invalid URL: " + address);
			} catch (final IOException e) {
				Main.lastPingErrors.put(address, "[PlaceholderSender] IOException: " + e.getMessage());
			}
		}
	}

	private String encode(final Object object) {
		try {
			return URLEncoder.encode(object.toString(), "UTF-8");
		} catch (final UnsupportedEncodingException e) {
			throw new RuntimeException(e.getMessage());
		}
	}

}

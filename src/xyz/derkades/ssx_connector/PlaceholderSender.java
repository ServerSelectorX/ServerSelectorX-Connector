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

//		final Map<String, Map<String, String>> placeholders = new HashMap<>();
//
//		final Map<String, String> globalPlaceholders = new HashMap<>();
//		Main.placeholders.forEach((k, v) -> globalPlaceholders.put(k, v.get()));
//		placeholders.put("global", globalPlaceholders);
//
//		Main.players.forEach((uuid, name) -> {
//			final Map<String, String> playerPlaceholders = new HashMap<>();
//			Main.playerPlaceholders.forEach((k, v) -> playerPlaceholders.put(k, v.apply(uuid, name)));
//			placeholders.put(uuid.toString(), playerPlaceholders);
//		});

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
					logger.severe("The provided password is invalid (" + password + ")");
					return;
				}

				if (connection.getResponseCode() == 400) {
					logger.severe("An error occured. Please report this error.");
					logger.severe(address);
					logger.severe("Parameters: " + parameters);
					continue;
				}
			} catch (final MalformedURLException e) {
				logger.severe("Could not parse URL, is it valid? (" + address + ")");
			} catch (final IOException e) {
				if (config.getBoolean("log-ping-fail", true)) {
					logger.warning("Cannot send information to server. Is it down? " + e.getMessage());
				}
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

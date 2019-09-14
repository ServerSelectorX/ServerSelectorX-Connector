package xyz.derkades.ssx_connector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import org.bukkit.configuration.file.FileConfiguration;

import com.google.gson.Gson;

public class RetrievePlayersTask implements Runnable {

	@Override
	public void run() {
		final FileConfiguration config = Main.instance.getConfig();
		final Logger logger = Main.instance.getLogger();

		for (String address : config.getStringList("addresses")) {
			try {
				final String password = this.encode(config.getString("password"));
				address = "http://" + address + "?password=" + password;

				final HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
				//connection.setRequestProperty("Content-Length", parameters.length() + "");

				if (connection.getResponseCode() == 401) {
					logger.severe("The provided password is invalid (" + password + ")");
					continue;
				}

				if (connection.getResponseCode() == 400) {
					logger.severe("An error occured (http bad request). Please report this error.");
					logger.severe(address);
					continue;
				}

				final InputStream inputStream = connection.getInputStream();
				final BufferedReader streamReader = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
				final StringBuilder responseBuilder = new StringBuilder();

				String responseString;
				while ((responseString = streamReader.readLine()) != null) {
					responseBuilder.append(responseString);
				}

				final Map<?, ?> map = new Gson().fromJson(responseBuilder.toString(), Map.class);

				Main.players.clear();

				map.forEach((k, v) -> Main.players.put(UUID.fromString(String.valueOf(k)), String.valueOf(v)));

				inputStream.close();
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

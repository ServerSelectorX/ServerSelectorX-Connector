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

import com.google.gson.Gson;

import ninja.leaping.configurate.commented.CommentedConfigurationNode;

public class RetrievePlayersTask implements Runnable {

	@Override
	public void run() {
		final CommentedConfigurationNode config = Main.instance.config;
		final Logger logger = Main.instance.logger;

		for (final String address : config.getNode("addresses").getList((o) -> String.valueOf(o))) {
			try {
				final String password = this.encode(config.getNode("password").getString("a"));

				Main.lastPlayerRetrieveTimes.put(address, System.currentTimeMillis());

				final HttpURLConnection connection = (HttpURLConnection) new URL("http://" + address + "/players?password=" + password).openConnection();
				//connection.setRequestProperty("Content-Length", parameters.length() + "");

				if (connection.getResponseCode() == 401) {
					Main.lastPlayerRetrieveErrors.put(address, "Invalid password");
					logger.warning("[PlayerRetriever] The provided password is invalid (" + password + ")");
					continue;
				}

				if (connection.getResponseCode() == 400) {
					Main.lastPlayerRetrieveErrors.put(address, "Error 400 (plugin bug)");
					logger.warning("[PlayerRetriever] An error 400 occured. Please report this error.");
					logger.warning(address);
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

				Main.lastPlayerRetrieveErrors.put(address, null);
			} catch (final MalformedURLException e) {
				Main.lastPlayerRetrieveErrors.put(address, "[PlayerRetriever] Invalid URL: " + address);
			} catch (final IOException e) {
				Main.lastPlayerRetrieveErrors.put(address, "[PlayerRetriever] IOException: " + e.getMessage());
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

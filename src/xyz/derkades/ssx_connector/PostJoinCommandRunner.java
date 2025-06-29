package xyz.derkades.ssx_connector;

import com.google.common.io.ByteStreams;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

public class PostJoinCommandRunner implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(final PlayerJoinEvent event) {
		if (!Main.instance.getConfig().getBoolean("post-join-command")) {
			return;
		}

		final List<String> addresses = Main.instance.addresses();
		if (addresses.size() != 1) {
			Main.instance.getLogger().warning("Skipped querying for post join command, multiple addresses are not supported");
			return;
		}
		final String baseAddress = addresses.get(0);
		final UUID uuid = event.getPlayer().getUniqueId();
		final String playerName = event.getPlayer().getName();

		Main.instance.getScheduler().async().runNow(() -> {
			try {
				final String address = baseAddress + "/post-join-command?uuid=" + uuid;
				final HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();
				connection.setReadTimeout(5000);
				connection.setConnectTimeout(5000);
				connection.connect();

				if (connection.getResponseCode() == 204) {
					Main.instance.getLogger().info("No post join command");
					return;
				}

				if (connection.getResponseCode() != 200) {
					Main.instance.getLogger().warning("Unexpected status code during post join command request: " + connection.getResponseCode());
				}

				final byte[] responseBytes;
				try (final InputStream input = connection.getInputStream()) {
					responseBytes = ByteStreams.toByteArray(input);
				}

				final String commandToRun = new String(responseBytes, StandardCharsets.UTF_8).replace("{player}", playerName);

				Main.instance.getScheduler().async().runNow(() -> {
					Main.instance.getLogger().info("Going to run command:");
					if (Bukkit.getPlayer(uuid) == null) {
						Main.instance.getLogger().warning("Player is no longer online?");
						return;
					}

					Bukkit.dispatchCommand(Bukkit.getConsoleSender(), commandToRun);
				});
			} catch (IOException e) {
				Main.instance.getLogger().warning("Failed to query for post join command");
				e.printStackTrace();
			}
		});
	}

}

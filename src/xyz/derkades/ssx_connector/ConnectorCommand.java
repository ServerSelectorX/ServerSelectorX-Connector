package xyz.derkades.ssx_connector;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import net.md_5.bungee.api.ChatColor;
import xyz.derkades.ssx_connector.PingLogger.PingFail;
import xyz.derkades.ssx_connector.PingLogger.PingSuccess;
import xyz.derkades.ssx_connector.PlaceholderRegistry.Placeholder;

public class ConnectorCommand implements CommandExecutor {

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (!sender.hasPermission("ssxc.admin")) {
			sender.sendMessage("No permission");
			return true;
		}
		
		if (args.length == 1 && (args[0].equals("reload") || args[0].equals("rl"))) {
			Main.instance.reloadConfig();
			Main.instance.loadAddons();
			Main.instance.restartPingTask();
			PingLogger.clear();
			sender.sendMessage("The plugin configuration file and addon configuration files have been reloaded. A complete server reload or restart is required for the (de)installation of addons.");
			return true;
		}

		if (args.length == 1 && args[0].equals("addons")) {
			if (Main.instance.addons.isEmpty()) {
				sender.sendMessage("There are no addons installed. The placeholders {online} and {max} are always available.");
				return true;
			}

			sender.sendMessage("The following addons are installed:");

			Main.instance.addons.forEach((ign, addon) -> sender.sendMessage(String.format(
					"%s by %s version %s: '%s'",
					addon.getName(), addon.getAuthor(), addon.getVersion(),
					addon.getDescription())));

			sender.sendMessage("To view placeholders for each addon, use the command /ssxc placeholders <addon>.");
			return true;
		}

		if (args.length == 2 && args[0].equals("placeholders")) {
			final Addon addon = Main.instance.addons.get(args[1]);

			if (addon == null) {
				sender.sendMessage("No addon is installed with the name '" + args[1] + "'.");
				sender.sendMessage("Get a list of installed addons using /ssxc addons");
				return true;
			}
			
			final Addon addonF = addon;
			
			final List<String> placeholders = PlaceholderRegistry.stream()
					.filter(p -> p.isFromAddon())
					.filter(p -> p.getAddon() == addonF)
					.map(Placeholder::getKey)
					.collect(Collectors.toList());

			if (placeholders.isEmpty()) {
				sender.sendMessage("This addon has not registered any placeholders.");
				return true;
			}

			sender.sendMessage(String.join(", ", placeholders));
			return true;
		}

		if (args.length == 1 && args[0].equals("status")) {
			final FileConfiguration config = Main.instance.getConfig();
			if (config.getStringList("addresses").isEmpty()) {
				sender.sendMessage("No addresses configured in config.yml, not sending data");
				return true;
			}
			
			if (config.getString("server-name").isEmpty()) {
				sender.sendMessage("The server-name option is blank, not sending data");
				return true;
			}
			
			if (PingLogger.isEmpty()) {
				sender.sendMessage("No data has been sent to servers");
			} else {
				sender.sendMessage("Data send history:");
				PingLogger.forEach((address, status) -> {
					final long ago = System.currentTimeMillis() - status.getTime();
					if (status instanceof PingSuccess) {
						sender.sendMessage(ChatColor.GREEN + String.format("  %s: Pinged successfully %sms ago.",
								address, ago));
					} else {
						sender.sendMessage(ChatColor.RED + String.format("  %s: Ping failed %sms ago with message \"%s\"",
								address, ago, ((PingFail) status).getMessage()));
					}
				});
			}
			return true;
		}
		
		if ((args.length == 1 || args.length == 2) && args[0].equals("count")) {
			final int seconds;
			if (args.length == 2) {
				try {
					seconds = Integer.parseInt(args[1]);
					if (seconds <= 0) {
						sender.sendMessage("Number must be positive");
						return true;
					}
				} catch (final NumberFormatException e) {
					sender.sendMessage("Invalid number '" + args[1] + "'");
					return true;
				}
			} else {
				seconds = 5;
			}
			
			sender.sendMessage("Measuring average placeholders per second over a period of " + seconds + " seconds..");
			Main.placeholdersUncached = 0;
			Main.placeholdersCached = 0;
			Main.sendAmount = 0;

			Main.instance.getScheduler().global().runDelayed(() -> {
				final int addresses = Main.instance.getConfig().getStringList("addresses").size();
				final int interval = Main.instance.getConfig().getInt("send-interval");
				final int ticks = (interval * 20) / addresses;
				sender.sendMessage("Sent data " + Main.sendAmount + " times (task is configured to run once every " + ticks + " ticks");
				sender.sendMessage("Placeholders collected: " + Main.placeholdersUncached + " (" + Main.placeholdersUncached/seconds + "/s)");
				sender.sendMessage("Placeholders from cache: " + Main.placeholdersCached + " (" + Main.placeholdersCached/seconds + "/s)");
				final int total = Main.placeholdersUncached+Main.placeholdersCached;
				sender.sendMessage("Total placeholders sent: " + total + " (" + total/seconds + "/s)");
				if (total > 0) {
					sender.sendMessage(String.format("Cache ratio: %.1f%%", ((float) Main.placeholdersCached / total) * 100));
				}
			}, seconds * 20);
			return true;
		}

		return false;
	}

}

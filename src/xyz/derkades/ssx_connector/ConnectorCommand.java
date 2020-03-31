package xyz.derkades.ssx_connector;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;
import xyz.derkades.ssx_connector.PingLogger.PingFail;
import xyz.derkades.ssx_connector.PingLogger.PingSuccess;
import xyz.derkades.ssx_connector.PlaceholderRegistry.Placeholder;

public class ConnectorCommand implements CommandExecutor {

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (args.length == 1 && (args[0].equals("reload") || args[0].equals("rl")) && sender.hasPermission("ssxc.reload")) {
			Main.instance.reloadConfig();
			Main.instance.reloadAddons();
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

			Main.instance.addons.forEach((addon) -> sender.sendMessage(String.format(
					"%s by %s version %s license %s: '%s'",
					addon.getName(), addon.getAuthor(), addon.getVersion(),
					addon.getLicense(), addon.getDescription())));

			sender.sendMessage("To view placeholders for each addon, use the command /ssxc placeholders <addon>.");
			return true;
		}

		if (args.length == 2 && args[0].equals("placeholders")) {
			Addon addon = null;
			for (final Addon addon2 : Main.instance.addons) {
				if (addon2.getName().equalsIgnoreCase(args[1])) {
					addon = addon2;
				}
			}

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

		if (args.length == 1 && args[0].equals("status") && sender.hasPermission("ssxc.status")) {
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
		
		if ((args.length == 1 || args.length == 2) &&
				args[0].equals("count") && sender.hasPermission("ssxc.count")) {
			final int seconds;
			if (args.length == 2) {
				try {
					seconds = Integer.parseInt(args[1]);
				} catch (final NumberFormatException e) {
					sender.sendMessage("Invalid number '" + args[1] + "'");
					return true;
				}
			} else {
				seconds = 5;
			}
			
			sender.sendMessage("Measuring average placeholders per second over a period of " + seconds + " seconds..");
			Main.placeholders = 0;
			Bukkit.getScheduler().scheduleSyncDelayedTask(Main.instance, () -> {
				sender.sendMessage("Placeholders collected: " + Main.placeholders + " (" + Main.placeholders/seconds + "/s)");
				sender.sendMessage("Placeholders from cache: " + Main.placeholdersCached + " (" + Main.placeholdersCached/seconds + "/s)");
				sender.sendMessage("Total placeholders sent: " + (Main.placeholders+Main.placeholdersCached) + " (" + (Main.placeholders+Main.placeholdersCached)/seconds + "/s)");
				sender.sendMessage(String.format("Cache ratio: %.1f%%", 100 - ((float) Main.placeholders / (Main.placeholdersCached > 0 ? Main.placeholdersCached : 1)) * 100));
			}, seconds * 20);
			return true;
		}

		return false;
	}

}

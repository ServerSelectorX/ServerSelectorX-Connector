package xyz.derkades.ssx_connector;

import java.util.List;
import java.util.stream.Collectors;

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

			final List<String> placeholders = PlaceholderRegistry.getPlaceholders()
					.stream()
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
			if (config.getString("network-id", "").isEmpty()) {
				sender.sendMessage("No network-id configured in config.yml");
				return true;
			}

			if (config.getString("server-name", "").isEmpty()) {
				sender.sendMessage("No server-name configured in config.yml");
				return true;
			}

			if (config.getStringList("placeholder-servers").isEmpty()) {
				sender.sendMessage("No placeholder-servers configured in config.yml");
				return true;
			}

			if (PingLogger.isEmpty()) {
				sender.sendMessage("No data has been sent");
			} else {
				PingLogger.forEach((address, status) -> {
					final long ago = System.currentTimeMillis() - status.getTime();
					if (status instanceof PingSuccess) {
						sender.sendMessage(String.format("%s%s: working (%sms ago)", ChatColor.GREEN, address, ago));
					} else {
						sender.sendMessage(String.format("%s%s: failed (%sms ago): %s", ChatColor.RED, address, ago, ((PingFail) status).getMessage()));
					}
				});
			}
			return true;
		}

		return false;
	}

}

package xyz.derkades.ssx_connector;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;
import xyz.derkades.ssx_connector.PingLogger.PingFail;
import xyz.derkades.ssx_connector.PingLogger.PingSuccess;

public class ConnectorCommand implements CommandExecutor {

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (args.length == 1 && (args[0].equals("reload") || args[0].equals("rl")) && sender.hasPermission("ssxc.reload")) {
			Main.instance.reloadConfig();
//			Main.instance.addons.forEach(Addon::reloadConfig);
			Main.instance.reloadAddons();
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

			if (!Main.addonPlaceholders.containsKey(addon)) {
				sender.sendMessage("This addon has not registered any placeholders.");
				return true;
			}

			sender.sendMessage(String.join(", ", Main.addonPlaceholders.get(addon)));

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

		return false;
	}

}

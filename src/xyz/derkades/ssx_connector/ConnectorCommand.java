package xyz.derkades.ssx_connector;

import org.bukkit.command.CommandSender;
import org.spongepowered.api.command.spec.CommandExecutor;

public class ConnectorCommand implements CommandExecutor {

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args) {
		if (args.length == 1 && args[0].equals("reload") && sender.hasPermission("ssxc.reload")) {
			Main.instance.reloadConfig();
			Main.instance.addons.forEach(Addon::reloadConfig);
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

			return true;
		}

		if (args.length == 1 && args[0].equals("status") && sender.hasPermission("ssxc.status")) {

		}

		return false;
	}

}

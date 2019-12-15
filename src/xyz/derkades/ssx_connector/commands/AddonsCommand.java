package xyz.derkades.ssx_connector.commands;

import java.util.List;
import java.util.Optional;

import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import xyz.derkades.ssx_connector.Main;

public class AddonsCommand implements CommandCallable {

	@Override
	public CommandResult process(final CommandSource source, final String arguments) throws CommandException {
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

	@Override
	public List<String> getSuggestions(final CommandSource source, final String arguments, final Location<World> targetPosition)
			throws CommandException {
		return null;
	}

	@Override
	public boolean testPermission(final CommandSource source) {
		return false;
	}

	@Override
	public Optional<Text> getShortDescription(final CommandSource source) {
		return null;
	}

	@Override
	public Optional<Text> getHelp(final CommandSource source) {
		return null;
	}

	@Override
	public Text getUsage(final CommandSource source) {
		return null;
	}

}

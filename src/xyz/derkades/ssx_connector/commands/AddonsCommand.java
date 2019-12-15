package xyz.derkades.ssx_connector.commands;

import java.util.Arrays;
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
			source.sendMessage(Text.of("There are no addons installed. The placeholders {online} and {max} are always available."));
			return CommandResult.success();
		}

		source.sendMessage(Text.of("The following addons are installed:"));

		Main.instance.addons.forEach((addon) -> source.sendMessage(Text.of(String.format(
				"%s by %s version %s license %s: '%s'",
				addon.getName(), addon.getAuthor(), addon.getVersion(),
				addon.getLicense(), addon.getDescription()))));

		source.sendMessage(Text.of("To view placeholders for each addon, use the command /ssxc placeholders <addon>."));
		return CommandResult.success();
	}

	@Override
	public List<String> getSuggestions(final CommandSource source, final String arguments, final Location<World> targetPosition)
			throws CommandException {
		return Arrays.asList();
	}

	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission("ssxc.addons");
	}

	@Override
	public Optional<Text> getShortDescription(final CommandSource source) {
		return Optional.of(Text.of("List installed addons"));
	}

	@Override
	public Optional<Text> getHelp(final CommandSource source) {
		return Optional.empty();
	}

	@Override
	public Text getUsage(final CommandSource source) {
		return Text.of("/ssxc addons");
	}

}

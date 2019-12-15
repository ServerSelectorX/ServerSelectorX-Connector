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

import xyz.derkades.ssx_connector.Addon;
import xyz.derkades.ssx_connector.Main;

public class PlaceholdersCommand implements CommandCallable {

	@Override
	public CommandResult process(final CommandSource source, final String arguments) throws CommandException {
		Addon addon = null;
		for (final Addon addon2 : Main.instance.addons) {
			if (addon2.getName().equalsIgnoreCase(arguments)) {
				addon = addon2;
			}
		}

		if (addon == null) {
			source.sendMessage(Text.of("No addon is installed with the name '" + arguments + "'."));
			source.sendMessage(Text.of("Get a list of installed addons using /ssxc addons"));
		}

		return CommandResult.success();
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

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

public class ReloadCommand implements CommandCallable {

	@Override
	public CommandResult process(final CommandSource source, final String arguments) throws CommandException {
		Main.instance.reloadConfig();
		Main.instance.addons.forEach(Addon::reloadConfig);
		sender.sendMessage("The plugin configuration file and addon configuration files have been reloaded. A complete server reload or restart is required for the (de)installation of addons.");
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

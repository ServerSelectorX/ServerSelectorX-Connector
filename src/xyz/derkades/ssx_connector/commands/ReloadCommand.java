package xyz.derkades.ssx_connector.commands;

import java.io.IOException;
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

public class ReloadCommand implements CommandCallable {

	@Override
	public CommandResult process(final CommandSource source, final String arguments) throws CommandException {
		try {
			Main.instance.reloadConfig();
			source.sendMessage(Text.of("Reloaded plugin configuration file"));
		} catch (final IOException e) {
			source.sendMessage(Text.of("Failed to reload plugin configuration file."));
		}

		Main.instance.addons.forEach((addon) -> {
			try {
				addon.reloadConfig();
				source.sendMessage(Text.of("Reloaded " + addon.getName()));
			} catch (final IOException e) {
				source.sendMessage(Text.of("Failed to reload configuration file for addon " + addon.getName()));
			}
		});

		source.sendMessage(Text.of("Note that a complete server reload or restart is required for the (de)installation of addons."));

		return CommandResult.success();
	}

	@Override
	public List<String> getSuggestions(final CommandSource source, final String arguments, final Location<World> targetPosition)
			throws CommandException {
		return Arrays.asList();
	}

	@Override
	public boolean testPermission(final CommandSource source) {
		return source.hasPermission("ssxc.reload");
	}

	@Override
	public Optional<Text> getShortDescription(final CommandSource source) {
		return Optional.of(Text.of("Reload plugin and addon configuration files"));
	}

	@Override
	public Optional<Text> getHelp(final CommandSource source) {
		return Optional.empty();
	}

	@Override
	public Text getUsage(final CommandSource source) {
		return Text.of("/ssxc reload");
	}

}

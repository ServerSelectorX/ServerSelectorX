package xyz.derkades.serverselectorx;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.serverselectorx.actions.Action;

public class Commands {

	static void registerCustomCommands() {
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			final CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

			for (final Map.Entry<String, FileConfiguration> configEntry : Main.getConfigurationManager().commands.entrySet()) {
				final String commandName = configEntry.getKey();
				final FileConfiguration config = configEntry.getValue();
				final String description = config.getString("description", "Opens menu");
				final String usage = config.getString("usage", "/<command>");
				final List<String> aliases = config.getStringList("aliases");
				final List<String> actions = config.getStringList("actions");

				commandMap.register("ssx-custom", new Command(commandName, description, usage, aliases) {

					@Override
					public boolean execute(final CommandSender sender, final String label, final String[] args) {
						if (sender instanceof Player){
							final Player player = (Player) sender;

							if (config.isInt("cooldown")) {
								final long timeLeft = Cooldown.getCooldown("ssxcommand" + commandName);
								if (timeLeft > 0) {
									player.sendMessage(String.format(Main.getConfigurationManager().misc.getString("cooldown-message"), timeLeft / 1000.0));
									return true;
								}

								Cooldown.addCooldown("ssxcommand" + commandName, config.getInt("cooldown"));
							}

							Action.runActions(player, actions);
						}
						return true;
					}
				});
			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

}

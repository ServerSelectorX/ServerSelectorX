package xyz.derkades.serverselectorx;

import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.reflection.ReflectionUtil;
import xyz.derkades.serverselectorx.actions.Action;

public class Commands {

	static void registerCustomCommands() {
		final CommandMap commandMap = ReflectionUtil.getCommandMap();

		for (final String commandName : Main.getConfigurationManager().listCommandConfigurations()) {
			final FileConfiguration config = Main.getConfigurationManager().getCommandConfiguration(commandName);
			final String description = config.getString("description", "Opens menu");
			final String usage = config.getString("usage", "/<command>");
			final List<String> aliases = config.getStringList("aliases");
			final List<String> actions = config.getStringList("actions");

			if (commandMap.getCommand(commandName) != null) {
				Main.getPlugin().getLogger().warning("Skipped registering command /" + commandName + ", it already exists.");
				continue;
			}

			commandMap.register("ssx-custom", new Command(commandName, description, usage, aliases) {

				@Override
				public boolean execute(final CommandSender sender, final String label, final String[] args) {
					if (sender instanceof Player){
						final Player player = (Player) sender;

						final FileConfiguration configMisc = Main.getConfigurationManager().getMiscConfiguration();

						if (config.isInt("cooldown")) {
							final long timeLeft = Cooldown.getCooldown("ssxcommand" + commandName);
							if (timeLeft > 0) {
								player.sendMessage(Colors.parseColors(String.format(configMisc.getString("cooldown-message"), timeLeft / 1000.0)));
								return true;
							}

							Cooldown.addCooldown("ssxcommand" + commandName, config.getInt("cooldown"));
						}

						if (config.getBoolean("permission", false) && !player.hasPermission("ssx.command." + commandName)) {

							if (configMisc.isString("no-permission")) {
								player.sendMessage(Colors.parseColors(configMisc.getString("no-permission")));
							}
							return true;
						}

						Action.runActions(player, actions);
					}
					return true;
				}
			});
		}

	}

}

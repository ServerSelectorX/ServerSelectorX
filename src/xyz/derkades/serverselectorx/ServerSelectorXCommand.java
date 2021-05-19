package xyz.derkades.serverselectorx;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import net.md_5.bungee.api.ChatColor;
import xyz.derkades.serverselectorx.placeholders.GlobalPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Placeholder;
import xyz.derkades.serverselectorx.placeholders.PlayerPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Server;

public class ServerSelectorXCommand implements CommandExecutor {

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args){
		if (!sender.hasPermission("ssx.admin")) {
			sender.sendMessage(ChatColor.RED + "You need the permission 'ssx.admin' to execute this command.");
			return true;
		}

		if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))){
			try {
				Main.getConfigurationManager().reload();
				sender.sendMessage("The configuration file has been reloaded.");
			} catch (final Exception e) {
				sender.sendMessage(ChatColor.RED + "An error occured while trying to reload the configuration files, probably because of a YAML syntax error.");
				sender.sendMessage("Error: " + e.getMessage());
				sender.sendMessage("For a more detailed error message see the console.");
				e.printStackTrace();
				return true;
			}

			Main.server.stop();
			Main.server.start();

			// Clear server status cache to remove any old server names
			Server.clear();

			sender.sendMessage("Run " + ChatColor.GRAY + "/ssx reloadcommands" + ChatColor.RESET + " to reload commands.");
			return true;
		}

		if (args.length == 1 && (args[0].equalsIgnoreCase("reloadcommands") || args[0].equalsIgnoreCase("rlcmd"))){
			Commands.registerCustomCommands();
			sender.sendMessage("Re-registered custom commands.");
			sender.sendMessage("Commands are not unregistered, if you disabled or renamed a command the old command will still work until the server is restarted.");
			return true;
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("status")) {
			final FileConfiguration configApi = Main.getConfigurationManager().getApiConfiguration();

			sender.sendMessage("Using port " + configApi.getInt("port"));
			sender.sendMessage("Listening on " + configApi.getString("host", "127.0.0.1 (no host specified in config)"));

			if (Server.getServers().isEmpty()){
				sender.sendMessage("No data has been received from servers.");
				return true;
			}

			for (final Server server : ServerSelectorX.getServers()) {
				final long ms = server.getTimeSinceLastMessage();
				final String lastInfo = ms < 999999 ? server.getTimeSinceLastMessage() + "ms" : "∞ ms";
				if (server.isOnline()) {
					final List<String> placeholderKeys = server.getPlaceholders().stream()
							.map(Placeholder::getKey).collect(Collectors.toList());
					sender.sendMessage(server.getName() + ": " + ChatColor.GREEN + "ONLINE (" + lastInfo + ") " + ChatColor.WHITE +
							": " + ChatColor.GRAY + String.join(", ", placeholderKeys.stream().map(s -> "{" + s + "}").collect(Collectors.toList())));
				} else {
					sender.sendMessage(server.getName() + ": " + ChatColor.RED + "OFFLINE (" + lastInfo + ")");
				}
			}
			return true;
		}

		if (args.length == 2 && args[0].equalsIgnoreCase("placeholders")) {
			final String serverName = args[1];

			final Server server = Server.getServer(serverName);

			if (!server.isOnline()) {
				sender.sendMessage("The server '" + serverName + "' does not exist or is currently offline");
				return true;
			}

			for (final Placeholder placeholder : server.getPlaceholders()) {
				if (placeholder instanceof PlayerPlaceholder) {
					sender.sendMessage(placeholder.getKey());
					final PlayerPlaceholder pp = (PlayerPlaceholder) placeholder;
					pp.getValues().forEach((uuid, value) -> {
						sender.sendMessage("  " + uuid + ": " + value);
					});
				} else {
					sender.sendMessage(placeholder.getKey() + ": " + ((GlobalPlaceholder) placeholder).getValue());
				}
			}
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("items")) {
			Main.getConfigurationManager().listItemConfigurations().forEach(name -> {
				sender.sendMessage(name);
			});
			return true;
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("menus")) {
			Main.getConfigurationManager().listMenuConfigurations().forEach(name -> {
				sender.sendMessage(name);
			});
			return true;
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("commands")) {
			Main.getConfigurationManager().listCommandConfigurations().forEach(name -> {
				sender.sendMessage(name);
			});
			return true;
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("lagdebug")) {
			Main.LAG_DEBUG = true;
			sender.sendMessage("Lag related debug console messages are now enabled until the next server restart/reload.");
			return true;
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("itemdebug")) {
			Main.ITEM_DEBUG = true;
			sender.sendMessage("Debug messages related to items on join are now enabled until the next server restart/reload.");
			return true;
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("sync")) {
			sender.sendMessage("Synchronising configuration files.. For more information have a look at the console.");
			Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> Main.getConfigSync().sync());
			return true;
		}

		if (args.length == 1 && args[0].equalsIgnoreCase("synclist")) {
			Bukkit.getScheduler().runTaskAsynchronously(Main.getPlugin(), () -> {
				Main.getConfigSync().getFilesToSync().forEach(f -> sender.sendMessage(f));
			});
			return true;
		}

		return false;
	}

}

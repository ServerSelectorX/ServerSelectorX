package xyz.derkades.serverselectorx;

import static org.bukkit.ChatColor.AQUA;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;

public class ServerSelectorXCommand implements CommandExecutor {

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args){
		if (!sender.hasPermission("ssx.admin")) {
			sender.sendMessage(ChatColor.RED + "You need the permission 'ssx.reload' to execute this command.");
			return true;
		}

		if (args.length == 1 && (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl"))){
			Main.getConfigurationManager().reload();
			sender.sendMessage(AQUA + "The configuration file has been reloaded.");

			Main.server.stop();
			Main.server.start();
			return true;
		}

		return false;
	}

}

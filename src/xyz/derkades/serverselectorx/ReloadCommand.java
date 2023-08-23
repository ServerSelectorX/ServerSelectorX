package xyz.derkades.serverselectorx;

import net.md_5.bungee.api.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {

	@Override
	public boolean onCommand(final CommandSender sender, final Command command, final String label, final String[] args){
		if (args.length != 1){
			return false;
		}

		if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")){
			if (!sender.hasPermission("ssx.reload")) {
				sender.sendMessage(ChatColor.RED + "You need the permission 'ssx.reload' to execute this command.");
				return true;
			}

			Main.getConfigurationManager().reload();
			Main.getPingManager().loadServers();
			sender.sendMessage("The configuration file has been reloaded.");
			return true;
		}

		return false;
	}

}

package xyz.derkades.serverselectorx;

import static org.bukkit.ChatColor.AQUA;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import net.md_5.bungee.api.ChatColor;

public class ReloadCommand implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if (args.length != 1){
			return false;
		}
		
		if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")){
			if (!sender.hasPermission("ssx.reload")) {
				sender.sendMessage(ChatColor.RED + "You need the permission 'ssx.reload' to execute this command.");
				return true;
			}
			
			Main.getConfigurationManager().reloadAll();
			Main.getPlugin().restartServer();
			sender.sendMessage(AQUA + "The configuration file has been reloaded.");
			return true;
		}
		
		return false;
	}

}

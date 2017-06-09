package xyz.derkades.serverselectorx;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

public class ReloadCommand implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if (args.length != 1){
			return false;
		}
		
		if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")){
			Main.getPlugin().saveDefaultConfig();
			Main.getPlugin().reloadConfig();
			sender.sendMessage(Message.CONFIG_RELOADED.toString());
			return true;
		}
		
		return false;
	}

}

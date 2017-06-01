package xyz.derkades.serverselectorx;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;

import xyz.derkades.serverselectorx.utils.Config;

public class ReloadCommand implements CommandExecutor {
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
		if (args.length != 1){
			return false;
		}
		
		if (args[0].equalsIgnoreCase("reload") || args[0].equalsIgnoreCase("rl")){
			Config.reloadConfig();
			sender.sendMessage(Message.CONFIG_RELOADED.toString());
			return true;
		}
		
		return false;
	}

}

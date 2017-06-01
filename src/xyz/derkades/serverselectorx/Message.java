package xyz.derkades.serverselectorx;

import static org.bukkit.ChatColor.AQUA;
import static org.bukkit.ChatColor.DARK_AQUA;
import static org.bukkit.ChatColor.DARK_GRAY;
import static org.bukkit.ChatColor.RED;

public enum Message {
	
	INVALID_ITEM_NAME(RED + "The item name you have provided in the config file is invalid."),
	CONFIG_RELOADED(AQUA + "The configuration file has been reloaded."),
	
	;
	
	private static final String PREFIX = DARK_GRAY + "[" + DARK_AQUA + "ServerSelectorX" + DARK_GRAY + "]";
	
	private String message;
	
	Message(String message){
		this.message = message;
	}
	
	@Override
	public String toString(){
		return PREFIX + " " + message;
	}

}

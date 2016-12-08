package com.robinmc.serverselectorx;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.robinmc.serverselectorx.utils.Config;
import com.robinmc.serverselectorx.utils.ItemBuilder;

public class OnJoinListener implements Listener {
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event){
		boolean putItemInInventory = Config.getConfig().getBoolean("on-join");
		if (!putItemInInventory)
			return;
		
		Player player = event.getPlayer();
		Material material = Material.getMaterial(Config.getConfig().getString("item"));
		
		if (material == null){
			player.sendMessage(Message.INVALID_ITEM_NAME.toString());
			return;
		}
		
		ItemStack item = new ItemBuilder(material)
				.setName(Main.parseColorCodes(Config.getConfig().getString("title")))
				.create();
		
		int slot = Config.getConfig().getInt("inv-slot");
		PlayerInventory inv = player.getInventory();
		
		inv.setItem(slot, item);
	}

}

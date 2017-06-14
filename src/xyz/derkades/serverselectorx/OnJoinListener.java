package xyz.derkades.serverselectorx;

import java.io.File;

import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.serverselectorx.utils.ItemBuilder;

public class OnJoinListener implements Listener {
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(PlayerJoinEvent event){
		for (File serverSelectorFile : new File(Main.getPlugin().getDataFolder() + "/menu").listFiles()){
			YamlConfiguration config = YamlConfiguration.loadConfiguration(serverSelectorFile);
			
			boolean putItemInInventory = config.getBoolean("on-join");
			if (!putItemInInventory)
				continue;
			
			Player player = event.getPlayer();
			Material material = Material.getMaterial(config.getString("item"));
			
			if (material == null){
				player.sendMessage(Message.INVALID_ITEM_NAME.toString());
				return;
			}
			
			ItemStack item = new ItemBuilder(material)
					.setName(Colors.parseColors(config.getString("title")))
					.create();
			
			int slot = config.getInt("inv-slot");
			PlayerInventory inv = player.getInventory();
			
			inv.setItem(slot, item);
		}
	}

}

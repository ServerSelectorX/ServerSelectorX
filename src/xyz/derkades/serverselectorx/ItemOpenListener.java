package xyz.derkades.serverselectorx;

import java.util.List;

import org.bukkit.Material;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

import de.tr7zw.nbtapi.NBTItem;

public class ItemOpenListener implements Listener {
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(final PlayerInteractEvent event){
		if (event.getAction() == Action.PHYSICAL)
			return;
		
		if (event.getHand() == EquipmentSlot.OFF_HAND)
			return;

		final FileConfiguration globalConfig = Main.getConfigurationManager().getGlobalConfig();

		if (event.isCancelled() && globalConfig.getBoolean("ignore-cancelled", false))
			return;
		
		final Player player = event.getPlayer();
		final ItemStack item = player.getInventory().getItemInMainHand();
		
		if (item == null || item.getType() == Material.AIR)
			return;
		
		final NBTItem nbt = new NBTItem(item);
		
		if (!nbt.hasKey("SSXMenu"))
			return;
		
		final String menuName = nbt.getString("SSXMenu");
		
		if (!Main.getConfigurationManager().getMenus().containsKey(menuName)) {
			player.sendMessage("A menu with the name '" + menuName + "' does not exist.");
			return;
		}
		
		final FileConfiguration menu = Main.getConfigurationManager().getMenus().get(menuName);
		
		final List<String> actions;
		
		if (menu.isList("left-click-actions")) {
			if (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK) {
				actions = menu.getStringList("left-click-actions");
			} else {
				actions = menu.getStringList("actions");
			}
		} else {
			actions = menu.getStringList("actions");
		}
		
		xyz.derkades.serverselectorx.actions.Action.runActions(player, actions);
		
		final boolean cancel = globalConfig.getBoolean("cancel-click-event", false);
		if (cancel) {
			event.setCancelled(true);
		}
	}
}

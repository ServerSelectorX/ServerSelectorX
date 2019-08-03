package xyz.derkades.serverselectorx;

import org.bukkit.event.Listener;

@Deprecated
public class SelectorOpenListener implements Listener {

	/*@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.HIGH)
	public void onInteract(final PlayerInteractEvent event){
		if (event.getHand() == EquipmentSlot.OFF_HAND) {
			return;
		}

		final FileConfiguration globalConfig = Main.getConfigurationManager().getGlobalConfig();

		if (event.isCancelled() && globalConfig.getBoolean("ignore-cancelled", false)) {
			return;
		}

		final boolean openOnRightClick = globalConfig.getBoolean("right-click-open", true);
		final boolean openOnLeftClick = globalConfig.getBoolean("left-click-open", false);

		if (!(
				openOnRightClick && (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) ||
				openOnLeftClick && (event.getAction() == Action.LEFT_CLICK_AIR || event.getAction() == Action.LEFT_CLICK_BLOCK)
				)) return;

		final Player player = event.getPlayer();

		for (final Map.Entry<String, FileConfiguration> menuConfigEntry : Main.getConfigurationManager().getMenus().entrySet()) {
			final String configName = menuConfigEntry.getKey();
			final FileConfiguration menuConfig = menuConfigEntry.getValue();

			if (!menuConfig.getBoolean("item.enabled")) {
				continue;
			}

			final ItemStack item = Main.getHotbarItemStackFromMenuConfig(player, menuConfig, configName);

			if (item == null) {
				continue;
			}
			
			if (!item.isSimilar(player.getItemInHand())) {
				continue;
			}

			final boolean cancel = globalConfig.getBoolean("cancel-click-event", false);
			if (cancel) event.setCancelled(true);

			Main.openSelector(player, configName);
			return;
		}
	}*/

}

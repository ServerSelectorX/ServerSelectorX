package xyz.derkades.serverselectorx;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.Configuration;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import xyz.derkades.serverselectorx.conditional.ConditionalItem;

import java.lang.reflect.InvocationTargetException;
import java.util.HashSet;
import java.util.Set;

public class HotbarItemManager implements Listener {

	private final Main plugin;

	HotbarItemManager(Main plugin) {
		this.plugin = plugin;
	}

	void enable() {
		Bukkit.getPluginManager().registerEvents(this, plugin);
	}

	void reload() {
		for (Player player : Bukkit.getOnlinePlayers()) {
			this.updateSsxItems(player);
		}
	}

	private void debug(String message) {
		if (Main.ITEM_DEBUG) {
			Main.getPlugin().getLogger().info("[Item debug] " + message);
		}
	}

	private boolean shouldHaveItem(final Player player, String configName) {
		Configuration config = Main.getConfigurationManager().getItemConfiguration(configName);

		if (config == null) {
			debug("Item was for a config file that is now removed");
			return false;
		}

		if (config.getBoolean("give.permission")) {
			debug("Permissions are enabled, checking permission");
			final String permission = "ssx.item." + configName;
			if (!player.hasPermission(permission)) {
				debug("Player does not have permission");
				return false;
			}
		}

		if (config.isList("worlds")) {
			debug("World whitelisting is enabled");
			// World whitelisting option is present
			if (!config.getStringList("worlds").contains(player.getWorld().getName())) {
				debug("Player is in a world that is not whitelisted (" + player.getWorld().getName() + ")");
				return false;
			}
		}

		return true;
	}

	private void giveItem(Player player, String configName, int currentSlot) {
		Configuration config = Main.getConfigurationManager().getItemConfiguration(configName);

		if (!config.isConfigurationSection("item")) {
			Main.getPlugin().getLogger().warning("Item '" + configName + "' has no item section, it has been ignored.");
			return;
		}

		String cooldownId = player.getUniqueId() + configName;
		PlayerInventory inv = player.getInventory();
		try {
			ConditionalItem.getItem(player, config.getConfigurationSection("item"), cooldownId, item -> {
				NBTItem nbt = new NBTItem(item);
				nbt.setString("SSXItemConfigName", configName);
				item = nbt.getItem();

				final int configuredSlot = config.getInt("give.inv-slot", 0);

				if (configuredSlot < 0) { // First available slot
					// Update item in current slot if specified, otherwise add new item
					if (currentSlot < 0) {
						inv.addItem(item);
					} else {
						inv.setItem(currentSlot, item);
					}
				} else {
					if (currentSlot < 0 || currentSlot == configuredSlot) {
						// No existing item or item in correct slot, overwrite item in configured slot
						inv.setItem(configuredSlot, item);
					} else {
						// Item is not in the slot that it is supposed to be in.
						inv.setItem(currentSlot, null);
						inv.setItem(configuredSlot, item);
					}
				}
			});
		} catch (InvalidConfigurationException e) {
			player.sendMessage(String.format("Invalid item config (in %s.yaml): %s",
					configName, e.getMessage()));
		}
	}

	private void updateSsxItems(final Player player) {
		debug("Updating items for: " + player.getName());
		ItemStack[] contents = player.getInventory().getContents();

		// First remove any items

		Set<String> itemConfigNames = Main.getConfigurationManager().listItemConfigurations();

		Set<String> presentItems = new HashSet<>();

		for (int slot = 0; slot < contents.length; slot++) {
			ItemStack item = contents[slot];
			if (item == null) {
				continue;
			}

			NBTItem nbt = new NBTItem(item);

			if (!nbt.hasKey("SSXItemConfigName")) {
				// Not our item
				if (nbt.hasKey("SSXActions")) {
					// actually, it is our item from an old SSX version
					debug("Removing item from old SSX version from slot " + slot);
					player.getInventory().setItem(slot, null);
				}
				continue;
			}

			String configName = nbt.getString("SSXItemConfigName");

			if (shouldHaveItem(player, configName)) {
				debug("Player is allowed to keep item in slot " + slot);
			} else {
				debug("Player is not allowed to keep item in slot " + slot + ", removing it");
				player.getInventory().setItem(slot, null);
				continue;
			}

			debug("Update item " + slot + " in case the configuration has changed");

			giveItem(player, configName, slot);
			presentItems.add(configName);
		}

		for (String configName : itemConfigNames) {
			if (presentItems.contains(configName)) {
				continue;
			}

			debug("Player does not have item " + configName);

			if (!shouldHaveItem(player, configName)) {
				debug("Player should not have the item, not giving it.");
				continue;
			}

			debug("Player should have this item, giving it now");

			giveItem(player, configName, -1);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onJoin(final PlayerJoinEvent event) {
		final Player player = event.getPlayer();
		final FileConfiguration config = Main.getConfigurationManager().getInventoryConfiguration();

		if (config.getBoolean("clear-inv", false) && !player.hasPermission("ssx.clearinvbypass")) {
			debug("Clearing inventory for " + player.getName());
			final PlayerInventory inv = player.getInventory();
			inv.setContents(new ItemStack[inv.getContents().length]);
			inv.setArmorContents(new ItemStack[inv.getArmorContents().length]);
			try {
				final int length = ((ItemStack[]) inv.getClass().getMethod("getStorageContents").invoke(inv)).length;
				inv.getClass().getMethod("setStorageContents", ItemStack[].class).invoke(inv, (Object) new ItemStack[length]);
			} catch (final NoSuchMethodException ignored) { // This method does not exist on <1.9
			} catch (final InvocationTargetException | IllegalAccessException e) {
				e.printStackTrace();
			}
		}

		this.updateSsxItems(player);
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onWorldChange(final PlayerChangedWorldEvent event) {
		this.updateSsxItems(event.getPlayer());
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onRespawn(final PlayerRespawnEvent event) {
		this.updateSsxItems(event.getPlayer());
	}

	@EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
	public void onClear(final PlayerCommandPreprocessEvent event) {
		if (event.getMessage().equals("/clear") && event.getPlayer().hasPermission("minecraft.command.clear")) {
			Bukkit.getScheduler().runTaskLater(Main.getPlugin(), () -> this.updateSsxItems(event.getPlayer()), 1);
		}
	}

}

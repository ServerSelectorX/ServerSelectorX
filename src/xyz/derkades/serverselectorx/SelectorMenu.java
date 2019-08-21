package xyz.derkades.serverselectorx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import de.tr7zw.nbtapi.NBTItem;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.IllegalItems;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.bukkit.menu.IconMenu;
import xyz.derkades.derkutils.bukkit.menu.MenuCloseEvent;
import xyz.derkades.derkutils.bukkit.menu.OptionClickEvent;
import xyz.derkades.serverselectorx.actions.Action;

public class SelectorMenu extends IconMenu {

	private final FileConfiguration config;
	private final int slots;

	private final IllegalItems illegalItems;

	private BukkitTask refreshTimer;

	public SelectorMenu(final Player player, final FileConfiguration config, final String configName) {
		super(Main.getPlugin(), Colors.parseColors(config.getString("title", UUID.randomUUID().toString())), 9, player);
		this.config = config;

		this.slots = config.getInt("rows", 6) * 9;
		this.setSize(this.slots);

		this.illegalItems = new IllegalItems(Main.getPlugin());
	}

	@Override
	public void open() {
		this.addItems();

		super.open();

		this.refreshTimer = Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), () -> {
			this.addItems();
			super.refreshItems();
		}, 1*20, 1*20);
	}

	private void addItems() {
		itemLoop:
		for (final String key : this.config.getConfigurationSection("menu").getKeys(false)) {
			final ConfigurationSection section = this.config.getConfigurationSection("menu." + key);

			List<String> actions;
			ItemBuilder builder;

			if (section.contains("permission") && !this.player.hasPermission(section.getString("permission"))) {
				// Use no-permission section
				builder = Main.getItemBuilderFromItemSection(this.player, section.getConfigurationSection("no-permission"));
				actions = section.getConfigurationSection("no-permission").getStringList("actions");
			} else {
				// Player has permission, use other sections
				if (section.contains("connector")) {
					// Advanced server section. Use online, offline, dynamic sections.
					final String serverName = section.getString("connector");

					if (Main.isOnline(serverName)) {
						final Map<UUID, Map<String, String>> playerPlaceholders = Main.PLACEHOLDERS.get(serverName);

						builder = null;
						actions = null;

						if (section.contains("dynamic")) {
							for (final String dynamicKey : section.getConfigurationSection("dynamic").getKeys(false)) {
								final String placeholder = dynamicKey.split(":")[0];
								final String placeholderValueInConfig = dynamicKey.split(":")[1];

								final String placeholderValueFromConnector;

								if (playerPlaceholders.get(null).containsKey(placeholder))
								{ // Global placeholder
									placeholderValueFromConnector = playerPlaceholders.get(null).get(placeholder);
								} else if (playerPlaceholders.containsKey(this.player.getUniqueId()) && playerPlaceholders.get(this.player.getUniqueId()).containsKey(placeholder))
								{ // Player specific placeholder
									placeholderValueFromConnector = playerPlaceholders.get(this.player.getUniqueId()).get(placeholder);
								} else {
									Main.getPlugin().getLogger().warning("Dynamic feature contains rule with placeholder " + placeholder + " which has not been received from the server.");
									continue;
								}

								final ConfigurationSection dynamicSection = section.getConfigurationSection("dynamic." + dynamicKey);

								final String mode = dynamicSection.getString("mode", "equals");

								if (
										(mode.equals("equals") && placeholderValueInConfig.equals(placeholderValueInConfig)) ||
										(mode.equals("less") && Double.parseDouble(placeholderValueInConfig) < Double.parseDouble(placeholderValueFromConnector)) ||
										(mode.equals("less") && Double.parseDouble(placeholderValueInConfig) > Double.parseDouble(placeholderValueFromConnector))
										) {

									if (dynamicSection.getString("material").equalsIgnoreCase("NONE")) {
										continue itemLoop;
									}

									builder = Main.getItemBuilderFromItemSection(this.player, dynamicSection);
									actions = dynamicSection.getStringList("actions");
									break;
								}
							}
						}

						if (builder == null) {
							//No dynamic rule matched, fall back to online
							if (!section.isConfigurationSection("online")) {
								this.player.sendMessage("Error for item " + key);
								this.player.sendMessage("Online section does not exist");
								return;
							}

							final ConfigurationSection onlineSection = section.getConfigurationSection("online");

							if (!onlineSection.contains("material")) {
								this.player.sendMessage("Error for item " + key);
								this.player.sendMessage("Online section does not have a material option");
							}

							if (onlineSection.getString("material").equalsIgnoreCase("NONE")) {
								continue itemLoop;
							}

							builder = Main.getItemBuilderFromItemSection(this.player, onlineSection);
							actions = onlineSection.getStringList("actions");
						}

						final Map<String, String> placeholders = new HashMap<>();

						// Add global placeholders to list (uuid = null)
						playerPlaceholders.get(null).forEach((k, v) -> placeholders.put("{" + k + "}", v));

						// If there are any player specific placeholders for this player, parse them too
						if (playerPlaceholders.containsKey(this.player.getUniqueId())) {
							playerPlaceholders.get(this.player.getUniqueId()).forEach((k, v) -> placeholders.put("{" + k + "}", v));
						}

						// Now parse the collected placeholders and papi placeholders in name and lore
						builder.namePlaceholders(placeholders).lorePlaceholders(placeholders);

						// Set item amount if dynamic item count is enabled
						if (section.getBoolean("dynamic-item-count", false)) {
							final int amount = Integer.parseInt(playerPlaceholders.get(null).get("online"));
							builder.amount(amount < 1 || amount > 64 ? 1 : amount);
						}
					} else {
						//Server is offline
						if (!section.isConfigurationSection("offline")) {
							this.player.sendMessage("Offline section does not exist");
							return;
						}

						final ConfigurationSection offlineSection = section.getConfigurationSection("offline");

						if (!offlineSection.contains("material")) {
							this.player.sendMessage("Error for item " + key);
							this.player.sendMessage("Offline section does not have a material option");
						}

						if (offlineSection.getString("material").equalsIgnoreCase("NONE")) {
							continue;
						}

						builder = Main.getItemBuilderFromItemSection(this.player, offlineSection);
						actions = offlineSection.getStringList("actions");
					}
//				} else if (firstAction.startsWith("openmenu:")) {
//					if (section.getString("material").equalsIgnoreCase("NONE")) {
//						continue itemLoop;
//					}
//
//					final String menuName = firstAction.substring(9);
//
//					if (!Main.getConfigurationManager().getMenus().containsKey(menuName)) {
//						this.player.sendMessage("Menu '" + menuName + "' does not exist");
//						return;
//					}
//
//					final Supplier<String> placeholderSupplier = () -> {
//						//Add all online counts of servers in the submenu
//						int totalOnline = 0;
//
//						final FileConfiguration subConfig = Main.getConfigurationManager().getMenus().get(menuName);
//						for (final String subKey : subConfig.getConfigurationSection("menu").getKeys(false)){
//							final ConfigurationSection subSection = subConfig.getConfigurationSection("menu." + subKey);
//							final String subAction = subSection.getString("action", "none");
//							if (!subAction.startsWith("srv:")) {
//								continue;
//							}
//
//							final String serverName = subAction.substring(4);
//
//							if (Main.isOnline(serverName)) {
//								totalOnline += Integer.parseInt(Main.PLACEHOLDERS.get(serverName).get(null).get("online"));
//							}
//						}
//						return totalOnline + "";
//					};
//
//					builder = Main.getItemBuilderFromItemSection(this.player, section)
//							.namePlaceholderOptional("{total}", placeholderSupplier).lorePlaceholderOptional("{total}", placeholderSupplier);
				} else {
					// Simple section
					if (section.getString("material") == null) {
						this.player.sendMessage("Error for item " + key);
						this.player.sendMessage("Missing material option. Remember, this is not an advanced section, so don't use online/offline/dynamic sections in the config.");
						this.player.sendMessage("If you want to use these sections, add a connector option.");
					}

					if (section.getString("material").equalsIgnoreCase("NONE")) {
						continue itemLoop;
					}

					builder = Main.getItemBuilderFromItemSection(this.player, section);
					actions = section.getStringList("actions");
				}
			}

			builder.papi(this.player)
				.placeholder("{player}", this.player.getName())
				.placeholder("{globalOnline}", Main.getGlobalPlayerCount() + "");

			// Add actions to item as NBT
			final NBTItem nbt = new NBTItem(builder.create());
			nbt.setObject("SSXActions", actions);

			final ItemStack item = nbt.getItem();

			this.illegalItems.setIllegal(item, true);

			final int slot = Integer.valueOf(key);

			if (slot > this.slots - 1) {
				this.player.sendMessage("You put an item in slot " + slot + ", which is higher than the maximum number of slots in your menu.");
				this.player.sendMessage("Increase the number of rows in the config");
				return;
			}

			if (slot < 0) {
				// Slot is set to -1 (or other negative value), fill all blank slots
				for (int i = 0; i < this.slots; i++) {
					if (!this.items.containsKey(i)) {
						this.items.put(i, item);
					}
				}
			} else {
				// Slot is set to a positive value, put item in slot.
				this.items.put(slot, item);
			}
		}
	}

	@Override
	public boolean onOptionClick(final OptionClickEvent event) {
		final Player player = event.getPlayer();

		final NBTItem nbt = new NBTItem(event.getItemStack());

		@SuppressWarnings("unchecked")
		final List<String> actions = nbt.getObject("SSXActions", List.class);

		System.out.println(actions.toArray());

		return Action.runActions(player, actions);
	}

	@Override
	public void onClose(final MenuCloseEvent event) {
		if (this.refreshTimer != null) {
			this.refreshTimer.cancel();
		}

		this.illegalItems.unregister();
	}

}

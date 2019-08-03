package xyz.derkades.serverselectorx;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitTask;

import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.IllegalItems;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.bukkit.menu.IconMenu;
import xyz.derkades.derkutils.bukkit.menu.MenuCloseEvent;
import xyz.derkades.derkutils.bukkit.menu.OptionClickEvent;
import xyz.derkades.serverselectorx.actions.Action;

public class SelectorMenu extends IconMenu {

	private final FileConfiguration config;
	private final Player player;
	private final int slots;
	
	private final IllegalItems illegalItems;

	private BukkitTask refreshTimer;

	public SelectorMenu(final Player player, final FileConfiguration config, final String configName) {
		super(Main.getPlugin(), Colors.parseColors(config.getString("title", UUID.randomUUID().toString())), 9, player);
		this.config = config;
		this.player = player;

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

			ItemBuilder builder;
			
			if (section.contains("permission") && !this.player.hasPermission(section.getString("permission"))) {
				// Use no-permission section
				builder = Main.getItemBuilderFromItemSection(this.player, section.getConfigurationSection("no-permission"));
			} else {
				// Player has permission, use other sections
				String firstAction;

				if (section.contains("action")) {
					if (section.isList("action")) {
						firstAction = section.getStringList("action").get(0);
					} else {
						firstAction = section.getString("action");
					}
				} else {
					firstAction = "none";
				}

				if (firstAction.startsWith("server")) {
					String serverName = firstAction.substring(4);

					if (serverName.startsWith("__")) {
						serverName = serverName.substring(2);
					}

					if (Main.isOnline(serverName)) {
						final Map<UUID, Map<String, String>> playerPlaceholders = Main.PLACEHOLDERS.get(serverName);

						builder = null;
						
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
									break;
								}
							}
						}

						if (builder == null) {
							//No dynamic rule matched, fall back to online
							if (!section.isConfigurationSection("online")) {
								this.player.sendMessage("Online section does not exist");
								return;
							}
							
							final ConfigurationSection onlineSection = section.getConfigurationSection("online");
							if (section.getString("material").equalsIgnoreCase("NONE")) {
								continue itemLoop;
							}
							
							builder = Main.getItemBuilderFromItemSection(this.player, onlineSection);
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
						if (section.getString("material").equalsIgnoreCase("NONE")) {
							continue;
						}
						
						builder = Main.getItemBuilderFromItemSection(this.player, offlineSection);
					}
				} else if (firstAction.startsWith("openmenu:")) {
					if (section.getString("material").equalsIgnoreCase("NONE")) {
						continue itemLoop;
					}
					
					final String menuName = firstAction.substring(9);

					if (!Main.getConfigurationManager().getMenus().containsKey(menuName)) {
						this.player.sendMessage("Menu '" + menuName + "' does not exist");
						return;
					}
					
					final Supplier<String> placeholderSupplier = () -> {
						//Add all online counts of servers in the submenu
						int totalOnline = 0;
						
						final FileConfiguration subConfig = Main.getConfigurationManager().getMenus().get(menuName);
						for (final String subKey : subConfig.getConfigurationSection("menu").getKeys(false)){
							final ConfigurationSection subSection = subConfig.getConfigurationSection("menu." + subKey);
							final String subAction = subSection.getString("action", "none");
							if (!subAction.startsWith("srv:")) {
								continue;
							}

							final String serverName = subAction.substring(4);

							if (Main.isOnline(serverName)) {
								totalOnline += Integer.parseInt(Main.PLACEHOLDERS.get(serverName).get(null).get("online"));
							}
						}
						return totalOnline + "";
					};
					
					builder = Main.getItemBuilderFromItemSection(this.player, section)
							.namePlaceholderOptional("{total}", placeholderSupplier).lorePlaceholderOptional("{total}", placeholderSupplier);
				} else {
					//Not a server
					if (section.getString("material").equalsIgnoreCase("NONE")) {
						continue itemLoop;
					}
					
					builder = Main.getItemBuilderFromItemSection(this.player, section);
				}
			}

			builder.papi(this.player)
				.placeholder("{player}", this.player.getName())
				.placeholder("{globalOnline}", Main.getGlobalPlayerCount() + "");

			final ItemStack item = builder.create();
			
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
		final int slot = event.getPosition();
		final Player player = event.getPlayer();

		List<String> actions;

		if (this.config.isList("menu." + slot + ".action")) {
			// Action exists and is a list
			actions = this.config.getStringList("menu." + slot + ".action");

			if (actions.isEmpty()) {
				actions.add("msg:Action list found, but list is empty");
			}
		} else {
			// Action is not a list or might not exist
			String possibleAction = this.config.getString("menu." + slot + ".action", "none");

			if (possibleAction == null) {
				//If the action is null ('slot' is not found in the config) it is probably a wildcard
				possibleAction = this.config.getString("menu.-1.action");

				if (possibleAction == null) { //If it is still null it must be missing
					possibleAction = "msg:No action found.";
				}
			}

			actions = Arrays.asList(possibleAction);
		}
		
		return Action.runActions(player, actions);
			
			/*} else if (action.equalsIgnoreCase("toggleInvis")) {
				
			} else if (action.equalsIgnoreCase("toggleSpeed")) {
				if (player.hasPotionEffect(PotionEffectType.SPEED)) {
					player.removePotionEffect(PotionEffectType.SPEED);
					player.sendMessage(Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("speed-off")));
				} else {
					final int amplifier = Main.getConfigurationManager().getGlobalConfig().getInt("speed-amplifier", 3);

					if (Main.getConfigurationManager().getGlobalConfig().getBoolean("show-particles")) {
						player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true));
					} else {
						player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true, false));
					}

					player.sendMessage(Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("speed-on")));
				}
			} else if (action.equalsIgnoreCase("toggleHideOthers")) {
				
			} else if (action.startsWith("msg:")){ //Send message
				final String message = action.substring(4);
				player.sendMessage(Main.PLACEHOLDER_API.parsePlaceholders(player, message));*/
	}

	@Override
	public void onClose(final MenuCloseEvent event) {
		if (this.refreshTimer != null) {
			this.refreshTimer.cancel();
		}
		
		this.illegalItems.unregister();
	}

}

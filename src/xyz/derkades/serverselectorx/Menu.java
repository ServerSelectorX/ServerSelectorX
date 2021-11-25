package xyz.derkades.serverselectorx;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.jetbrains.annotations.Nullable;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.NbtItemBuilder;
import xyz.derkades.derkutils.bukkit.menu.IconMenu;
import xyz.derkades.derkutils.bukkit.menu.MenuCloseEvent;
import xyz.derkades.derkutils.bukkit.menu.OptionClickEvent;
import xyz.derkades.serverselectorx.actions.Action;
import xyz.derkades.serverselectorx.placeholders.GlobalPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Placeholder;
import xyz.derkades.serverselectorx.placeholders.PlayerPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Server;

public class Menu extends IconMenu {

	private final FileConfiguration config;

	private boolean closed = false;

	public Menu(final Player player, final @Nullable FileConfiguration config, final String configName) {
		super(Main.getPlugin(),
				config == null ? "error" : Colors.parseColors(config.getString("title", "TITLE MISSING")),
				config == null ? 1 : config.getInt("rows", 6),
				player);

		this.config = config;

		if (this.config == null ||
				this.config.getConfigurationSection("menu") == null) {
			player.sendMessage("The configuration file failed to load, probably due to a syntax error.");
			player.sendMessage("Take a look at the console for any YAML errors, or paste your config in http://www.yamllint.com/");
			player.sendMessage("Check for identation and balanced quotes. If you want to use quotation marks in strings, they must be escaped properly by putting two quotation marks (for example \"\" or '').");
			player.sendMessage("Menu name: " + configName);
			return;
		}

		try {
			if (config.contains("sound")) {
				player.playSound(player.getLocation(), Sound.valueOf(config.getString("sound")), 1.0f, 1.0f);
			}
		} catch (final IllegalArgumentException e) {
			Main.getPlugin().getLogger().warning("Invalid sound name in config file '" + configName + "'");
			Main.getPlugin().getLogger().warning("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Sound-names");
		}

		final int updateInterval = config.getInt("update-interval", 100);

		new BukkitRunnable() {
			@Override
			public void run() {
				if (Menu.this.closed || // menu has been closed
						Bukkit.getPlayer(player.getName()) == null // player is offline
						) {
					this.cancel();
					return;
				}

				final long start = System.nanoTime();
				if (Main.LAG_DEBUG) {
					Main.getPlugin().getLogger().info("Starting menu load for player " + player.getName());
				}

				addItems();

				if (Main.LAG_DEBUG) {
					final long diff = System.nanoTime() - start;
					Main.getPlugin().getLogger().info(String.format("--Finished (re)loading menu in %.2fμs. (one tick is 50ms)", diff / 1000f));
				}

				if (config.getBoolean("disable-updates", false)) {
					this.cancel();
				}
			}
		}.runTaskTimer(Main.getPlugin(), 0, updateInterval);
	}

	private void addItems() {
		final FileConfiguration configMisc = Main.getConfigurationManager().getMiscConfiguration();

		final OfflinePlayer potentiallyOffline = this.getPlayer();
		if (!(potentiallyOffline instanceof Player)) {
			Main.getPlugin().getLogger().warning("Player " + potentiallyOffline.getUniqueId() + " went offline?");
			return;
		}

		final Player player = (Player) potentiallyOffline;

		itemLoop:
		for (final String key : this.config.getConfigurationSection("menu").getKeys(false)) {
			long itemStart = System.nanoTime();
			
			if (!this.config.isConfigurationSection("menu." + key)) {
				player.sendMessage("Invalid item " + key + ", check indentation.");
				continue;
			}

			final ConfigurationSection section = this.config.getConfigurationSection("menu." + key);

			ConfigurationSection chosenSection;
			NbtItemBuilder builder;

			if (section.contains("permission") && !player.hasPermission(section.getString("permission"))) {
				// Use no-permission section
				final ConfigurationSection noPermissionSection = section.getConfigurationSection("no-permission");

				if (noPermissionSection == null) {
					player.sendMessage("Missing no-permission section for item " + key + ". Remove the permission option or add a no-permission section.");
					return;
				}

				final String materialString = noPermissionSection.getString("material");

				if (materialString == null) {
					player.sendMessage("No permission section is missing the material option");
					return;
				}

				if (materialString.equals("NONE") || materialString.equals("AIR")) {
					continue;
				}

				builder = Main.getItemBuilderFromItemSection(player, noPermissionSection);
				chosenSection = noPermissionSection;
			} else {
				// Player has permission, use other sections
				if (section.contains("connector")) {
					// Advanced server section. Use online, offline, dynamic sections.
					final String serverName = section.getString("connector");
					final Server server = Server.getServer(serverName);

					if (server.isOnline()) {
						// to avoid "may not have been initialized" errors later
						builder = null;
						chosenSection = null;

						if (configMisc.contains("server-name") &&
								serverName.equalsIgnoreCase(configMisc.getString("server-name")) &&
								section.isConfigurationSection("connected")) {
							final ConfigurationSection connectedSection = section.getConfigurationSection("connected");
							builder = Main.getItemBuilderFromItemSection(player, connectedSection);
							chosenSection = connectedSection;
						} else {
							if (section.contains("dynamic")) {
								for (final String dynamicKey : section.getConfigurationSection("dynamic").getKeys(false)) {
									final long colons = dynamicKey.chars().filter(c -> c == ':').count();
									if (colons != 1) {
										player.sendMessage("Invalid dynamic section '" + dynamicKey + "'. Dynamic section identifiers should contain exactly one colon, this one contains " + colons + ".");
										return;
									}

									final String[] split = dynamicKey.split(":");
									final String placeholderKeyInConfig = split[0];
									final String placeholderValueInConfig = split[1];

									final Placeholder placeholder = server.getPlaceholder(placeholderKeyInConfig);
									if (placeholder == null) {
										Main.getPlugin().getLogger().warning("Dynamic feature contains rule with placeholder " + placeholder + " which has not been received from the server.");
										continue;
									}

									final String placeholderValueFromConnector;

									if (placeholder instanceof GlobalPlaceholder) {
										final GlobalPlaceholder global = (GlobalPlaceholder) placeholder;
										placeholderValueFromConnector = global.getValue();
									} else {
										final PlayerPlaceholder playerPlaceholder = (PlayerPlaceholder) placeholder;
										placeholderValueFromConnector = playerPlaceholder.getValue(player);
									}

									final ConfigurationSection dynamicSection = section.getConfigurationSection("dynamic." + dynamicKey);

									final String mode = dynamicSection.getString("mode", "equals");

									if (
											mode.equals("equals") && placeholderValueInConfig.equals(placeholderValueFromConnector) ||
											mode.equals("less") && Double.parseDouble(placeholderValueInConfig) > Double.parseDouble(placeholderValueFromConnector) ||
											mode.equals("more") && Double.parseDouble(placeholderValueInConfig) < Double.parseDouble(placeholderValueFromConnector)
											) {

										final String materialString = dynamicSection.getString("material");

										if (materialString == null) {
											player.sendMessage("Dynamic section '" + dynamicKey + "' is missing the material option");
											return;
										}

										if (materialString.equals("NONE") || materialString.equals("AIR")) {
											continue itemLoop;
										}

										builder = Main.getItemBuilderFromItemSection(player, dynamicSection);
										chosenSection = dynamicSection;
										break;
									}
								}
							}

							if (builder == null) {
								//No dynamic rule matched, fall back to online
								if (!section.isConfigurationSection("online")) {
									player.sendMessage("Error for item " + key);
									player.sendMessage("Online section does not exist");
									return;
								}

								final ConfigurationSection onlineSection = section.getConfigurationSection("online");

								final String materialString = onlineSection.getString("material");

								if (materialString == null) {
									player.sendMessage("Error for item " + key);
									player.sendMessage("Online section does not have a material option");
								}

								if (materialString.equals("NONE") || materialString.equals("AIR")) {
									continue;
								}

								builder = Main.getItemBuilderFromItemSection(player, onlineSection);
								chosenSection = onlineSection;
							}
						}

						final Map<String, String> placeholders = new HashMap<>();

						// Add global placeholders to list (uuid = null)
						for (final Placeholder placeholder : server.getPlaceholders()) {
							final String value;
							if (placeholder instanceof GlobalPlaceholder) {
								final GlobalPlaceholder global = (GlobalPlaceholder) placeholder;
								value = global.getValue();
							} else {
								final PlayerPlaceholder playerPlaceholder = (PlayerPlaceholder) placeholder;
								value = playerPlaceholder.getValue(player);
							}
							placeholders.put("{" + placeholder.getKey() + "}", value);
						}

						// Now parse the collected placeholders and papi placeholders in name and lore
						builder.namePlaceholders(placeholders).lorePlaceholders(placeholders);

						// Set item amount if dynamic item count is enabled
						if (section.getBoolean("dynamic-item-count", false)) {
							final int amount = server.getOnlinePlayers();
							builder.amount(amount < 1 || amount > 64 ? 1 : amount);
						}
					} else {
						// Server is offline
						if (!section.isConfigurationSection("offline")) {
							player.sendMessage("Offline section does not exist");
							return;
						}

						final ConfigurationSection offlineSection = section.getConfigurationSection("offline");

						final String materialString = offlineSection.getString("material");

						if (materialString == null) {
							player.sendMessage("Error for item " + key);
							player.sendMessage("Offline section does not have a material option");
						}

						if (materialString.equals("NONE") || materialString.equals("AIR")) {
							continue;
						}

						builder = Main.getItemBuilderFromItemSection(player, offlineSection);
						chosenSection = offlineSection;
					}
				} else {
					// Simple section
					final String materialString = section.getString("material");

					if (materialString == null) {
						player.sendMessage("Error for item " + key);
						player.sendMessage("Missing material option. Remember, this is not an advanced section, so don't use online/offline/dynamic sections in the config.");
						player.sendMessage("If you want to use these sections, add a connector option.");
						player.sendMessage("Read more here: https://github.com/ServerSelectorX/ServerSelectorX/wiki/Menu-items-v2");
					}

					if (materialString.equals("NONE") || materialString.equals("AIR")) {
						continue;
					}

					builder = Main.getItemBuilderFromItemSection(player, section);
					chosenSection = section;
				}
			}

			builder.papi(player)
				.placeholder("{player}", player.getName())
				.placeholder("{globalOnline}", ServerSelectorX.getGlobalPlayerCount() + "");

			// Add actions to item as NBT
			final NBTItem nbt = new NBTItem(builder.create());
			nbt.setObject("SSXActions", chosenSection.getStringList("actions"));
			nbt.setObject("SSXActionsLeft", chosenSection.getStringList("left-click-actions"));
			nbt.setObject("SSXActionsRight", chosenSection.getStringList("right-click-actions"));

			if (chosenSection.contains("cooldown")) {
				if (!chosenSection.isList("cooldown-actions")) {
					player.sendMessage("When using the 'cooldown' option, a list of actions 'cooldown-actions' must also be specified.");
					return;
				}

				nbt.setInteger("SSXCooldownTime", (int) (chosenSection.getDouble("cooldown") * 1000));
				nbt.setString("SSXCooldownId", player.getName() + this.getInventoryView().getTitle() + key);
				nbt.setObject("SSXCooldownActions", chosenSection.getStringList("cooldown-actions"));
			}

			final ItemStack item = nbt.getItem();

			if (key.equals("fill") || key.equals("-1")) { // -1 for backwards compatibility
				// Fill all blank slots
				for (int i = 0; i < this.getInventory().getSize(); i++) {
					if (!this.hasItem(i)) {
						this.addItem(i, item);
					}
				}
			} else {
				for (final String split : key.split(",")) {
					int slot;
					try {
						slot = Integer.parseInt(split);
					} catch (final NumberFormatException e) {
						player.sendMessage("Invalid slot number " + split);
						return;
					}

					if (slot >= this.getInventory().getSize() || slot < 0) {
						player.sendMessage("You put an item in slot " + slot + ", which is higher than the maximum number of slots in your menu.");
						player.sendMessage("Use numbers 0 to " + (this.getInventory().getSize() - 1) + "or increase the number of rows in the config");
						return;
					}

					this.addItem(slot, item);
				}
			}
			if (Main.LAG_DEBUG) {
				long current = System.nanoTime();
				Main.getPlugin().getLogger().info(String.format("  item %s took %.2fμs", key, (current - itemStart) / 1000f));
				itemStart = current;
			}
		}
	}

	@Override
	public boolean onOptionClick(final OptionClickEvent event) {
		final Player player = event.getPlayer();

		final NBTItem nbt = new NBTItem(event.getItemStack());

		@SuppressWarnings("unchecked")
		final List<String> actions = nbt.getObject("SSXActions", List.class);
		@SuppressWarnings("unchecked")
		final List<String> leftActions = nbt.getObject("SSXActionsLeft", List.class);
		@SuppressWarnings("unchecked")
		final List<String> rightActions = nbt.getObject("SSXActionsRight", List.class);

		if (nbt.hasKey("SSXCooldownTime")) {
			final int cooldownTime = nbt.getInteger("SSXCooldownTime");
			final String cooldownId = nbt.getString("SSXCooldownId");
			if (Cooldown.getCooldown(cooldownId) > 0) {
				@SuppressWarnings("unchecked")
				final List<String> cooldownActions = nbt.getObject("SSXCooldownActions", List.class);
				return Action.runActions(player, cooldownActions);
			} else {
				Cooldown.addCooldown(cooldownId, cooldownTime);
			}
		}

		boolean close = Action.runActions(player, actions);
		if (event.getClickType() == ClickType.RIGHT || event.getClickType() == ClickType.SHIFT_RIGHT) {
			close = close || Action.runActions(player, rightActions);
		} else if (event.getClickType() == ClickType.LEFT || event.getClickType() == ClickType.SHIFT_LEFT) {
			close = close || Action.runActions(player, leftActions);
		}
		return close;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onClose(final MenuCloseEvent event) {
		this.closed = true;

		// this cooldown is checked in the item click event
		// 1.16 clients for some inexplicable reason interact with hotbar items when
		// a menu item is clicked. To avoid the menu re-opening right after it is closed,
		// I use this cooldown.
		Cooldown.addCooldown("ssxitemglobal" + this.getPlayer().getName(), 100);

		if (!this.config.contains("on-close")) {
			return;
		}

		final String reason;
		switch(event.getReason()) {
		case PLAYER_CLOSED:
			reason = "player";
			break;
		case ITEM_CLICK:
			reason = "item";
			break;
		case PLAYER_QUIT:
			reason = "quit";
			break;
		default:
			reason = null;
		}

		if (reason == null) {
			return;
		}

		this.config.getMapList("on-close").forEach(map -> {
			final List<String> reasons = (List<String>) map.get("reasons");
			if (reasons.contains(reason)) {
				final List<String> actions = (List<String>) map.get("actions");
				if (event.getOfflinePlayer() instanceof Player) {
					Action.runActions(event.getPlayer(), actions);
				} else {
					Action.runActions(null, actions);
				}
			}
		});
	}

}

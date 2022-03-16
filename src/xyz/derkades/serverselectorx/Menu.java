package xyz.derkades.serverselectorx;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

				addItems();

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
			if (!this.config.isConfigurationSection("menu." + key)) {
				player.sendMessage("Invalid item " + key + ", check indentation.");
				continue;
			}

			final ConfigurationSection section = Objects.requireNonNull(this.config.getConfigurationSection("menu." + key), "Null configuration section: menu." + key);

			ConfigurationSection chosenSection = null;
			int amountOverride = -1;
			Map<String, String> placeholders = null;

			if (section.isString("permission") && !player.hasPermission(Objects.requireNonNull(section.getString("permission"), "null permission"))) {
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

				chosenSection = noPermissionSection;
			} else {
				// Player has permission, use other sections
				// Advanced server section. Use online, offline, dynamic sections.
				if (section.isString("connector")) {
					final String serverName = Objects.requireNonNull(section.getString("connector"), "connector server name null");
					final Server server = Server.getServer(serverName);

					if (server.isOnline()) {
						if (section.isConfigurationSection("connected")) {
							if (!configMisc.contains("server-name")) {
								player.sendMessage("To use the connected section, specify server-name in misc.yml");
								return;
							}

							if (serverName.equalsIgnoreCase(configMisc.getString("server-name"))) {
								chosenSection = section.getConfigurationSection("connected");
							}
						}

						// Connected section didn't match, try dynamic section next
						if (chosenSection == null &&
								section.isConfigurationSection("dynamic")) {
							for (final String dynamicKey : section.getConfigurationSection("dynamic").getKeys(false)) {
								final String[] split = dynamicKey.split(":");

								if (split.length != 2) {
									player.sendMessage("Invalid dynamic section '" + dynamicKey + "'. Dynamic section identifiers should contain exactly one colon.");
									return;
								}

								final String placeholderKeyInConfig = split[0];
								final String placeholderValueInConfig = split[1];

								final Placeholder placeholder = server.getPlaceholder(placeholderKeyInConfig);
								if (placeholder == null) {
									Main.getPlugin().getLogger().warning("Dynamic feature contains rule with placeholder " + placeholderKeyInConfig + " which has not been received from the server.");
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

									chosenSection = dynamicSection;
									break;
								}
							}
						}

						// If dynamic section didn't match either, fall back to online section
						if (chosenSection == null) {
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

							chosenSection = onlineSection;
						}

						// Build placeholder map
						placeholders = new HashMap<>();
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

						// Set item amount override if dynamic item count is enabled
						if (section.getBoolean("dynamic-item-count", false)) {
							final int amount = server.getOnlinePlayers();
							amountOverride = amount < 1 || amount > 64 ? 1 : amount;
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

					chosenSection = section;
				}
			}

			final ConfigurationSection chosenSectionFinal = chosenSection;
			final int amountOverrideFinal = amountOverride;
			final Map<String, String> placeholdersFinal = placeholders;

			Main.getItemBuilderFromItemSection(player, chosenSection, builder -> {
				if (amountOverrideFinal >= 0) {
					builder.amount(amountOverrideFinal);
				}
				if (placeholdersFinal != null) {
					builder.namePlaceholders(placeholdersFinal).lorePlaceholders(placeholdersFinal);
				}
				addToMenu(key, player, chosenSectionFinal, builder);
			});
		}
	}

	public void addToMenu(String key, Player player, ConfigurationSection section, NbtItemBuilder builder) {
		// Add actions to item as NBT
		builder.editNbt(nbt -> {
			nbt.setObject("SSXActions", section.getStringList("actions"));
			nbt.setObject("SSXActionsLeft", section.getStringList("left-click-actions"));
			nbt.setObject("SSXActionsRight", section.getStringList("right-click-actions"));

			if (section.contains("cooldown")) {
				if (!section.isList("cooldown-actions")) {
					player.sendMessage("When using the 'cooldown' option, a list of actions 'cooldown-actions' must also be specified.");
					return;
				}

				nbt.setInteger("SSXCooldownTime", (int) (section.getDouble("cooldown") * 1000));
				nbt.setString("SSXCooldownId", player.getName() + this.getInventoryView().getTitle() + key);
				nbt.setObject("SSXCooldownActions", section.getStringList("cooldown-actions"));
			}
		});

		final ItemStack item = builder.create();

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

		final ClickType click = event.getClickType();
		boolean rightClick = click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT;
		boolean leftClick = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;

		if (nbt.hasKey("SSXCooldownTime") &&
				( // Only apply cooldown if an action is about to be performed
					!actions.isEmpty() ||
					rightClick && !rightActions.isEmpty() ||
					leftClick && !leftActions.isEmpty()
				)
			) {
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
		if (rightClick) {
			close = close || Action.runActions(player, rightActions);
		} else if (leftClick) {
			close = close || Action.runActions(player, leftActions);
		}
		return close;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void onClose(final MenuCloseEvent event) {
		this.closed = true;

		if (this.config == null) {
			Main.getPlugin().getLogger().warning("Ignoring menu close event, config failed to load");
			return;
		}

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

package xyz.derkades.serverselectorx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.ListUtils;
import xyz.derkades.derkutils.Random;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.bukkit.menu.IconMenu;
import xyz.derkades.derkutils.bukkit.menu.MenuCloseEvent;
import xyz.derkades.derkutils.bukkit.menu.OptionClickEvent;

public class SelectorMenu extends IconMenu {

	private final FileConfiguration config;
	//private String configName;
	private final Player player;
	private final int slots;

	private BukkitTask refreshTimer;

	public SelectorMenu(final Player player, final FileConfiguration config, final String configName) {
		super(Main.getPlugin(), Colors.parseColors(config.getString("title", UUID.randomUUID().toString())), 9, player);
		this.config = config;
		//this.configName = configName;
		this.player = player;

		this.slots = config.getInt("rows", 6) * 9;
		this.setSize(this.slots);
	}

	@Override
	public void open() {
		this.addItems();

		Cooldown.addCooldown(this.config.getName() + this.player.getName(), 0); //Remove cooldown if menu opened successfully
		super.open();

		this.refreshTimer = Bukkit.getScheduler().runTaskTimer(Main.getPlugin(), () -> {
			this.addItems();
			super.refreshItems();
		}, 1*20, 1*20);
	}

	private void addItems() {
		for (final String key : this.config.getConfigurationSection("menu").getKeys(false)) {
			final ConfigurationSection section = this.config.getConfigurationSection("menu." + key);

			String materialString = "STONE";
			int data = 0;
			String name = "";
			List<String> lore = new ArrayList<>();
			int amount = 1;
			boolean enchanted = false;

			if (section.contains("permission") && !this.player.hasPermission(section.getString("permission"))) {
				// Use no-permission section
				final ConfigurationSection noPermissionSection = section.getConfigurationSection("no-permission");
				materialString = noPermissionSection.getString("item");
				data = noPermissionSection.getInt("data", 0);
				name = noPermissionSection.getString("name", "");
				lore = noPermissionSection.getStringList("lore");
				enchanted = noPermissionSection.getBoolean("enchanted", false);
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

				if (firstAction.startsWith("srv")) {
					String serverName = firstAction.substring(4);

					if (serverName.startsWith("__")) {
						serverName = serverName.substring(2);
					}

					if (Main.isOnline(serverName)) {
						final Map<String, String> placeholders = Main.PLACEHOLDERS.get(serverName);

						boolean dynamicMatchFound = false;

						if (section.contains("dynamic")) {
							for (final String dynamicKey : section.getConfigurationSection("dynamic").getKeys(false)) {
								final String placeholder = dynamicKey.split(":")[0];
								final String placeholderValueInConfig = dynamicKey.split(":")[1];
								final String placeholderValueFromConnector = placeholders.get(placeholder);

								if (!placeholders.containsKey(placeholder)) {
									Main.getPlugin().getLogger().warning("Dynamic feature contains rule with placeholder " + placeholder + " which has not been received from the server.");
									continue;
								}

								final ConfigurationSection dynamicSection = section.getConfigurationSection("dynamic." + dynamicKey);

								final String mode = dynamicSection.getString("mode", "equals");

								if (
										(mode.equals("equals") && placeholders.get(placeholder).equals(placeholderValueInConfig)) ||
										(mode.equals("less") && Double.parseDouble(placeholderValueInConfig) < Double.parseDouble(placeholderValueFromConnector)) ||
										(mode.equals("less") && Double.parseDouble(placeholderValueInConfig) > Double.parseDouble(placeholderValueFromConnector))
										) {
									//Placeholder result matches with placeholder result in rule
									dynamicMatchFound = true;

									materialString = dynamicSection.getString("item");
									data = dynamicSection.getInt("data", 0);
									name = dynamicSection.getString("name", "");
									lore = dynamicSection.getStringList("lore");
									enchanted = dynamicSection.getBoolean("enchanted", false);
								}
							}
						}

						if (!dynamicMatchFound) {
							//No dynamic rule matched, fall back to online
							materialString = section.getString("online.item");
							data = section.getInt("online.data", 0);
							name = section.getString("online.name", "");
							lore = section.getStringList("online.lore");
							enchanted = section.getBoolean("online.enchanted", false);
						}

						for (final Map.Entry<String, String> placeholder : placeholders.entrySet()) {
							final List<String> newLore = new ArrayList<>();
							for (final String string : lore) {
								newLore.add(string.replace("{" + placeholder.getKey() + "}", placeholder.getValue()));
							}
							lore = newLore;

							name = name.replace("{" + placeholder.getKey() + "}", placeholder.getValue());
						}

						if (section.getBoolean("dynamic-item-count", false)) {
							if (!placeholders.containsKey("online")) { //Check for very old SSX-Connector versions. Can be removed soon.
								Main.getPlugin().getLogger().warning("Dynamic item count is enabled but player count is unknown.");
								Main.getPlugin().getLogger().warning("Is the PlayerCount addon installed?");
							} else {
								amount = Integer.parseInt(placeholders.get("online"));
							}
						} else {
							amount = section.getInt("item-count", 1);
						}
					} else {
						//Server is offline
						final ConfigurationSection offlineSection = section.getConfigurationSection("offline");

						materialString = offlineSection.getString("item");
						data = offlineSection.getInt("data", 0);
						name = offlineSection.getString("name", "");
						lore = offlineSection.getStringList("lore");
						enchanted = offlineSection.getBoolean("enchanted", false);

						amount = section.getInt("item-count", 1); // When the server is offline, so the item count is not set by the player count
					}

				} else if (firstAction.startsWith("sel:")) {
					//Add all online counts of servers in the submenu
					int totalOnline = 0;

					final FileConfiguration subConfig = Main.getConfigurationManager().getMenuByName(firstAction.substring(4));
					for (final String subKey : subConfig.getConfigurationSection("menu").getKeys(false)){
						final ConfigurationSection subSection = subConfig.getConfigurationSection("menu." + subKey);
						final String subAction = subSection.getString("action");
						if (!subAction.startsWith("srv:"))
							continue;

						final String serverName = subAction.substring(4);

						if (!Main.PLACEHOLDERS.containsKey(serverName)) {
							continue;
						}

						final Map<String, String> placeholders = Main.PLACEHOLDERS.get(serverName);
						if (placeholders.containsKey("online")) {
							totalOnline += Integer.parseInt(placeholders.get("online"));
						}
					}

					materialString = section.getString("item");
					data = section.getInt("data", 0);
					name = section.getString("name", "");
					lore = ListUtils.replaceInStringList(section.getStringList("lore"), new Object[] {"{total}"}, new Object[] {totalOnline});
					enchanted = section.getBoolean("enchanted", false);
				} else {
					//Not a server
					materialString = section.getString("item");
					data = section.getInt("data", 0);
					name = section.getString("name", "");
					lore = section.getStringList("lore");
					enchanted = section.getBoolean("enchanted", false);
				}
			}

			// If data is set to -1, randomize data
			if (data < 0) {
				data = Random.getRandomInteger(0, 15);
			}

			if (materialString != "NONE") {
				final ItemBuilder builder;

				if (materialString.startsWith("head:")) {
					final String owner = materialString.split(":")[1];
					if (owner.equals("auto")) {
						builder = new ItemBuilder(this.player);
					} else {
						this.player.sendMessage("Custom player heads are not implemented in this version. You can only use 'head:auto'.");
						return;
					}
				} else {

					Material material;
					try {
						material = Material.valueOf(materialString);
					} catch (final IllegalArgumentException e) {
						this.player.sendMessage("Invalid item name '" + materialString + "'");
						return;
					}

					builder = new ItemBuilder(material);
				}

				name = name.replace("{player}", this.player.getName()).replace("{globalOnline}", "" + Main.getGlobalPlayerCount());
				lore = ListUtils.replaceInStringList(lore,
						new Object[] {"{player}", "{globalOnline}"},
						new Object[] {this.player.getName(), Main.getGlobalPlayerCount()});

				if (amount < 1 || amount > 64) amount = 1;

				builder.amount(amount);
				builder.name(Main.PLACEHOLDER_API.parsePlaceholders(this.player, name));
				builder.lore(Main.PLACEHOLDER_API.parsePlaceholders(this.player, lore));

				final int slot = Integer.valueOf(key);

				ItemStack item = builder.create();

				if (enchanted) item.addUnsafeEnchantment(Enchantment.DURABILITY, 1);

				if (section.getBoolean("hide-flags", false)) item = Main.addHideFlags(item);

				if (slot < 0) {
					for (int i = 0; i < this.slots; i++) {
						if (!this.items.containsKey(i)) {
							this.items.put(i, item);
						}
					}
				} else {
					this.items.put(slot, item);
				}
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

		for (final String action : actions) {
			if (action.startsWith("url:")){ //Send url message
				final String url = action.substring(4);
				final String message = Colors.parseColors(this.config.getString("url-message", "&3&lClick here"));

				player.spigot().sendMessage(
						new ComponentBuilder(message)
						.event(new ClickEvent(ClickEvent.Action.OPEN_URL, url))
						.create()
						);
			} else if (action.startsWith("cmd:")){ //Execute command
				final String command = action.substring(4);

				//Send command 2 ticks later to let the GUI close first (for commands that open a GUI)
				Bukkit.getScheduler().scheduleSyncDelayedTask(Main.getPlugin(), () -> {
					Bukkit.dispatchCommand(player, Main.PLACEHOLDER_API.parsePlaceholders(player, command));
				}, 2);
			} else if (action.startsWith("consolecmd:")) {
				String command = action.substring(11);
				command = command.replace("{player}", player.getName());
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), Main.PLACEHOLDER_API.parsePlaceholders(player, command));
			} else if (action.startsWith("bungeecmd:")) { //BungeeCord command
				final String command = action.substring(10);
				Bukkit.dispatchCommand(Bukkit.getConsoleSender(), String.format("sync player %s %s", player.getName(), Main.PLACEHOLDER_API.parsePlaceholders(player, command)));
			} else if (action.startsWith("sel:")){ //Open selector
				final String configName = action.substring(4);
				final FileConfiguration config = Main.getConfigurationManager().getMenuByName(configName);
				if (config == null){
					player.sendMessage(ChatColor.RED + "This server selector does not exist.");
				} else {
					new SelectorMenu(player, config, configName).open();
				}
			} else if (action.startsWith("world:")){ //Teleport to world
				final String worldName = action.substring(6);
				final World world = Bukkit.getWorld(worldName);
				if (world == null){
					player.sendMessage(ChatColor.RED + "A world with the name " + worldName + " does not exist.");
				} else {
					player.teleport(world.getSpawnLocation());
				}
			} else if (action.startsWith("srv:")){ //Teleport to server
				final String serverName = action.substring(4);

				// If offline-cancel-connect is turned on and the server is offline, send message and cancel connecting
				if (Main.getConfigurationManager().getGlobalConfig().getBoolean("offline-cancel-connect", false) && !Main.isOnline(serverName)) {
					player.sendMessage(Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("offline-cancel-connect-message", "error")));
				}

				Main.teleportPlayerToServer(player, serverName);
			} else if (action.equalsIgnoreCase("toggleInvis")) {
				if (player.hasPotionEffect(PotionEffectType.INVISIBILITY)) {
					player.removePotionEffect(PotionEffectType.INVISIBILITY);
					player.sendMessage(Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("invis-off")));
				} else {
					if (Main.getConfigurationManager().getGlobalConfig().getBoolean("show-particles"))
						player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true));
					else
						player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, true, false));

					player.sendMessage(Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("invis-on")));
				}
			} else if (action.equalsIgnoreCase("toggleSpeed")) {
				if (player.hasPotionEffect(PotionEffectType.SPEED)) {
					player.removePotionEffect(PotionEffectType.SPEED);
					player.sendMessage(Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("speed-off")));
				} else {
					final int amplifier = Main.getConfigurationManager().getGlobalConfig().getInt("speed-amplifier", 3);

					if (Main.getConfigurationManager().getGlobalConfig().getBoolean("show-particles"))
						player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true));
					else
						player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, Integer.MAX_VALUE, amplifier, true, false));

					player.sendMessage(Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("speed-on")));
				}
			} else if (action.equalsIgnoreCase("toggleHideOthers")) {
				if (InvisibilityToggle.hasHiddenOthers(player)) {
					InvisibilityToggle.showOthers(player);
					player.sendMessage(Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("show-others")));
				} else {
					InvisibilityToggle.hideOthers(player);
					player.sendMessage(Colors.parseColors(Main.getConfigurationManager().getGlobalConfig().getString("hide-others")));
				}
			} else if (action.startsWith("msg:")){ //Send message
				final String message = action.substring(4);
				player.sendMessage(Main.PLACEHOLDER_API.parsePlaceholders(player, message));
			} else if (action.equals("close")){ //Close selector
				return true; //Return true = close
			} else if (action.equals("none")){
				continue;
			} else {
				player.sendMessage(ChatColor.RED + "unknown action");
				continue;
			}
		}

		return false;
	}

	@Override
	public void onClose(final MenuCloseEvent event) {
		if (this.refreshTimer != null) this.refreshTimer.cancel();
	}

}

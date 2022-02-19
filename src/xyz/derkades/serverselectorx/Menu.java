package xyz.derkades.serverselectorx;

import de.tr7zw.changeme.nbtapi.NBTItem;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.menu.IconMenu;
import xyz.derkades.derkutils.bukkit.menu.MenuCloseEvent;
import xyz.derkades.derkutils.bukkit.menu.OptionClickEvent;
import xyz.derkades.serverselectorx.actions.Action;
import xyz.derkades.serverselectorx.conditional.ConditionalItem;

import java.util.List;
import java.util.Objects;

public class Menu extends IconMenu {

	private final FileConfiguration config;
	private final String configName;

	private boolean closed = false;

	public Menu(final Player player, final @Nullable FileConfiguration config, final String configName) {
		super(Main.getPlugin(),
				config == null ? "error" : Colors.parseColors(config.getString("title", "TITLE MISSING")),
				config == null ? 1 : config.getInt("rows", 6),
				player);

		this.config = config;
		this.configName = configName;

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

			final ConfigurationSection section = Objects.requireNonNull(this.config.getConfigurationSection("menu." + key),
					"Null configuration section: menu." + key);

			final String cooldownId = player.getName() + this.configName + key;
			try {
				ConditionalItem.getItem(player, section, cooldownId, item -> {
					addToMenu(key, player, item);
				});
			} catch (InvalidConfigurationException e) {
				player.sendMessage("Invalid configuration: " + e.getMessage());
			}
		}
	}

	public void addToMenu(@NotNull String key, @NotNull Player player, @NotNull ItemStack item) {
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
		ItemStack item = event.getItemStack();
		if (item == null) {
			Main.getPlugin().getLogger().warning("Received click event for null item. This is a bug.");
			return false;
		}

		final NBTItem nbt = new NBTItem(item);

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

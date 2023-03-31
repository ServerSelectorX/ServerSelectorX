package xyz.derkades.serverselectorx.conditional;

import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NBTItem;
import de.tr7zw.changeme.nbtapi.NbtApiException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.Color;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil;
import xyz.derkades.derkutils.bukkit.menu.OptionClickEvent;
import xyz.derkades.serverselectorx.Main;
import xyz.derkades.serverselectorx.ServerSelectorX;
import xyz.derkades.serverselectorx.actions.Action;
import xyz.derkades.serverselectorx.conditional.condition.Condition;
import xyz.derkades.serverselectorx.conditional.condition.Conditions;
import xyz.derkades.serverselectorx.placeholders.GlobalPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Placeholder;
import xyz.derkades.serverselectorx.placeholders.PlayerPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Server;

import java.util.*;
import java.util.function.Consumer;

import static org.bukkit.event.block.Action.*;

public class ConditionalItem {

	@SuppressWarnings("unchecked")
	private static Map<String, Object> matchSection(Player player, ConfigurationSection globalSection) throws InvalidConfigurationException {
		if (!globalSection.contains("conditional")) {
			return sectionToMap(globalSection);
		}

		List<Map<?, ?>> conditionalsList = globalSection.getMapList("conditional");

		for (Map<?, ?> genericMap : conditionalsList) {
			Map<String, Object> map = (Map<String, Object>) genericMap;

			// Add options from global section to this map
			for (String key : globalSection.getKeys(false)) {
				map.putIfAbsent(key, globalSection.get(key));
			}

			if (!map.containsKey("type")) {
				throw new InvalidConfigurationException("Missing 'type' option for a conditional");
			}

			String type = (String) map.get("type");
			boolean invert = (boolean) map.getOrDefault("invert-condition", false);

			Condition condition = Conditions.getConditionByType(type);
			if (condition == null) {
				throw new InvalidConfigurationException("Unknown condition type: " + type);
			}
			if (condition.isTrue(player, map) != invert) {
				return map;
			}
		}

		return sectionToMap(globalSection);
	}

	private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
			.character(ChatColor.COLOR_CHAR)
			.hexColors()
			.useUnusualXRepeatedCharacterHexFormat()
			.build();

	private static Map<String, Object> sectionToMap(ConfigurationSection section) {
		Map<String, Object> map = new HashMap<>();
		for (String key : section.getKeys(false)) {
			map.put(key, section.get(key));
		}
		return map;
	}

	@SuppressWarnings("unchecked")
	public static void getItem(@NotNull Player player, @NotNull ConfigurationSection section,
							   @NotNull String cooldownId, @NotNull Consumer<@NotNull ItemStack> consumer)
			throws InvalidConfigurationException {

		Map<String, Object> matchedSection = matchSection(player, section);

		final String materialString = (String) matchedSection.getOrDefault("material", null);

		if (materialString == null) {
			throw new InvalidConfigurationException("Material is missing from config or null");
		}

		Main.getItemBuilderFromMaterialString(player, materialString, builder -> {
			final boolean useMiniMessage = (boolean) matchedSection.getOrDefault("minimessage", false);
			final @NotNull String title = (String) matchedSection.getOrDefault("title", " ");
			final @NotNull List<String> lore = (List<String>) matchedSection.getOrDefault("lore", Collections.emptyList());
			final boolean enchanted = (boolean) matchedSection.getOrDefault("enchanted", false);
			final boolean hideFlags = (boolean) matchedSection.getOrDefault("hide-flags", true);
			final int amount = (int) matchedSection.getOrDefault("amount", 1);
			final int durability = (int) matchedSection.getOrDefault("durability", -1);
			final int data = (int) matchedSection.getOrDefault("data", -1); // the same as durability, for compatibility
			final @Nullable String nbtJson = (String) matchedSection.getOrDefault("nbt", null);
			final @NotNull List<String> actions = (List<String>) matchedSection.getOrDefault("actions", Collections.emptyList());
			final @NotNull List<String> leftClickActions = (List<String>) matchedSection.getOrDefault("left-click-actions", Collections.emptyList());
			final @NotNull List<String> rightClickActions = (List<String>) matchedSection.getOrDefault("right-click-actions", Collections.emptyList());
			final int cooldownTime = (int) matchedSection.getOrDefault("cooldown", 0);
			final @NotNull List<String> cooldownActions = (List<String>) matchedSection.getOrDefault("cooldown-actions", Collections.emptyList());
			final @Nullable String serverName = (String) matchedSection.get("server-name");
			final @Nullable String color = (String) matchedSection.get("color");

			final PlaceholderUtil.Placeholder playerNamePlaceholder = new PlaceholderUtil.Placeholder("{player}", player.getName());
			final PlaceholderUtil.Placeholder globalOnlinePlaceholder = new PlaceholderUtil.Placeholder("{globalOnline}", String.valueOf(ServerSelectorX.getGlobalPlayerCount()));
			final List<PlaceholderUtil.Placeholder> additionalPlaceholders = new ArrayList<>(2);
			additionalPlaceholders.add(playerNamePlaceholder);
			additionalPlaceholders.add(globalOnlinePlaceholder);

			if (serverName != null) {
				Server server = Server.getServer(serverName);
				if (server.isOnline()) {
					for (final Placeholder placeholder : server.getPlaceholders()) {
						final String value = placeholder instanceof GlobalPlaceholder
								? ((GlobalPlaceholder) placeholder).getValue()
								: ((PlayerPlaceholder) placeholder).getValue(player);
						additionalPlaceholders.add(new PlaceholderUtil.Placeholder("{" + placeholder.getKey() + "}", value));
					}
				}
			}

			String parsedTitle = PlaceholderUtil.parsePapiPlaceholders(player, title, additionalPlaceholders);
			if (useMiniMessage) {
				Component c = MiniMessage.miniMessage().deserialize(parsedTitle);
				parsedTitle = LEGACY_COMPONENT_SERIALIZER.serialize(c);
				builder.name(parsedTitle);
			} else {
				parsedTitle = "&r&f" + parsedTitle;
				builder.coloredName(parsedTitle);
			}

			if (!lore.isEmpty()) {
				List<String> parsedLore = new ArrayList<>(lore.size());
				for (String line : lore) {
					String parsedLine = PlaceholderUtil.parsePapiPlaceholders(player, line, additionalPlaceholders);
					parsedLine = useMiniMessage
							? LEGACY_COMPONENT_SERIALIZER.serialize(MiniMessage.miniMessage().deserialize(parsedLine))
							: "&r&f" + parsedLine;
					parsedLore.add(parsedLine);
				}
				if (useMiniMessage) {
					builder.lore(parsedLore);
				} else {
					builder.coloredLore(parsedLore);
				}
			}

			if (enchanted) {
				builder.unsafeEnchant(Enchantment.DURABILITY, 1);
			}

			builder.hideFlags(hideFlags);

			builder.amount(amount);

			if (durability >= 0) {
				builder.damage(durability);
			}

			if (data >= 0) {
				builder.damage(data);
			}

			if (color != null) {
				if (color.length() != 7 || color.charAt(0) != '#') {
					player.sendMessage("Invalid color '" + color + "', should be a # followed by 6 hex digits");
					return;
				}
				
				int r = Integer.parseInt(color.substring(1, 3), 16);
				int g = Integer.parseInt(color.substring(3, 5), 16);
				int b = Integer.parseInt(color.substring(5, 7), 16);
				builder.leatherArmorColor(Color.fromRGB(r, g, b));
			}

			if (nbtJson != null) {
				try {
					NBTContainer container = new NBTContainer(nbtJson);
					builder.editNbt(nbt -> nbt.mergeCompound(container));
				} catch (NbtApiException e) {
					player.sendMessage("Skipped adding custom NBT to an item because of an error, please see the console for more info.");
					e.printStackTrace();
				}
			}

			builder.editNbt(nbt -> {
				nbt.getStringList("SSXActions").addAll(actions);
				nbt.getStringList("SSXActionsLeft").addAll(leftClickActions);
				nbt.getStringList("SSXActionsRight").addAll(rightClickActions);

				if (cooldownTime > 0) {
					nbt.setInteger("SSXCooldownTime", cooldownTime);
					nbt.setString("SSXCooldownId", cooldownId);
					nbt.getStringList("SSXCooldownActions").addAll(cooldownActions);
				}
			});

			consumer.accept(builder.create());
		});
	}

	public static boolean runActions(OptionClickEvent event) {
		final ClickType click = event.getClickType();
		boolean leftClick = click == ClickType.LEFT || click == ClickType.SHIFT_LEFT;
		boolean rightClick = click == ClickType.RIGHT || click == ClickType.SHIFT_RIGHT;
		return runActions(event.getPlayer(), event.getItemStack(), leftClick, rightClick);
	}

	public static boolean runActions(PlayerInteractEvent event) {
		boolean leftClick = event.getAction() == LEFT_CLICK_AIR || event.getAction() == LEFT_CLICK_BLOCK;
		boolean rightClick = event.getAction() == RIGHT_CLICK_AIR || event.getAction() == RIGHT_CLICK_BLOCK;
		return runActions(event.getPlayer(), event.getItem(), leftClick, rightClick);
	}

	private static boolean runActions(Player player, ItemStack item, boolean isLeftClick, boolean isRightClick) {
		if (item == null) {
			Main.getPlugin().getLogger().warning("Received click event for null item. This is a bug.");
			return false;
		}

		NBTItem nbt = new NBTItem(item);

		final List<String> actions = nbt.getStringList("SSXActions");
		final List<String> leftActions = nbt.getStringList("SSXActionsLeft");
		final List<String> rightActions = nbt.getStringList("SSXActionsRight");
		if (nbt.hasTag("SSXCooldownTime") &&
				( // Only apply cooldown if an action is about to be performed
						!actions.isEmpty() ||
								isRightClick && !rightActions.isEmpty() ||
								isLeftClick && !leftActions.isEmpty()
				)
		) {
			final int cooldownTime = nbt.getInteger("SSXCooldownTime");
			final String cooldownId = nbt.getString("SSXCooldownId");
			if (Cooldown.getCooldown(cooldownId) > 0) {
				final List<String> cooldownActions = nbt.getStringList("SSXCooldownActions");
				return Action.runActions(player, cooldownActions);
			} else {
				Cooldown.addCooldown(cooldownId, cooldownTime);
			}
		}

		boolean close = false;
		if (actions != null) {
			close = Action.runActions(player, actions);
		}
		if (isRightClick && rightActions != null) {
			close |= Action.runActions(player, rightActions);
		} else if (isLeftClick && leftActions != null) {
			close |= Action.runActions(player, leftActions);
		}
		return close;

	}

}

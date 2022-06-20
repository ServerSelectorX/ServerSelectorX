package xyz.derkades.serverselectorx.conditional;

import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NbtApiException;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.ChatColor;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil;
import xyz.derkades.serverselectorx.Main;
import xyz.derkades.serverselectorx.ServerSelectorX;
import xyz.derkades.serverselectorx.conditional.condition.Condition;
import xyz.derkades.serverselectorx.conditional.condition.Conditions;
import xyz.derkades.serverselectorx.placeholders.GlobalPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Placeholder;
import xyz.derkades.serverselectorx.placeholders.PlayerPlaceholder;
import xyz.derkades.serverselectorx.placeholders.Server;

import java.util.*;
import java.util.function.Consumer;

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

	private static <T> T getOption(@Nullable Map<String, Object> matchedSection,
								   ConfigurationSection section,
								   String name,
								   @Nullable T def) {
		if (matchedSection != null && matchedSection.containsKey(name)) {
			return (T) matchedSection.get(name);
		} else if (section.contains(name)) {
			return (T) section.get(name);
		} else {
			return def;
		}
	}

	@SuppressWarnings("unchecked")
	public static void getItem(@NotNull Player player, @NotNull ConfigurationSection section,
							   @NotNull String cooldownId, @NotNull Consumer<@NotNull ItemStack> consumer)
			throws InvalidConfigurationException {

		Map<String, Object> matchedSection = matchSection(player, section);

		final String materialString = getOption(matchedSection, section, "material", null);

		if (materialString == null) {
			throw new InvalidConfigurationException("Material is missing from config or null");
		}

		Main.getItemBuilderFromMaterialString(player, materialString, builder -> {
			final boolean useMiniMessage = (boolean) matchedSection.getOrDefault("minimessage", false);
			final @NotNull String title = (String) matchedSection.getOrDefault("title", " ");
			final @NotNull List<String> lore = (List<String>) matchedSection.getOrDefault("lore", Collections.emptyList());
			final boolean enchanted = (boolean) matchedSection.getOrDefault("enchanted", false);
			final boolean hideFlags = (boolean) matchedSection.getOrDefault("hideFlags", false);
			final int amount = (int) matchedSection.getOrDefault("amount", 1);
			final int durability = (int) matchedSection.getOrDefault("durability", -1);
			final @Nullable String nbtJson = (String) matchedSection.getOrDefault("nbt", null);
			final @NotNull List<String> actions = (List<String>) matchedSection.getOrDefault("actions", Collections.emptyList());
			final @NotNull List<String> leftClickActions = (List<String>) matchedSection.getOrDefault("left-click-actions", Collections.emptyList());
			final @NotNull List<String> rightClickActions = (List<String>) matchedSection.getOrDefault("right-click-actions", Collections.emptyList());
			final int cooldownTime = (int) matchedSection.getOrDefault("cooldown", 0);
			final @NotNull List<String> cooldownActions = (List<String>) matchedSection.getOrDefault("cooldown-actions", Collections.emptyList());
			final @Nullable String serverName = (String) matchedSection.getOrDefault("server-name", null);

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

			if (nbtJson != null) {
				try {
					NBTContainer container = new NBTContainer(nbtJson);
					Object compound = container.getCompound();
					if (compound instanceof NBTCompound) {
						builder.editNbt(nbt -> nbt.mergeCompound((NBTCompound) compound));
					} else {
						player.sendMessage("Custom NBT is not a compound? " + compound.getClass().getSimpleName());
					}
				} catch (NbtApiException e) {
					player.sendMessage("Skipped adding custom NBT to an item because of an error, please see the console for more info.");
					e.printStackTrace();
				}
			}

			builder.editNbt(nbt -> {
				nbt.setObject("SSXActions", actions);
				nbt.setObject("SSXActionsLeft", leftClickActions);
				nbt.setObject("SSXActionsRight", rightClickActions);

				if (section.contains("cooldown")) {
					if (!section.isList("cooldown-actions")) {
						player.sendMessage("When using the 'cooldown' option, a list of actions 'cooldown-actions' must also be specified.");
						return;
					}

					if (cooldownTime > 0) {
						nbt.setInteger("SSXCooldownTime", cooldownTime);
						nbt.setString("SSXCooldownId", cooldownId);
						nbt.setObject("SSXCooldownActions", cooldownActions);
					}
				}
			});

			consumer.accept(builder.create());
		});
	}

}

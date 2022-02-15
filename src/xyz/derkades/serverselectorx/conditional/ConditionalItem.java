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

import java.util.*;
import java.util.function.Consumer;

public class ConditionalItem {

	private static @Nullable Map<String, Object> matchSection(Player player, List<Map<?, ?>> conditionalsList) throws InvalidConfigurationException {
		for (Map<?, ?> genericMap : conditionalsList) {
			Map<String, Object> map = (Map<String, Object>) genericMap;
			if (!map.containsKey("type")) {
				throw new InvalidConfigurationException("Missing 'type' option for a conditional");
			}
			String type = (String) map.get("type");
			Condition condition = Conditions.getConditionByType(type);
			if (condition.isTrue(player, map)) {
				return map;
			}
		}
		return null;
	}

	private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
			.character(ChatColor.COLOR_CHAR)
			.hexColors()
			.useUnusualXRepeatedCharacterHexFormat()
			.build();

	public static void getItem(@NotNull Player player, @NotNull ConfigurationSection section,
									  @NotNull String cooldownId, @NotNull Consumer<@NotNull ItemStack> consumer)
			throws InvalidConfigurationException {
		final @Nullable Map<String, Object> matchedSection = section.contains("conditional") ?
				matchSection(player, section.getMapList("conditional")) :
				null;

		@NotNull String materialString = matchedSection != null ?
				(String) matchedSection.get("material") :
				section.getString("material");

		Main.getItemBuilderFromMaterialString(player, materialString, builder -> {
			final boolean useMiniMessage; // default: false
			final @NotNull String title; // default: space
			final @NotNull List<String> lore; // default: empty list
			final boolean enchanted; // default: false
			final boolean hideFlags; // default true
			final int amount; // default: 1
			final int durability; // default: -1 (ignored by if statement)
			final @Nullable String nbtJson; // default: null (ignored by if statement)
			final @NotNull List<String> actions; // default: empty list
			final @NotNull List<String> leftClickActions; // default: empty list
			final @NotNull List<String> rightClickActions; // default: empty list
			final int cooldownTime; // default: 0
			final @NotNull List<String> cooldownActions; // default: empty list;
			if (matchedSection != null) {
				useMiniMessage = (boolean) matchedSection.getOrDefault("minimessage", false);
				title = (String) matchedSection.getOrDefault("title", " ");
				lore = (List<String>) matchedSection.getOrDefault("lore", Collections.emptyList());
				enchanted = (boolean) matchedSection.getOrDefault("enchanted", false);
				hideFlags = (boolean) matchedSection.getOrDefault("hide-flags", true);
				amount = (int) matchedSection.getOrDefault("amount", 1);
				durability = (int) matchedSection.getOrDefault("durability", -1);
				nbtJson = (String) matchedSection.getOrDefault("nbt", null);
				actions = (List<String>) matchedSection.getOrDefault("actions", Collections.emptyList());
				leftClickActions = (List<String>) matchedSection.getOrDefault("left-click-actions", Collections.emptyList());
				rightClickActions = (List<String>) matchedSection.getOrDefault("right-click-actions", Collections.emptyList());
				cooldownTime = (int) matchedSection.getOrDefault("cooldown", 0);
				cooldownActions = (List<String>) matchedSection.getOrDefault("cooldown-actions", Collections.emptyList());
			} else {
				useMiniMessage = section.getBoolean("minimessage", false);
				title = Objects.requireNonNull(section.getString("title", " "));
				lore = section.getStringList("lore");
				enchanted = section.getBoolean("enchanted", false);
				hideFlags = section.getBoolean("hide-flags", true);
				amount = section.getInt("amount", 1);
				durability = section.getInt("durability", -1);
				nbtJson = section.getString("nbt", null);
				actions = section.getStringList("actions");
				leftClickActions = section.getStringList("left-click-actions");
				rightClickActions = section.getStringList("right-click-actions");
				cooldownTime = section.getInt("cooldown");
				cooldownActions = section.getStringList("cooldown-actions");
			}

			final PlaceholderUtil.Placeholder playerPlaceholder = new PlaceholderUtil.Placeholder("{player}", player.getName());
			final PlaceholderUtil.Placeholder globalOnlinePlaceholder = new PlaceholderUtil.Placeholder("{globalOnline}", String.valueOf(ServerSelectorX.getGlobalPlayerCount()));
			final PlaceholderUtil.Placeholder[] additionalPlaceholders = new PlaceholderUtil.Placeholder[] {
					playerPlaceholder,
					globalOnlinePlaceholder,
			};


			String parsedTitle = PlaceholderUtil.parsePapiPlaceholders(player, title, additionalPlaceholders);
			if (useMiniMessage) {
				Component c = MiniMessage.get().deserialize(parsedTitle);
				parsedTitle = LEGACY_COMPONENT_SERIALIZER.serialize(c);
				builder.name(title);
			} else {
				parsedTitle = "&r&f" + parsedTitle;
				builder.coloredName(parsedTitle);
			}

			if (!lore.isEmpty()) {
				List<String> parsedLore = new ArrayList<>(lore.size());
				for (String line : lore) {
					String parsedLine = PlaceholderUtil.parsePapiPlaceholders(player, line, additionalPlaceholders);
					parsedLine = useMiniMessage
							? LEGACY_COMPONENT_SERIALIZER.serialize(MiniMessage.get().deserialize(parsedLine))
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



//			consumer.accept(new ConditionalItemReturn();
		});
	}

}

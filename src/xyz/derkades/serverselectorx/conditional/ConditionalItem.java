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
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.derkades.derkutils.bukkit.NbtItemBuilder;
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

	public static void getItemBuilder(Player player, ConfigurationSection section, Consumer<NbtItemBuilder> itemBuilderConsumerConsumer) throws InvalidConfigurationException {
		final @Nullable Map<String, Object> matchedSection = section.contains("conditional") ?
				matchSection(player, section.getMapList("conditional")) :
				null;

		@NotNull String materialString = matchedSection != null ?
				(String) matchedSection.get("material") :
				section.getString("material");

		Main.getItemBuilderFromMaterialString(player, materialString, builder -> {
			boolean useMiniMessage; // default: false
			@NotNull String title; // default: space
			@NotNull List<String> lore; // needs to be mutable. default: empty list
			boolean enchanted; // default: false
			boolean hideFlags; // default true
			int amount; // default: 1
			int durability; // default: -1 (ignored by if statement)
			@Nullable String nbtJson; // default: null (ignored by if statement)
			if (matchedSection != null) {
				useMiniMessage = (boolean) matchedSection.getOrDefault("minimessage", false);
				title = (String) matchedSection.getOrDefault("title", " ");
				lore = new ArrayList<>((List<String>) matchedSection.getOrDefault("lore", Collections.emptyList()));
				enchanted = (boolean) matchedSection.getOrDefault("enchanted", false);
				hideFlags = (boolean) matchedSection.getOrDefault("hide-flags", true);
				amount = (int) matchedSection.getOrDefault("amount", 1);
				durability = (int) matchedSection.getOrDefault("durability", -1);
				nbtJson = (String) matchedSection.getOrDefault("nbt", null);
			} else {
				useMiniMessage = section.getBoolean("minimessage", false);
				title = Objects.requireNonNull(section.getString("title", " "));
				lore = new ArrayList<>(section.getStringList("lore"));
				enchanted = section.getBoolean("enchanted", false);
				hideFlags = section.getBoolean("hide-flags", true);
				amount = section.getInt("amount", 1);
				durability = section.getInt("durability", -1);
				nbtJson = section.getString("nbt", null);
			}

			final PlaceholderUtil.Placeholder playerPlaceholder = new PlaceholderUtil.Placeholder("{player}", player.getName());
			final PlaceholderUtil.Placeholder globalOnlinePlaceholder = new PlaceholderUtil.Placeholder("{globalOnline}", String.valueOf(ServerSelectorX.getGlobalPlayerCount()));
			final PlaceholderUtil.Placeholder[] additionalPlaceholders = new PlaceholderUtil.Placeholder[] {
					playerPlaceholder,
					globalOnlinePlaceholder,
			};

			title = PlaceholderUtil.parsePapiPlaceholders(player, title, additionalPlaceholders);
			if (useMiniMessage) {
				Component c = MiniMessage.get().deserialize(title);
				title = LEGACY_COMPONENT_SERIALIZER.serialize(c);
				builder.name(title);
			} else {
				title = "&r&f" + title;
				builder.coloredName(title);
			}

			if (!lore.isEmpty()) {
				for (int i = 0; i < lore.size(); i++) {
					String line = lore.get(i);

					line = PlaceholderUtil.parsePapiPlaceholders(player, line, additionalPlaceholders);
					lore.set(i, useMiniMessage
							? LEGACY_COMPONENT_SERIALIZER.serialize(MiniMessage.get().deserialize(line))
							: "&r&f" + line);
				}
				if (useMiniMessage) {
					builder.lore(lore);
				} else {
					builder.coloredLore(lore);
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

			itemBuilderConsumerConsumer.accept(builder);
		});
	}

}

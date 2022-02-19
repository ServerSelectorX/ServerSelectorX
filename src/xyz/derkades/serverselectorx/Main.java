package xyz.derkades.serverselectorx;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.tr7zw.changeme.nbtapi.NBTCompound;
import de.tr7zw.changeme.nbtapi.NBTContainer;
import de.tr7zw.changeme.nbtapi.NbtApiException;
import de.tr7zw.changeme.nbtapi.utils.MinecraftVersion;
import net.kyori.adventure.platform.bukkit.BukkitAudiences;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import xyz.derkades.derkutils.bukkit.NbtItemBuilder;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil;
import xyz.derkades.serverselectorx.configuration.ConfigSync;
import xyz.derkades.serverselectorx.configuration.ConfigurationManager;
import xyz.derkades.serverselectorx.placeholders.PapiExpansionRegistrar;
import xyz.derkades.serverselectorx.placeholders.Server;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;
import java.util.function.Consumer;

public class Main extends JavaPlugin {

	static {
		// Disable NBT API update checker
		MinecraftVersion.disableUpdateCheck();
	}

	// When set to true, debug information related to giving items on join is printed to the
	// console. This boolean is enabled using /ssx lagdebug
	static boolean ITEM_DEBUG = false;

	private static ConfigurationManager configurationManager;
	private static ConfigSync configSync;

	private static Main plugin;

	private static BukkitAudiences adventure;

	public static WebServer server;

	public static final Gson GSON = new GsonBuilder().registerTypeAdapter(Server.class, Server.SERIALIZER).create();

	private static final Map<UUID, String> HEAD_TEXTURE_CACHE = new HashMap<>();

	@SuppressWarnings("null")
	@NotNull
	public static Main getPlugin(){
		return plugin;
	}

	@Override
	public void onEnable() {
		plugin = this;

		MinecraftVersion.replaceLogger(this.getLogger());

		configurationManager = new ConfigurationManager();
		try {
			configurationManager.reload();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		this.getCommand("serverselectorx").setExecutor(new ServerSelectorXCommand());
		this.getCommand("serverselectorx").setTabCompleter(new ServerSelectorXCommandCompleter());

		// Register custom selector commands
		Commands.registerCustomCommands();

		adventure = BukkitAudiences.create(this);

		server = new WebServer();
		server.start();

		configSync = new ConfigSync();
		new Stats();
		new ItemMoveDropCancelListener();

		Bukkit.getPluginManager().registerEvents(new ItemClickListener(), this);
		Bukkit.getPluginManager().registerEvents(new ItemGiveListener(), this);
		Bukkit.getPluginManager().registerEvents(new BetaMessageJoinListener(), this);
		Bukkit.getPluginManager().registerEvents(new ActionsOnJoinListener(), this);

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			PapiExpansionRegistrar.register();
		}
	}

	@Override
	public void onDisable() {
		if (adventure != null) {
			adventure.close();
			adventure = null;
		}

		if (server != null) {
			server.stop();
		}
	}

	public static ConfigurationManager getConfigurationManager() {
		return configurationManager;
	}

	static ConfigSync getConfigSync() {
		return configSync;
	}

	public static BukkitAudiences adventure() {
		return adventure;
	}

	private static final LegacyComponentSerializer LEGACY_COMPONENT_SERIALIZER = LegacyComponentSerializer.builder()
			.character(ChatColor.COLOR_CHAR)
			.hexColors()
			.useUnusualXRepeatedCharacterHexFormat()
			.build();

    public static void getItemBuilderFromItemSection(final @NotNull Player player,
													 final @NotNull ConfigurationSection section,
													 final @NotNull Consumer<NbtItemBuilder> itemConsumer) {
    	final String materialString = section.getString("material");
		getItemBuilderFromMaterialString(player, materialString, builder -> {
			boolean useMiniMessage = section.getBoolean("minimessage", false);

			final PlaceholderUtil.Placeholder playerPlaceholder = new PlaceholderUtil.Placeholder("{player}", player.getName());
			final PlaceholderUtil.Placeholder globalOnlinePlaceholder = new PlaceholderUtil.Placeholder("{globalOnline}", String.valueOf(ServerSelectorX.getGlobalPlayerCount()));
			final PlaceholderUtil.Placeholder[] additionalPlaceholders = new PlaceholderUtil.Placeholder[] {
					playerPlaceholder,
					globalOnlinePlaceholder,
			};

			if (section.contains("title")) {
				String title = section.getString("title");
				title = PlaceholderUtil.parsePapiPlaceholders(player, title, additionalPlaceholders);
				if (useMiniMessage) {
					Component c = MiniMessage.get().deserialize(title);
					title = LEGACY_COMPONENT_SERIALIZER.serialize(c);
					builder.name(title);
				} else {
					title = "&r&f" + title;
					builder.coloredName(title);
				}
			} else {
				builder.name(" ");
			}

			if (section.contains("lore")) {
				List<String> lore = new ArrayList<>(section.getStringList("lore"));
				for (int i = 0; i < lore.size(); i++) {
					String line = lore.get(i);

					line = PlaceholderUtil.parsePapiPlaceholders(player, line, additionalPlaceholders);
					if (useMiniMessage) {
						Component c = MiniMessage.get().deserialize(line);
						line = LEGACY_COMPONENT_SERIALIZER.serialize(c);
					} else {
						line = "&r&f" + line;
					}
					lore.set(i, line);
				}
				if (useMiniMessage) {
					builder.lore(lore);
				} else {
					builder.coloredLore(lore);
				}
			}
			boolean hideFlagsDefault = false;
			if (section.getBoolean("enchanted")) {
				builder.unsafeEnchant(Enchantment.DURABILITY, 1);
				hideFlagsDefault = true;
			}

			if (section.getBoolean("hide-flags", hideFlagsDefault)) {
				builder.hideFlags();
			}

			if (section.isInt("amount")) {
				builder.amount(section.getInt("amount"));
			}

			if (section.isInt("durability")) {
				builder.damage(section.getInt("durability"));
			}

			if (section.isString("nbt")) {
				try {
					NBTContainer container = new NBTContainer(section.getString("nbt"));
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

			itemConsumer.accept(builder);
		});
    }

    private static void getItemBuilderFromMaterialString(final Player player, @Nullable String materialString, Consumer<NbtItemBuilder> builderConsumer) {
		if (materialString == null) {
			return;
		}

		if (materialString.charAt(0) == '!') {
			materialString = PlaceholderUtil.parsePapiPlaceholders(player, materialString.substring(1));
		}

		if (materialString.startsWith("head:")) {
			final String owner = materialString.split(":")[1];
			if (owner.equals("auto")) {
				if (getConfigurationManager().getMiscConfiguration().getBoolean("mojang-api-head-auto", false)) {
					getHeadTexture(player.getUniqueId(), texture -> {
						if (texture != null) {
							builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD).skullTexture(texture));
						} else {
							builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD));
						}
					});
				} else {
					builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD).skullOwner(player));
				}
			} else {
				try {
					final UUID ownerUuid = UUID.fromString(owner);
					getHeadTexture(ownerUuid, texture -> {
						if (texture != null) {
							builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD).skullTexture(texture));
						} else {
							builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD));
						}
					});
				} catch (final IllegalArgumentException e) {
					// Invalid UUID, parse as texture
					builderConsumer.accept(new NbtItemBuilder(Material.PLAYER_HEAD).skullTexture(owner));
				}
			}
			return;
		}

		if (materialString.startsWith("hdb")) {
			final String id = materialString.substring(4);
			builderConsumer.accept(HDBHandler.getBuilder(id));
			return;
		}


		final String[] materialsToTry = materialString.split("\\|");
		Material material = null;
		for (final String materialString2 : materialsToTry) {
			try {
				material = Material.valueOf(materialString2);
				break;
			} catch (final IllegalArgumentException ignored) {
			}
		}

		if (material == null) {
			player.sendMessage("Invalid item name '" + materialString + "'");
			player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Item-names");
			return;
		}

		if (material != Material.AIR) {
			builderConsumer.accept(new NbtItemBuilder(material));
		}
	}

    public static void getHeadTexture(final UUID uuid, Consumer<String> textureConsumer) {
    	if (HEAD_TEXTURE_CACHE.containsKey(uuid)) {
    		textureConsumer.accept(HEAD_TEXTURE_CACHE.get(uuid));
    	}

		Main.getPlugin().getLogger().info("Getting texture value for " + uuid + " from Mojang API");
		Bukkit.getScheduler().runTaskAsynchronously(getPlugin(), () -> {
			try {

				final HttpURLConnection connection = (HttpURLConnection) new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid.toString()).openConnection();
				try (final Reader reader = new InputStreamReader(connection.getInputStream())) {
					final JsonObject jsonResponse = (JsonObject) JsonParser.parseReader(reader);
					final String texture = jsonResponse.get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();
					HEAD_TEXTURE_CACHE.put(uuid, texture);
					Main.getPlugin().getLogger().info("Got " + texture);
					Bukkit.getScheduler().runTask(getPlugin(), () -> textureConsumer.accept(texture));
				}
			} catch (final IOException | IllegalArgumentException | NullPointerException | ClassCastException | IllegalStateException | IndexOutOfBoundsException e) {
				Main.getPlugin().getLogger().warning("Failed to get base64 texture value for " + uuid + ". Is the UUID valid? Error details: " + e.getClass().getSimpleName() + " " + e.getMessage());
			}
		});
    }

}

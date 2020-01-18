package xyz.derkades.serverselectorx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import de.tr7zw.changeme.nbtapi.NBTItem;
import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.ItemBuilder;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil;
import xyz.derkades.derkutils.bukkit.PlaceholderUtil.Placeholder;
import xyz.derkades.serverselectorx.configuration.ConfigSync;
import xyz.derkades.serverselectorx.configuration.ConfigurationManager;
import xyz.derkades.serverselectorx.effects.EffectsOnJoin;
import xyz.derkades.serverselectorx.placeholders.PapiExpansionRegistrar;
import xyz.derkades.serverselectorx.placeholders.Server;

public class Main extends JavaPlugin {

	private static final int FREEZE_STEP_MS = 50;
	private static final int FREEZE_MAX_MS = 5000;

	// When set to true, lag-related debug information is printed to the console. This boolean
	// is enabled using /ssx lagdebug
	static boolean LAG_DEBUG = false;

	private static ConfigurationManager configurationManager;
	private static ConfigSync configSync;

	private static Main plugin;

	public static WebServer server;

	public static Main getPlugin(){
		return plugin;
	}

	@Override
	public void onEnable(){
		plugin = this;

		configurationManager = new ConfigurationManager();
		try {
			configurationManager.reload();
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		this.getCommand("serverselectorx").setExecutor(new ServerSelectorXCommand());

		//Register custom selector commands
		Commands.registerCustomCommands();

		// Disable annoying jetty warnings
		if (!configurationManager.api.getBoolean("jetty-debug", false)){
			//System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.StdErrLog");
			System.setProperty("org.eclipse.jetty.LEVEL", "OFF");
		}

		server = new WebServer();
		server.start();

		new EffectsOnJoin();
		configSync = new ConfigSync();
		new Stats();
		new ItemMoveDropCancelListener();

		Bukkit.getPluginManager().registerEvents(new ItemClickListener(), this);
		Bukkit.getPluginManager().registerEvents(new ItemGiveListener(), this);
		Bukkit.getPluginManager().registerEvents(new BetaMessageJoinListener(), this);

		if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
			PapiExpansionRegistrar.register();
		}
	}

	@Override
	public void onDisable() {
		if (server != null) {
			server.stop();

			// Freeze bukkit thread to give the server time to stop
			int safetyLimit = 0;
			final int max = FREEZE_MAX_MS / FREEZE_STEP_MS;
			while(!server.isStopped()) {

				// Don't freeze for longer than 5 seconds
				if (safetyLimit > max) {
					this.getLogger().severe("Webserver was still running after waiting for 5 seconds.");
					this.getLogger().severe("Giving up, crashing the server is worse than breaking the menu.");
					break;
				}

				safetyLimit++;

				try {
					Thread.sleep(FREEZE_STEP_MS);
				} catch (final InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Sleep a little longer to give the internal jetty stuff time to shut down
			// otherwise I still got a NoClassDefFoundError
			try {
				Thread.sleep(100);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public static ConfigurationManager getConfigurationManager() {
		return configurationManager;
	}

	static ConfigSync getConfigSync() {
		return configSync;
	}

	public static void teleportPlayerToServer(final Player player, final String server){
		if (Cooldown.getCooldown("servertp" + player.getName() + server) > 0) {
			return;
		}

		Cooldown.addCooldown("servertp" + player.getName() + server, 1000);

		// Send message to bungeecord
		try (
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				DataOutputStream dos = new DataOutputStream(baos)
			){

	        dos.writeUTF("Connect");
	        dos.writeUTF(server);
	        player.sendPluginMessage(getPlugin(), "BungeeCord", baos.toByteArray());
		} catch (final IOException e){
			e.printStackTrace();
		}
	}

	public static int getGlobalPlayerCount() {
		int online = 0;
		for (final Server server : Server.getServers()) {
			if (server.isOnline()) {
				online += server.getOnlinePlayers();
			}
		}

		return online;
	}

    public static ItemBuilder getItemBuilderFromItemSection(final Player player, final ConfigurationSection section) {
    	final String materialString = section.getString("material");
    	final ItemBuilder builder = getItemBuilderFromMaterialString(player, materialString);

    	if (section.contains("title")) {
	    	String title = "&r" + section.getString("title");
	    	title = title.replace("{player}", player.getName());
	    	title = PlaceholderUtil.parsePapiPlaceholders(player, title);
	    	builder.coloredName(title);
    	} else {
    		builder.name(" ");
    	}

		if (section.contains("lore")) {
			List<String> lore = section.getStringList("lore");
			lore = lore.stream().map((s) -> "&r" + s).collect(Collectors.toList());
			final Placeholder playerPlaceholder = new Placeholder("{player}", player.getName());
			lore = PlaceholderUtil.parsePapiPlaceholders(player, lore, playerPlaceholder);
			builder.coloredLore(lore);
		}

		if (section.getBoolean("enchanted")) {
			builder.unsafeEnchant(Enchantment.DURABILITY, 1);
		}

		if (section.getBoolean("hide-flags")) {
			builder.hideFlags(63);
		}

		if (section.isInt("amount")) {
			builder.amount(section.getInt("amount"));
		}

		if (section.isInt("data")) {
			final int data = section.getInt("data");
			builder.damage(data);
		}

		if (section.isConfigurationSection("nbt")) {
			final NBTItem nbt = new NBTItem(builder.create());
			final ConfigurationSection nbtSection = section.getConfigurationSection("nbt");
			for (final String key : section.getKeys(false)) {
				if (nbtSection.isBoolean(key)) {
					nbt.setBoolean(key, nbtSection.getBoolean(key));
				} else if (nbtSection.isString(key)) {
					nbt.setString(key, nbtSection.getString(key));
				} else if (nbtSection.isInt(key)) {
					nbt.setInteger(key, nbtSection.getInt(key));
				} else {
					player.sendMessage("Unsupported NBT option '" + key + "'");
				}
			}
			return new ItemBuilder(nbt.getItem());
		}

		return builder;
    }

    public static ItemBuilder getItemBuilderFromMaterialString(final Player player, final String materialString) {
		ItemBuilder builder;

		if (materialString.startsWith("head:")) {
			final String owner = materialString.split(":")[1];
			if (owner.equals("auto")) {
				builder = new ItemBuilder(player.getName());
			} else {
				if (owner.length() > 16) {
					// parse as texture
					builder = new ItemBuilder(Material.SKULL_ITEM).damage(3).skullTexture(owner);
				} else {
					// parse as player name
					builder = new ItemBuilder(owner);
				}
			}
		} else {
			Material material;
			try {
				material = Material.valueOf(materialString);
			} catch (final IllegalArgumentException e) {
				player.sendMessage("Invalid item name '" + materialString + "'");
				return new ItemBuilder(Material.COBBLESTONE);
			}

			builder = new ItemBuilder(material);
		}

		return builder;
    }

}

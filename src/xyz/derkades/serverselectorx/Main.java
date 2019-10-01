package xyz.derkades.serverselectorx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import xyz.derkades.derkutils.Cooldown;
import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.serverselectorx.placeholders.Placeholders;
import xyz.derkades.serverselectorx.placeholders.PlaceholdersDisabled;
import xyz.derkades.serverselectorx.placeholders.PlaceholdersEnabled;

public class Main extends JavaPlugin {

	public static Placeholders PLACEHOLDER_API;

	public static Map<String, Map<String, Object>> SERVER_PLACEHOLDERS = new HashMap<>();

	private static ConfigurationManager configurationManager;

	private static JavaPlugin plugin;

	public static JavaPlugin getPlugin(){
		return plugin;
	}

	@Override
	public void onEnable(){
		plugin = this;

		configurationManager = new ConfigurationManager();
		configurationManager.reload();

		//Register listeners
		Bukkit.getPluginManager().registerEvents(new SelectorOpenListener(), this);
		Bukkit.getPluginManager().registerEvents(new OnJoinListener(), this);
		Bukkit.getPluginManager().registerEvents(new ItemMoveDropCancelListener(), this);

		final List<String> offHandVersions = Arrays.asList("1.9", "1.10", "1.11", "1.12", "1.13", "1.14", "1.15");
		for (final String version : offHandVersions) {
			if (Bukkit.getBukkitVersion().contains(version)) {
				Bukkit.getPluginManager().registerEvents(new OffHandMoveCancel(), this);
			}
		}

		//Register messaging channels
		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		new PingServersBackground().start();

		//Register command
		this.getCommand("serverselectorx").setExecutor(new ReloadCommand());

		//Start bStats
		Stats.initialize();

		//Register custom selector commands
		this.registerCommands();

		//Check if PlaceHolderAPI is installed
		if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")){
			Main.PLACEHOLDER_API = new PlaceholdersEnabled();
			this.getLogger().log(Level.INFO, "PlaceholderAPI is found. Placeholders will work!");
		} else {
			Main.PLACEHOLDER_API = new PlaceholdersDisabled();
			this.getLogger().log(Level.INFO, "PlaceholderAPI is not installed. The plugin will still work.");
		}

		this.getLogger().info("Thank you for using ServerSelectorX. If you enjoy using this plugin, please consider buying the premium version. It has more features and placeholders update instantly. https://github.com/ServerSelectorX/ServerSelectorX/wiki/Premium");
	}

	/**
	 * Registers all custom commands by going through all menu files and adding commands
	 */
	private void registerCommands(){
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			final CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

			for (final FileConfiguration config : configurationManager.getAll()){
				final String commandName = config.getString("command");

				if (commandName == null || commandName.equalsIgnoreCase("none")) {
					continue;
				}

				commandMap.register("ssx-custom", new Command(commandName){

					@Override
					public boolean execute(final CommandSender sender, final String label, final String[] args) {
						if (sender instanceof Player){
							final Player player = (Player) sender;
							Main.openSelector(player, config);
						}
						return true;
					}

				});

			}
		} catch (NoSuchFieldException | IllegalAccessException e) {
			e.printStackTrace();
		}
	}

	public static ConfigurationManager getConfigurationManager() {
		return configurationManager;
	}

	public static void openSelector(final Player player, final FileConfiguration config) {
		final long cooldown = Cooldown.getCooldown(config.getName() + player.getName());
		if (cooldown > 900) {
			String cooldownMessage = Main.getPlugin().getConfig().getString("cooldown-message");
			if (cooldownMessage != null) {
				cooldownMessage = cooldownMessage.replace("{x}", String.valueOf((cooldown / 1000) + 1));
				cooldownMessage = Colors.parseColors(cooldownMessage);
				player.sendMessage(cooldownMessage);
			}

			return;
		}

		final long cooldownDuration = Main.getPlugin().getConfig().getLong("selector-open-cooldown", 0);
		if (cooldownDuration >= 1000) {
			Cooldown.addCooldown(config.getName() + player.getName(), cooldownDuration);
		}

		new SelectorMenu(player, config).open();
	}

	public static void teleportPlayerToServer(final Player player, final String server){
		if (Cooldown.getCooldown("servertp" + player.getName() + server) > 0)
			return;

		Cooldown.addCooldown("servertp" + player.getName() + server, 1000);

		final String message = Main.getPlugin().getConfig().getString("server-teleport-message");
		if (message != null) {
			player.sendMessage(Colors.parseColors(message.replace("{x}", server)));
		}

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

}

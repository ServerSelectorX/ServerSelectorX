package xyz.derkades.serverselectorx;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Field;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

import xyz.derkades.derkutils.bukkit.Colors;
import xyz.derkades.derkutils.bukkit.ItemBuilder;

public class Main extends JavaPlugin {

	private static ConfigurationManager configurationManager;

	private static JavaPlugin plugin;

	public static JavaPlugin getPlugin(){
		return plugin;
	}

	public static BukkitTask pingTask;

	@Override
	public void onEnable(){
		plugin = this;

		configurationManager = new ConfigurationManager();
		configurationManager.reload();

		Bukkit.getPluginManager().registerEvents(new SelectorOpenListener(), this);
		Bukkit.getPluginManager().registerEvents(new OnJoinListener(), this);
		Bukkit.getPluginManager().registerEvents(new ItemMoveDropCancelListener(), this);
		try {
			PlayerSwapHandItemsEvent.class.getName();
			Bukkit.getPluginManager().registerEvents(new OffhandMoveListener(), this);
		} catch (final NoClassDefFoundError e) {
			getLogger().info("Skipping offhand listener (this is fine on <1.9)");
		}

		this.getServer().getMessenger().registerOutgoingPluginChannel(this, "BungeeCord");

		PingServersBackground.generatePingJobs();
		pingTask = Bukkit.getScheduler().runTaskLaterAsynchronously(this, new PingServersBackground(), 40);

		this.getCommand("serverselectorx").setExecutor(new ReloadCommand());

		Stats.initialize();

		this.registerCommands();

		this.getLogger().info("Thank you for using ServerSelectorX. If you enjoy using this plugin, consider buying the premium version for more features: https://github.com/ServerSelectorX/ServerSelectorX/wiki/Premium");
	}

	@Override
	public void onDisable() {
		if (pingTask != null) {
			// Unfortunately not possible to wait until ping task is cancelled,
			// isCancelled() is missing in 1.8
			pingTask.cancel();

			try {
				Thread.sleep(200);
			} catch (final InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Registers all custom commands by going through all menu files and adding commands
	 */
	private void registerCommands(){
		try {
			final Field bukkitCommandMap = Bukkit.getServer().getClass().getDeclaredField("commandMap");

			bukkitCommandMap.setAccessible(true);
			final CommandMap commandMap = (CommandMap) bukkitCommandMap.get(Bukkit.getServer());

			for (final String configName : configurationManager.list()) {
				final FileConfiguration config = configurationManager.getByName(configName);

				final String commandName = config.getString("command");

				if (commandName == null || commandName.equalsIgnoreCase("none")) {
					continue;
				}

				commandMap.register("ssx-custom", new Command(commandName){

					@Override
					public boolean execute(final CommandSender sender, final String label, final String[] args) {
						if (sender instanceof Player){
							final Player player = (Player) sender;
							final FileConfiguration config = configurationManager.getByName(configName);
							if (config == null) {
								player.sendMessage("The menu '" + configName + "' has disappeared? Restart to remove this command.");
							} else {
								new SelectorMenu(player, config);
							}
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

	static ItemBuilder getItemFromMaterialString(final Player player, final String materialString) {
		if (materialString.startsWith("head:")) {
			final String owner = materialString.split(":")[1];
			if (owner.equals("auto")) {
				return new ItemBuilder(player.getName());
			} else {
				return new ItemBuilder(owner);
			}
		} else {
			try {
				final Material material = Material.valueOf(materialString);
				return new ItemBuilder(material);
			} catch (final IllegalArgumentException e) {
				player.sendMessage("Invalid item name " + materialString);
				player.sendMessage("https://github.com/ServerSelectorX/ServerSelectorX/wiki/Item-names");
				return new ItemBuilder(Material.COBBLESTONE);
			}
		}
	}

	public static void teleportPlayerToServer(final Player player, final String server){
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

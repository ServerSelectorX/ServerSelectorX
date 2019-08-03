package xyz.derkades.serverselectorx.effects;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import xyz.derkades.serverselectorx.Main;

public class Effects implements Listener {

	public static final List<UUID> SPEED = new ArrayList<>();
	public static final List<UUID> INVIS = new ArrayList<>();
	
	public Effects() {
		Bukkit.getPluginManager().registerEvents(this, Main.getPlugin());
		new EffectsTimer();
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onJoin(final PlayerJoinEvent event) {
		final UUID uuid = event.getPlayer().getUniqueId();
		final FileConfiguration config = Main.getConfigurationManager().getGlobalConfig();
		
		if (config.getBoolean("speed-on-join")) {
			Effects.SPEED.add(uuid);
		}
		
		if (config.getBoolean("invis-on-join")) {
			Effects.INVIS.add(uuid);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onQuit(final PlayerQuitEvent event) {
		final UUID uuid = event.getPlayer().getUniqueId();
		SPEED.remove(uuid);
		INVIS.remove(uuid);
	}
	
}

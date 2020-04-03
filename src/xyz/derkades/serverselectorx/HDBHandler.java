package xyz.derkades.serverselectorx;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import me.arcaniax.hdb.api.HeadDatabaseAPI;
import xyz.derkades.derkutils.bukkit.ItemBuilder;

/*
 * needs to be in a separate class to avoid classdefnotfound errors when hdb is not installed
 */
public class HDBHandler {
	
	static ItemBuilder getBuilder(final String id) {
		final HeadDatabaseAPI api = new HeadDatabaseAPI();
		try {
			final ItemStack item = api.getItemHead(id);
			return new ItemBuilder(item);
		} catch (final NullPointerException e) {
			Main.getPlugin().getLogger().warning("Couldn't load head from head database, using barrier for item instead.");
			return new ItemBuilder(Material.BARRIER);
		}
	}

}

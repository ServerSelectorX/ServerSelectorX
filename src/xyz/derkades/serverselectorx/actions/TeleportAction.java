package xyz.derkades.serverselectorx.actions;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class TeleportAction extends Action {

	public TeleportAction() {
		super("teleport", true);
	}

	@Override
	public boolean apply(final Player player, final String value) {
		final String[] split = value.split("!");
		if (split.length != 6) {
			player.sendMessage("Invalid teleport format. Correct format: 'worldname!x!y!z!yaw!pitch' for example 'world!0.5!65!0.5!90!0'");
			return true;
		}
		
		final World world = Bukkit.getWorld(split[0]);
		final double x = Double.parseDouble(split[1]);
		final double y = Double.parseDouble(split[2]);
		final double z = Double.parseDouble(split[3]);
		final float yaw = Float.parseFloat(split[4]);
		final float pitch = Float.parseFloat(split[5]);
		
		if (world == null) {
			player.sendMessage("A world with the name '" + split[0] + "' does not exist.");
			return true;
		}
		
		final Location location = new Location(world, x, y, z, yaw, pitch);
		player.teleport(location);
		return false;
	}

}

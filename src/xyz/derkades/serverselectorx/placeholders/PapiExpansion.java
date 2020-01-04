package xyz.derkades.serverselectorx.placeholders;

import org.bukkit.entity.Player;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import xyz.derkades.serverselectorx.Main;

public class PapiExpansion extends PlaceholderExpansion {

	@Override
	public String onPlaceholderRequest(final Player player, final String identifier) {
		if (identifier.startsWith("server_")) {
			String serverName = "";
			for (final char c : identifier.toCharArray()) {
				if (c == '_') {
					break;
				}
				serverName += c;
			}

			// Remove 'server_' (length 7), the server name and the underscore after it
			final String placeholderName = identifier.substring(7 + serverName.length() + 1);

			final Server server = Server.getServer(serverName);

			if (placeholderName.equals("status")) {
				return server.isOnline() ? "online" : "offline";
			} else {
				final Placeholder placeholder = server.getPlaceholder(placeholderName);
				if (placeholder instanceof PlayerPlaceholder) {
					return ((PlayerPlaceholder) placeholder).getValue(player);
				} else {
					return ((GlobalPlaceholder) placeholder).getValue();
				}
			}
		}

		return null;
	}

	@Override
	public String getAuthor() {
		return String.join(", ", Main.getPlugin().getDescription().getAuthors());
	}

	@Override
	public String getIdentifier() {
		return "serverselectorx";
	}

	@Override
	public String getVersion() {
		return Main.getPlugin().getDescription().getVersion();
	}

    @Override
    public boolean persist(){
        return true;
    }

    @Override
    public boolean canRegister(){
        return true;
    }

}
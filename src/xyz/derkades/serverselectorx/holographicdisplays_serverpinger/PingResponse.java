/*
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package xyz.derkades.serverselectorx.holographicdisplays_serverpinger;

import java.util.logging.Level;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import xyz.derkades.serverselectorx.Main;

public class PingResponse
{
	private boolean isOnline;
    private String motd;
    private int onlinePlayers;
    private int maxPlayers;
    
    public PingResponse(final boolean isOnline, final String motd, final int onlinePlayers, final int maxPlayers) {
		this.isOnline = isOnline;
		this.motd = motd;
		this.onlinePlayers = onlinePlayers;
		this.maxPlayers = maxPlayers;
	}

	public PingResponse(final String jsonString, final String address, final int port) {
		
		if (jsonString == null || jsonString.isEmpty()) {
    		this.motd = "Invalid ping response";
    		Main.getPlugin().getLogger().log(Level.WARNING, "Received empty Json response from IP \"" + address.toString() + "\"!");
    		return;
    	}
		
		final Object jsonObject = JSONValue.parse(jsonString);
		
    	if (!(jsonObject instanceof JSONObject)) {
    		this.motd = "Invalid ping response";
    		Main.getPlugin().getLogger().log(Level.WARNING, "Received invalid Json response from IP \"" + address.toString() + "\": " + jsonString);
    		return;
    	}
    	
    	final JSONObject json = (JSONObject) jsonObject;
    	this.isOnline = true;
    	
    	final Object descriptionObject = json.get("description");
    	
    	if (descriptionObject != null) {
    		if (descriptionObject instanceof JSONObject) {
    			final Object text = ((JSONObject) descriptionObject).get("text");
    			if (text != null) {
    				this.motd = text.toString();
    			} else {
    				this.motd = "Invalid ping response (text not found)";
    			}
    		} else {
    			this.motd = descriptionObject.toString();
    		}
    	} else {
    		this.motd = "Invalid ping response (description not found)";
    		Main.getPlugin().getLogger().log(Level.WARNING, "Received invalid Json response from IP \"" + address.toString() + "\": " + jsonString);
    	}
        
        final Object playersObject = json.get("players");

		if (playersObject instanceof JSONObject) {
			final JSONObject playersJson = (JSONObject) playersObject;
			
			final Object onlineObject = playersJson.get("online");
			if (onlineObject instanceof Number) {
				this.onlinePlayers = ((Number) onlineObject).intValue();
			}
			
			final Object maxObject = playersJson.get("max");
			if (maxObject instanceof Number) {
				this.maxPlayers = ((Number) maxObject).intValue();
			}
        }
    }

	public boolean isOnline() {
		return this.isOnline;
	}

	public String getMotd() {
		return this.motd;
	}

	public int getOnlinePlayers() {
		return this.onlinePlayers;
	}

	public int getMaxPlayers() {
		return this.maxPlayers;
	}

}
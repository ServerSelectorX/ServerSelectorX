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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;

/*
 * taken from https://github.com/filoghost/HolographicDisplays/tree/master/Plugin/src/main/java/com/gmail/filoghost/holographicdisplays/bridge/bungeecord/serverpinger
 * made some minor modifications to get it to compile
 */
public class ServerPinger {

	public static PingResponse fetchData(final String address, final int port, final int timeout) throws SocketTimeoutException, UnknownHostException, IOException, Exception {
		
		Socket socket = null;
		DataOutputStream dataOut = null;
		DataInputStream dataIn = null;
		
		try {
			socket = new Socket(address, port);
			socket.setSoTimeout(timeout);
			dataOut = new DataOutputStream(socket.getOutputStream());
			dataIn = new DataInputStream(socket.getInputStream());
			final ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
			final DataOutputStream handshake = new DataOutputStream(byteOut);
			handshake.write(0);
			PacketUtils.writeVarInt(handshake, 4);
			PacketUtils.writeString(handshake, address, PacketUtils.UTF8);
			handshake.writeShort(port);
			PacketUtils.writeVarInt(handshake, 1);
			byte[] bytes = byteOut.toByteArray();
			PacketUtils.writeVarInt(dataOut, bytes.length);
			dataOut.write(bytes);
			bytes = new byte[] { 0 };
			PacketUtils.writeVarInt(dataOut, bytes.length);
			dataOut.write(bytes);
			PacketUtils.readVarInt(dataIn);
			PacketUtils.readVarInt(dataIn);
			final byte[] responseData = new byte[PacketUtils.readVarInt(dataIn)];
			dataIn.readFully(responseData);
			final String jsonString = new String(responseData, PacketUtils.UTF8);
			return new PingResponse(jsonString, address, port);
		}
		finally {
			PacketUtils.closeQuietly(dataOut);
			PacketUtils.closeQuietly(dataIn);
			PacketUtils.closeQuietly(socket);
		}
	}

}
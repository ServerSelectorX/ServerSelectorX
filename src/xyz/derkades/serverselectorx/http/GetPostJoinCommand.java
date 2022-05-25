package xyz.derkades.serverselectorx.http;

import com.google.common.base.Preconditions;
import org.glassfish.grizzly.http.Method;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class GetPostJoinCommand extends HttpHandler {

	private static final Map<String, String> COMMAND_BY_UUID_STRING = new HashMap<>();

	@Override
	public void service(Request request, Response response) throws Exception {
		Preconditions.checkArgument(request.getMethod() == Method.GET, "Must use GET method");
		request.getParameters().setEncoding(StandardCharsets.UTF_8);

		String uuid = Objects.requireNonNull(request.getParameter("uuid"), "Missing UUID");

		String command = COMMAND_BY_UUID_STRING.get(uuid);

		if (command == null) {
			response.setStatus(HttpStatus.NO_CONTENT_204);
		} else {
			response.getWriter().write(command);
		}
	}

	public static void setCommand(UUID uuid, String command) {
		COMMAND_BY_UUID_STRING.put(uuid.toString(), command);
	}

}

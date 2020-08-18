package xyz.derkades.serverselectorx.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.bukkit.configuration.file.FileConfiguration;

import xyz.derkades.serverselectorx.Main;

public class GetFile extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final FileConfiguration api = Main.getConfigurationManager().api;

		if (!api.getBoolean("files-api")) {
			response.getWriter().println("Files API disabled");
			response.setStatus(HttpServletResponse.SC_FORBIDDEN);
			return;
		}

		final String fileName = request.getParameter("file");
		final File file = new File(fileName);
		try (InputStream input = new FileInputStream(file)) {
			IOUtils.copy(input, response.getOutputStream());
		}
	}

}

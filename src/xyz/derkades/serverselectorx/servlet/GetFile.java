package xyz.derkades.serverselectorx.servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;

import xyz.derkades.serverselectorx.Main;

public class GetFile extends HttpServlet {

	private static final long serialVersionUID = 1L;

	@Override
	protected void doGet(final HttpServletRequest request, final HttpServletResponse response) throws IOException {
		final Logger logger = Main.getPlugin().getLogger();
		final String fileName = request.getParameter("file");

		// Do not allow going outside of the plugin directory for security reasons
		if (Main.getConfigurationManager().api.getBoolean("block-outside-directory", true) && fileName.contains("..")) {
			logger.warning("Received request with dangerous filename from " + request.getRemoteAddr());
			logger.warning("File name: " + fileName);
			logger.warning("The request has been blocked, no need to panic. Maybe do consider looking into where the request came from?");
			response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
			return;
		}

		final File file = new File(fileName);
		try (InputStream input = new FileInputStream(file)) {
			IOUtils.copy(input, response.getOutputStream());
		}
	}

}

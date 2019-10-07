package xyz.derkades.serverselectorx.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {

	public static void zipDirectory(final File directory, final File out) {
		try (
				FileOutputStream fos = new FileOutputStream(out);
				ZipOutputStream zos = new ZipOutputStream(fos)
			){
			for (final File file : directory.listFiles()) {
				if (file.isDirectory()) {
					continue;
				}

				final ZipEntry ze = new ZipEntry(file.getAbsolutePath());
				zos.putNextEntry(ze);
				final FileInputStream fis = new FileInputStream(out);
				final byte[] buffer = new byte[1024];
				int len;
				while ((len = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
				zos.closeEntry();
				fis.close();
			}
		} catch (final IOException e) {
			throw new RuntimeException(e);
		}
	}

}

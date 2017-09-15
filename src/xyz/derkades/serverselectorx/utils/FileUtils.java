package xyz.derkades.serverselectorx.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FileUtils {
	
	public static void zipDirectory(File directory, File out) {
		try (
				FileOutputStream fos = new FileOutputStream(out);
				ZipOutputStream zos = new ZipOutputStream(fos)
			){
			for (File file : directory.listFiles()) {
				if (file.isDirectory()) {
					continue;
				}
				
				ZipEntry ze = new ZipEntry(file.getAbsolutePath());
				zos.putNextEntry(ze);
				FileInputStream fis = new FileInputStream(out);
				byte[] buffer = new byte[1024];
				int len;
				while ((len = fis.read(buffer)) > 0) {
					zos.write(buffer, 0, len);
				}
				zos.closeEntry();
				fis.close();
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

}

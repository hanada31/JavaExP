package com.iscas.crashtracker.client.manifest;

import com.iscas.crashtracker.base.Global;
import com.iscas.crashtracker.utils.FileUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;

/**
 * output analyze result
 * 
 * @author 79940
 *
 */
public class ManifestClientOutput {
	/**
	 * writeManifest
	 * 
	 * @param dir
	 * @param file
	 */
	public static void writeManifest(String dir, String file) {
		FileUtils.createFolder(dir);
		FileUtils.createFile(dir + file);
		File f = new File(dir + file);
		BufferedWriter writer = null;
		try {
			writer = new BufferedWriter(new FileWriter(f));
			writer.write(Global.v().getAppModel().getManifestString());
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}

}

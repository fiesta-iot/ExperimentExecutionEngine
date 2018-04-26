/*
===========================================================
Experiment Execution Engine
Copyright (C) 2018  Authors: Rachit Agarwal.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <https://www.gnu.org/licenses/>.

contact: rachit.agarwal@inria.fr 
===========================================================
*/
package eu.fiesta_iot.platform.eee.scheduler.impl;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Instant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sender {

	static Logger log = LoggerFactory.getLogger(Sender.class);

	public static String saveResultsInAFile(String data, String urlPath, String name) {

		String fileName = Constants.getFilePath() + name.replace("-", "")
				+ urlPath.replace(":", "").replace("/", "_") + Instant.now().toEpochMilli();
		log.debug("fileName=" + fileName);
		File file = new File(fileName);

		if (!file.exists()){
			try {
				file.createNewFile();
				log.debug("fileName created");
			} catch (IOException e1) {
				log.error("error while creating a file", e1);// e1.printStackTrace();
			}
		}
		FileWriter out;
		try {
			out = new FileWriter(file);
			try {
				out.write(data);
				log.debug("file written");
			} finally {
				try {
					out.close();
					log.debug("file writter closed");
				} catch (IOException closeException) {
					log.error("", closeException);// closeException.printStackTrace();
				}
			}
		} catch (IOException e) {
			log.error("IOExecption while saving the file", e);// e.printStackTrace();
		}
		log.debug("file created");

		return fileName;
	}

	public static boolean sendResultsToExperimenter(String filename, String urlPath) {

		File textFile = new File(filename);
		// Just generate some unique random value.
		String boundary = "--" + Long.toHexString(System.currentTimeMillis()); 
		String charset = "UTF-8";
		// ping the url
		try {
			URL url = new URL(urlPath);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			// conn.setDoInput(true);
			// conn.setUseCaches(false);
			conn.setRequestMethod(Constants.postMethod);
			conn.setRequestProperty("connection", "keep-alive");

			// conn.setRequestProperty("Cache-Control", "no-cache");
			conn.setRequestProperty(Constants.contentType, "multipart/form-data; boundary=" + boundary);

			OutputStream output = conn.getOutputStream();
			PrintWriter writer = new PrintWriter(new OutputStreamWriter(output, charset), true);

			output.write(("\r\n\r\n--" + boundary + "\r\n").getBytes());
			output.write(("Content-Disposition: form-data; name=\"file1\"; filename=\"" + textFile.getName() + "\"\r\n")
					.getBytes());
			output.write(("Content-Type: text/plain\r\n").getBytes());
			output.write(("\r\n").getBytes());
			Files.copy(textFile.toPath(), output);

			output.write(("\r\n").getBytes());
			output.write(("--" + boundary + "--\r\n").getBytes());
			output.flush();
			writer.close();

			int responseMC = conn.getResponseCode();
			if (responseMC == HttpURLConnection.HTTP_OK) {
				Files.delete(textFile.toPath());
				return true;
			} 
			else if (responseMC == HttpURLConnection.HTTP_GATEWAY_TIMEOUT){
				log.error("File cannot be sent to " + urlPath);
				return false;
			}else {
				log.error("File cannot be sent to " + urlPath);
				return false;
			}
		}catch (ConnectException e) {
			log.error("File cannot be sent to " + urlPath+ " due to timeout. Thus storing it in the ERS. The complete error is:",e);
			return false;
		}
		catch (SocketTimeoutException e) {
			log.error("File cannot be sent to " + urlPath+ " due to timeout. Thus storing it in the ERS. The complete error is:",e);
			return false;
		} 
		catch (IOException e) {
			log.error("File cannot be sent to " + urlPath+ ". Thus storing it in the ERS. The complete error is:",e);
			return false;
		}
	}
	
}

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

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Saver {
	final static Logger logger = LoggerFactory.getLogger(Saver.class);

	public static boolean saveResultOnserver(MetaCloudReturn metaCloudReturn, String jobID, String url,
			boolean reportIfEmpty, String femoID, String userID, String token) {

		logger.debug("reportIfEmpty=" + reportIfEmpty);
		logger.debug("metaCloudReturn=" + metaCloudReturn.getNumber());

		if (reportIfEmpty) {
			return onServer(metaCloudReturn, jobID, url, femoID, userID, token);
		} else if (metaCloudReturn.getNumber() == 200) {
			return onServer(metaCloudReturn, jobID, url, femoID, userID, token);
		} else
			return true;
	}

	public static boolean onServer(MetaCloudReturn metaCloudReturn, String jobID, String url, String femoID,
			String userID, String token) {
		String fileName = Sender.saveResultsInAFile(metaCloudReturn.getData(), url, jobID);
		if (!Sender.sendResultsToExperimenter(fileName, url)) {
			if (!sendResultsToERS(metaCloudReturn.getData(), jobID, femoID, userID, token)) {
				logger.info("not able to save result and send resultset. userID:" + userID + " femoID:" + femoID);
				return false;
			} else
				return true;
		}
		return true;
	}

	public static boolean sendResultsToERS(String data, String jobID, String femoID, String userID, String token) {
		try {
			URL url = new URL(Constants.getERSStoreAPI());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod(Constants.postMethod);
			conn.setRequestProperty("userID", userID);
			conn.setRequestProperty("femoID", femoID);
			conn.setRequestProperty("jobID", jobID);
			conn.setRequestProperty(Constants.contentType, Constants.jsonContentType);
			conn.setRequestProperty(Constants.tokenName, token);
			JSONObject metadata = new JSONObject();
			logger.debug("data to be stored" + data);
			metadata.put("result", data);
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			logger.debug("meta data data to be stored" + metadata.toString());
			wr.write(metadata.toString());
			wr.flush();
			wr.close();
			int responseMC = conn.getResponseCode();

			logger.debug("response code getUserID:" + responseMC);
			if (responseMC == HttpURLConnection.HTTP_OK) {
				return true;
			}
			return false;
		} catch (IOException e) {
			logger.error("IOException:", e);// e.printStackTrace();
		} catch (Exception e) {
			logger.error("Exception:", e);// e.printStackTrace();
		}
		return false;
	}
}

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
import java.util.Date;

import org.quartz.JobExecutionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.eee.scheduler.db.CallLogModel;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.CallLogStorage;

public class MetaCloud {
	static Logger log = LoggerFactory.getLogger(MetaCloud.class);

	public static boolean callMetaCloud(JobExecutionContext context, Date now, String token, String query,
			String fileType, boolean reportIfEmpty, String url, String femoID, String userID, CallLogModel logging) {
		MetaCloudReturn metaCloudReturn = sendQueryToMetaCloud(query, token, fileType);

		if (metaCloudReturn.getNumber() == 200) {
			logging.setDataConsumed(Integer.toString(metaCloudReturn.getData().length()));
		} else
			logging.setDataConsumed("0");
		if (Saver.saveResultOnserver(metaCloudReturn, context.getJobDetail().getKey().getName(), url, reportIfEmpty,
				femoID, userID, token)) {
			Date future = new Date();
			long diff = future.getTime() - now.getTime();
			logging.setTimeConsumed(diff);

			new CallLogStorage().save(logging);
			return true;
		} else
			return false;
	}

	public static boolean callMetaCloud(String femoID, String token, CallLogModel logging, String jobID, String userID,
			String query, String urlPath, String returnType, boolean reportIfEmpty, Date now) {
		MetaCloudReturn metaCloudReturn = sendQueryToMetaCloud(query, token, returnType);

		if (metaCloudReturn.getNumber() == 200) {
			logging.setDataConsumed(Integer.toString(metaCloudReturn.getData().length()));
		} else
			logging.setDataConsumed("0");
		if (Saver.saveResultOnserver(metaCloudReturn, jobID, urlPath, reportIfEmpty, femoID, userID, token)) {
			Date future = new Date();
			long diff = future.getTime() - now.getTime();
			logging.setTimeConsumed(diff);

			new CallLogStorage().save(logging);
			return true;
		} else
			return false;
	}

	public static MetaCloudReturn sendQueryToMetaCloud(String query, String token, String acceptType) {
		log.debug("send Query To MetaCloud:" + query);
		MetaCloudReturn metaCloudReturn = new MetaCloudReturn();
		try {
			URL url = new URL(Constants.getMetaCloudServicesPath());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod(Constants.postMethod);
			conn.setRequestProperty(Constants.contentType, Constants.textContentType);
			conn.setRequestProperty(Constants.acceptsName, acceptType);
			conn.setRequestProperty(Constants.tokenName, token);

			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.write(query);
			wr.flush();
			wr.close();
			int responseMC = conn.getResponseCode();

			if (responseMC == HttpURLConnection.HTTP_OK) {
				
				String resp = conn.getHeaderField(Constants.getMetaCloudResponseHeaderParam());
				log.debug("response is 200 + resp="+resp);
				metaCloudReturn.setNumber(204);
				if (resp != null)
					if (resp.equals(Constants.getMetaCloudResponseHeaderParamValue()))
						metaCloudReturn.setNumber(204);
					if (!resp.equals(Constants.getMetaCloudResponseHeaderParamValue()))
						metaCloudReturn.setNumber(200);
				metaCloudReturn.setData(Commons.getContent(conn.getInputStream()));
				return metaCloudReturn;// getContent(conn.getInputStream());
			} else {
				log.debug("response is not http_OK. it is " + responseMC);
				log.debug("errormessage" + conn.getResponseMessage());
				metaCloudReturn.setNumber(responseMC);
				metaCloudReturn.setData(conn.getResponseMessage());
				return metaCloudReturn;// conn.getResponseMessage();
			}
		} catch (IOException e) {
			log.error("IOException:", e);
		} catch (Exception e) {
			log.error("Exception:", e);
		}
		metaCloudReturn.setNumber(400);
		metaCloudReturn.setData("error");
		return metaCloudReturn;// return "error";
	}
}

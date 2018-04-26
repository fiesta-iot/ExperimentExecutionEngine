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
*/package eu.fiesta_iot.platform.eee.scheduler.impl;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Date;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.eee.scheduler.db.CallLogModel;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.CallLogStorage;

public class KatHelper {
	final static Logger log = LoggerFactory.getLogger(KatHelper.class);

	public static boolean kATFunction(String jobID, Date now, String token, String query, String kATInput,
			String femoID, CallLogModel logging) {
		JSONObject katInputBody = new JSONObject(kATInput);
		
		//log.debug("the query before KAT Insert:" + query);
		katInputBody.append(Constants.kATInputSPARQLquery, query);
		log.debug("the KAT after query Insert:" + katInputBody);
		katInputBody.append(Constants.kATInputSPARQLendpoint, Constants.getMetaCloudServicesPath());

		log.debug("femo ID:" + femoID);
		if (callKAT(token, katInputBody, jobID,femoID)) {
			logging.setDataConsumed("0");

			Date future = new Date();
			long diff = future.getTime() - now.getTime();
			logging.setTimeConsumed(diff);

			new CallLogStorage().save(logging);

			return true;
		}
		return false;
	}

	public static boolean callKAT(String token, JSONObject katInputBody, String jobID, String femoID) {
		try {
			URL url = new URL(Constants.getKATAPI());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod(Constants.postMethod);
			conn.setRequestProperty(Constants.contentType, Constants.jsonContentType);
			conn.setRequestProperty(Constants.tokenName, token);
			String userID=SecurityHelper.getUserID(token);
			
			conn.setRequestProperty("jobId", jobID);
			conn.setRequestProperty("femoId", femoID);
			conn.setRequestProperty("userId", userID);
			
			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			
			log.debug("call kat for executing query:" +katInputBody.toString());
			
			wr.write(katInputBody.toString());
			wr.flush();
			wr.close();
			int responseMC = conn.getResponseCode();
			log.debug("KAT response code :" + responseMC);
			log.debug("KAT response message :" +Commons.getContent(conn.getInputStream()));
			if (responseMC == HttpURLConnection.HTTP_OK)
				return true;
			else
				return false;
		} catch (IOException e) {
			log.error("IOException", e);
		} catch (Exception e) {
			log.error("Exception", e);
		}
		return false;
	}
}

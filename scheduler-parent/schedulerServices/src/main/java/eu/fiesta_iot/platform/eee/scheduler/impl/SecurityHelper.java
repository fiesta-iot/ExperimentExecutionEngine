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

public class SecurityHelper {
	final static Logger logger = LoggerFactory.getLogger(SecurityHelper.class);

	public static String getUserID(String token) {
		String userID = "";

		try {
			URL url = new URL(Constants.getSecurityUserAPI());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod(Constants.postMethod);
			conn.setRequestProperty(Constants.contentType,Constants.jsonContentType);
			conn.setRequestProperty(Constants.tokenName, token);

			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.flush();
			wr.close();
			int responseMC = conn.getResponseCode();
			logger.debug("response code getUserID:"+responseMC);
			if (responseMC == HttpURLConnection.HTTP_OK) {
				String security = Commons.getContent(conn.getInputStream());
				JSONObject jObject = new JSONObject(security);
				if (jObject.has("id")) {
					userID = jObject.getString("id");
				}
			}
		} catch (IOException e) {
			logger.error("IOException in getting UserID:",e);//e.printStackTrace();
		} catch (Exception e) {
			logger.error("Exception:",e);//e.printStackTrace();
		}
		return userID;
	}

	public static String login() {
		String token = "";
		try {
			URL url = new URL(Constants.login());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod(Constants.postMethod);

			conn.setRequestProperty(Constants.contentType,Constants.jsonContentType);
			
			conn.setRequestProperty(Constants.openAMParamUser, Constants.getOpenAMUserName());
			conn.setRequestProperty(Constants.openAMParamPassword, Constants.getOpenAMPassword());

			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.flush();
			wr.close();
			int responseMC = conn.getResponseCode();
			logger.debug("response code login:"+responseMC);
			if (responseMC == HttpURLConnection.HTTP_OK) {
				String security = Commons.getContent(conn.getInputStream());
				JSONObject jObject = new JSONObject(security);
				if (jObject.has("tokenId")) {
					token = jObject.getString("tokenId");
				}
			}
		} catch (IOException e) {
			logger.error("IOException in login",e);//e.printStackTrace();
		} catch (Exception e) {
			logger.error("exception",e);//e.printStackTrace();
		}
		return token;
	}

	public static void logout(String token) {

		try {
			URL url = new URL(Constants.logout());
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();

			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod(Constants.postMethod);

			conn.setRequestProperty(Constants.contentType,Constants.jsonContentType);
			conn.setRequestProperty(Constants.tokenName, token);

			OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
			wr.flush();
			wr.close();
			int responseMC = conn.getResponseCode();
			logger.debug("response code logout:"+responseMC);
			if (responseMC == HttpURLConnection.HTTP_OK) {
				String security = Commons.getContent(conn.getInputStream());
				JSONObject jObject = new JSONObject(security);
				if (jObject.has("result")) {
					logger.debug("logout message=" + jObject.getString("result"));
				}
			}
		} catch (IOException e) {
			logger.error("IOException in logout",e);//e.printStackTrace();
		} catch (Exception e) {
			logger.error("Exception",e);//e.printStackTrace();
		}
		// return token;
	}

	
}

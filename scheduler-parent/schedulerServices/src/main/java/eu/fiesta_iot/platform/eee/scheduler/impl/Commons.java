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
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.eee.scheduler.dbUtils.SubscriptionStorage;
import eu.fiestaiot.commons.fedspec.model.DynamicAttr;
import eu.fiestaiot.commons.fedspec.model.DynamicAttrs;
import eu.fiestaiot.commons.fedspec.model.FISMO;
import eu.fiestaiot.commons.fedspec.model.PredefinedDynamicAttr;

public class Commons {
	static Logger log = LoggerFactory.getLogger(Commons.class);

	public static String processQuerySubscribe(String query, String jobID) throws Exception {
		String latitude = "0";
		String longitude = "0";
		int intervalNowToPast = 0;
		Long fromTime = new Date().getTime();
		Long toTime = new Date().getTime();

		String processedQuery = query;

		if (query.contains(Constants.dynamicQueryChar)) {
			List<String> variables = Arrays.asList(query.replaceAll(Constants.dynamicQueryReplaceString, "")
					.split(Constants.dynamicQuerySplitterString));

			JSONObject jsonObject = new JSONObject();
			try {
				fromTime = SubscriptionStorage.getInstance().getFromTime(jobID);
				toTime = SubscriptionStorage.getInstance().getToTime(jobID);
				intervalNowToPast = SubscriptionStorage.getInstance().getIntervalToPast(jobID);
				longitude = SubscriptionStorage.getInstance().getLongitude(jobID);
				latitude = SubscriptionStorage.getInstance().getLatitude(jobID);
				jsonObject = new JSONObject(SubscriptionStorage.getInstance().getOtherAttributes(jobID));
			} catch (Exception e) {
				log.debug("Attrs not set");
			}
			if (jsonObject.length() != 0) {
				for (String key : JSONObject.getNames(jsonObject)) {
					for (String variable : variables) {
						if (key.equals(variable)) {
							processedQuery = processedQuery.replaceAll(
									Constants.dynamicQueryChar + variable + Constants.dynamicQueryChar,
									jsonObject.get(key).toString());
						}
					}
				}
			}

			processedQuery = replaceAttributes(latitude, longitude, intervalNowToPast, fromTime, toTime,
					processedQuery);
		}

		if (processedQuery.contains(Constants.dynamicQueryChar)) {
			// raise there is exception
			log.error("processedQuery=" + processedQuery);
			throw new Exception("Incorrect Query");
		}

		return processedQuery;
	}

	public static String processQuery(String query, FISMO fismo) throws Exception {
		DynamicAttrs attribute;
		PredefinedDynamicAttr pda;
		List<DynamicAttr> lda = null;
		String latitude = "0";
		String longitude = "0";
		int intervalNowToPast = 0;
		Long fromTime = new Date().getTime();
		Long toTime = new Date().getTime();

		String processedQuery = query;

		if (query.contains(Constants.dynamicQueryChar)) {
			List<String> variables = Arrays.asList(query.replaceAll(Constants.dynamicQueryReplaceString, "")
					.split(Constants.dynamicQuerySplitterString));

			try {
				attribute = fismo.getQueryControl().getDynamicAttrs();
				try {
					pda = attribute.getPredefinedDynamicAttr();
					try {
						latitude = pda.getDynamicGeoLocation().getLatitude();
						longitude = pda.getDynamicGeoLocation().getLongitude();
					} catch (Exception e) {
						log.debug("DynamicGeoLocation not set");
					}
					try {
						intervalNowToPast = pda.getDynamicQueryInterval().getIntervalNowToPast();
					} catch (Exception e) {
						log.debug("intervalNowToPast not set");
					}
					// Clarify the use of from and to time. as cannot see the
					// value here
					try {
						fromTime = pda.getDynamicQueryInterval().getFromDateTime().getTime().getTime();
						toTime = pda.getDynamicQueryInterval().getToDateTime().getTime().getTime();
					} catch (Exception e) {
						log.debug("either from time to time not set");
					}
				} catch (Exception e) {
					log.debug("PredefinedDynamicAttr not set");
				}
				try {
					lda = attribute.getDynamicAttr();
				} catch (Exception e) {
					log.debug("DynamicAttr not set");
				}
			} catch (Exception e) {
				log.debug("DynamicAttrs not set");
			}

			for (int i = 0; i < lda.size(); i++) {
				for (String variable : variables) {
					if (lda.get(i).getName().equals(variable)) {
						processedQuery = processedQuery.replaceAll(
								Constants.dynamicQueryChar + variable + Constants.dynamicQueryChar,
								lda.get(i).getValue());
					}
				}
			}
			processedQuery = replaceAttributes(latitude, longitude, intervalNowToPast, fromTime, toTime,
					processedQuery);
		}

		if (processedQuery.contains(Constants.dynamicQueryChar)) {
			// raise there is exception
			log.error("processedQuery=" + processedQuery);
			throw new Exception("Incorrect Query");
		}

		return processedQuery;
	}

	public static String replaceAttributes(String latitude, String longitude, int intervalNowToPast, Long fromTime,
			Long toTime, String processedQuery) {

		String processedQueryReplacedChars = processedQuery;

		DateFormat formatter = new SimpleDateFormat(Constants.timeFormat);

		if (intervalNowToPast > 0) {// From now to interval in past
			if (intervalNowToPast>Constants.getIntervalNowToPastMax())
				intervalNowToPast=Constants.getIntervalNowToPastMax();
			Date d = new Date();

			processedQueryReplacedChars = processedQueryReplacedChars.replaceAll(Constants.fromStartDate,
					formatter.format(new Date(d.getTime() - intervalNowToPast*1000)));
			processedQueryReplacedChars = processedQueryReplacedChars.replaceAll(Constants.toEndDate,
					formatter.format(d));
		}

		else if (fromTime < toTime) {
			processedQueryReplacedChars = processedQueryReplacedChars.replaceAll(Constants.fromStartDate,
					formatter.format(new Date(fromTime)));
			processedQueryReplacedChars = processedQueryReplacedChars.replaceAll(Constants.toEndDate,
					formatter.format(new Date(toTime)));
		}
		// if (!latitude.equals("0"))
		processedQueryReplacedChars = processedQueryReplacedChars.replaceAll(Constants.geoLat, latitude);
		// if (!longitude.equals("0"))
		processedQueryReplacedChars = processedQueryReplacedChars.replaceAll(Constants.geoLong, longitude);

		return processedQueryReplacedChars;
	}

	public static String getContent(InputStream input) {
		StringBuilder sb = new StringBuilder();
		byte[] b = new byte[1024];
		int readBytes = 0;
		try {
			while ((readBytes = input.read(b)) >= 0) {
				sb.append(new String(b, 0, readBytes, "UTF-8"));
			}
			input.close();
			return sb.toString().trim();
		} catch (IOException e) {
			log.error("IOException:", e);
			if (input != null)
				try {
					input.close();
				} catch (IOException e1) {
					log.error("IOException:", e1);
				}
		}
		return null;
	}

	public static String stripCDATA(String s) {
		log.debug("stripping cdata");
		s = s.trim();
		if (s.startsWith(Constants.cdataPrefix)) {
			s = s.substring(9);
			int i = s.indexOf(Constants.cdataPostfix);

			if (i == -1) {
				return s;
			}

			s = s.substring(0, i);
		}
		log.debug("Stripped cdata");
		return s;
	}
}

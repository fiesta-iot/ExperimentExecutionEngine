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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Constants {
	private static final Object _sync = new Object();

	private static Properties props;

	static Logger log = LoggerFactory.getLogger(Constants.class);
	public static String NoSuchServiceModelObjectID = "{\"response\":\"NoSuchServiceModelObjectID\"}";
	public static String NoSuchServiceModelObjectIDOrNotSubscribed = "{\"response\":\"NoSuchServiceModelObjectIDOrNotSubscribed\"}";
	public static String NoSuchExperimentID = "{\"response\":\"NoSuchExperimentID\"}";
	public static String NoSuchUserID = "{\"response\":\"NoSuchUserID\"}";
	public static String NoSuchJobID = "{\"response\":\"NoSuchJobID\"}";
	public static String ImplementationException = "{\"response\":\"ImplementationException\"}";
	public static String InvalidURL = "{\"response\":\"InvalidUrl\"}";
	public static String InvalidExperimentOutputJson = "{\"response\":\"InvalidExperimentOutputJson\"}";
	public static String AlreadySubscribed = "{\"response\":\"AlreadySubscribed\"}";
	public static String SubscriptionNotFound = "{\"response\":\"SubscriptionNotFound\"}";
	public static String UnParsableDate = "{\"response\":\"UnParseableDate\"}";
	public static String InvalidTimeScheduleStructure = "{\"response\":\"InvalidTimeScheduleStructure\"}";
	public static String SchedulerException = "{\"response\":\"SchedulerException\"}";
	public static String JsonException = "{\"response\":\"JsonException\"}";
	public static String PersistenceException = "{\"response\":\"PersistenceException\"}";
	public static String MetaCloudException = "{\"response\":\"MetaCloudException\"}";
	public static String QueryException = "{\"response\":\"Invalid Query and Parameters\"}";

	public static String quartzPropertyFilePath = "/quartz.properties";
	public static String timeFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
	public static String postMethod = "POST";
	public static String gettMethod = "GET";
	public static String contentType = "Content-Type";
	public static String jsonContentType = "application/json";
	public static String textContentType = "text/plain";
	public static String tokenName = "iPlanetDirectoryPro";
	public static String acceptsName = "Accept";
	public static String cdataPrefix = "<![CDATA[";
	public static String cdataPostfix = "]]>";
	public static String dynamicQueryChar = "%%";
	public static String dynamicQueryReplaceString = "^.*?%%";
	public static String dynamicQuerySplitterString = "%%.*?(%%|$)";
	
	public static String fromStartDate = dynamicQueryChar + "fromDateTime" + dynamicQueryChar;
	public static String toEndDate = dynamicQueryChar + "toDateTime" + dynamicQueryChar;
	public static String geoLat = dynamicQueryChar + "geoLatitude" + dynamicQueryChar;
	public static String geoLong = dynamicQueryChar + "geoLongitude" + dynamicQueryChar;

	public static String kATInputSPARQLquery = "SPARQLquery";
	public static String kATInputSPARQLendpoint = "SPARQLendpoint";

	public static String openAMParamUser = "X-OpenAM-Username";
	public static String openAMParamPassword = "X-OpenAM-Password";

	public static String getERMServicesPath() {
		return getPropVal("eee.scheduler.ERMSERVICES");
	}

	public static String getERMServiceModelPath() {
		return getERMServicesPath() + getPropVal("eee.scheduler.GETEXPERIMENTSERVICEMODELOBJECT");
	}

	public static String getERMExperimentModelPath() {
		return getERMServicesPath() + getPropVal("eee.scheduler.GETEXPERIMENTMODELOBJECT");
	}

	public static String getMetaCloudServicesPath() {
		return getPropVal("eee.scheduler.METACLOUD");
	}

	public static String getFilePath() {
		return getPropVal("eee.scheduler.FILEPATHS");
	}

	public static String getSecurityUserAPI() {
		return getPropVal("eee.scheduler.SECURITYGETUSER");
	}

	public static String getERSStoreAPI() {
		return getPropVal("eee.scheduler.ERSSTOREAPI");
	}

	public static String getKATWidgetID() {
		return getPropVal("eee.scheduler.KATWIDGETID");
	}

	public static String getKATAPI() {
		return getPropVal("eee.scheduler.KATAPI");
	}

	public static String getPresentationAttrKAT() {
		return getPropVal("eee.scheduler.KATREQUESTBODY");// "requestBody";
	}

	public static String getMetaCloudResponseHeaderParam() {
		return getPropVal("eee.scheduler.METACLOUDRESPHEADERPARAM");
	}

	public static String getMetaCloudResponseHeaderParamValue() {
		return getPropVal("eee.scheduler.METACLOUDRESPHEADERPARAMVALUE");
	}

	public static String login() {
		return getPropVal("eee.scheduler.LOGIN");
	}

	public static String logout() {
		return getPropVal("eee.scheduler.LOGOUT");
	}

	public static String getOpenAMUserName() {
		return getPropVal("eee.scheduler.OPENAMUSERNAME");
	}

	public static String getOpenAMPassword() {
		return getPropVal("eee.scheduler.OPENAMPASSWORD");
	}
	
	public static int getIntervalNowToPastMax() {
		return Integer.valueOf(getPropVal("eee.scheduler.INTERVALNOWTOPASTMAX"));
	}

	static List<String> acceptedTypes = Arrays.asList("text/plain", "text/tab-separated-values", "text/csv",
			"application/sparql-results+json", "application/sparql-results+xml", "application/sparql-results+thrift",
			"application/json", "text/xml", "application/xml");

	public static List<String> getAcceptedTypes() {
		return acceptedTypes;
	}

	private static String getPropVal(String key) {
		if (props == null) {
			load();
			log.info("property files read and stored in mem");
		}
		// if (props == null)
		// return null;
		return props.getProperty(key);
	}

	private static void load() {
		InputStream input = null;
		if (props != null)
			return;
		synchronized (_sync) {
			// if (props == null) {
			try {
				// InputStream input =
				// Constants.class.getResourceAsStream("/Properties.properties");
				String PROPERTIES_FILE = "fiesta-iot.properties";

				String jbosServerConfigDir = System.getProperty("jboss.server.config.dir");
				String fiestaIotConfigFile = jbosServerConfigDir + File.separator + PROPERTIES_FILE;
				input = new FileInputStream(fiestaIotConfigFile);

				if (input != null) {
					props = new Properties();
					props.load(input);
				}
			} catch (IOException e) {
				log.error("Property File Missing:", e);
			} finally {
				if (input != null) {
					try {
						input.close();
					} catch (IOException e) {
						log.error("Property File Missing:", e);
					}
				}
			}

			// }
		}
	}

	private Constants() {
	}
}
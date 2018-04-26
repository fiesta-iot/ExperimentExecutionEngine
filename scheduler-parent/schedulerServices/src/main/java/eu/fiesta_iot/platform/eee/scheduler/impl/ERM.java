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

import java.net.HttpURLConnection;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiestaiot.commons.expdescriptiveids.model.FemoDescriptiveID;
import eu.fiestaiot.commons.fedspec.model.FISMO;

public class ERM {

	static Logger log = LoggerFactory.getLogger(ERM.class);

	public static boolean existFISMOID(String fismoID, String token) {
		log.debug("Check for FismoID if it exists:"+ fismoID);
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(Constants.getERMServiceModelPath());
		target = target.queryParam("fismoID", fismoID);

		Response response = target.request().header(Constants.tokenName, token).get();
		if (response.getStatus() == HttpURLConnection.HTTP_OK) {
			response.close();
			client.close();
			return true;
		}
		response.close();
		client.close();
		return false;
	}

	public static boolean existFEMOID(String femoID, String token) {
		log.debug("Check for FemoID if it exists:"+ femoID);
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(Constants.getERMExperimentModelPath());
		target = target.queryParam("femoID", femoID);
		Response response = target.request().header(Constants.tokenName, token).get();
		if (response.getStatus() == HttpURLConnection.HTTP_OK) {
			response.close();
			client.close();
			return true;
		}
		response.close();
		client.close();
		return false;
	}

	public static FISMO getFismoDescription(String fismoID, String token) {
		log.debug("get Fismo Description:"+ fismoID);
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(Constants.getERMServiceModelPath());
		target = target.queryParam("fismoID", fismoID);
		Response response = target.request().header(Constants.tokenName, token).get();
		if (response.getStatus() == HttpURLConnection.HTTP_OK) {
			FISMO fismo = response.readEntity(FISMO.class);
			response.close();
			client.close();
			return fismo;
		}
		response.close();
		client.close();
		return null;
	}

	public static FemoDescriptiveID getFemoDescription(String femoID, String token) {
		log.debug("get Femo Descriptive ID:"+ femoID);
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(Constants.getERMExperimentModelPath());
		target = target.queryParam("femoID", femoID);
		Response response = target.request().header(Constants.tokenName, token).get();
		if (response.getStatus() == HttpURLConnection.HTTP_OK) {
			FemoDescriptiveID femo = response.readEntity(FemoDescriptiveID.class);
			response.close();
			client.close();
			return femo;
		}
		response.close();
		client.close();
		return null;
	}

	//public static boolean existUserID(String userID, String token) {
		// TODO: Add Check for userID
		// There is no notion of userID in the ERM all is to be handled by
		// security DB. check with Paul to get this part going

		// TODO: Add session token probably
		// HttpSession session = httpRequest.getSession();
		// Boolean token = (Boolean) session.getAttribute("session token");
		// if (!token) return
		// Response.ok("{\"status\":\"NoSuchServiceModelObjectID\"}").build()

	//	return true;
	//}

}

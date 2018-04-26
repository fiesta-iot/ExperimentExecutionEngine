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
package eu.fiesta_iot.platform.eee.scheduler.rest;

import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.HibernateException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.eee.scheduler.dbUtils.SubscriptionStorage;
import eu.fiesta_iot.platform.eee.scheduler.impl.ErrorCodes;
import eu.fiesta_iot.platform.eee.scheduler.impl.JobScheduler;
import eu.fiesta_iot.platform.eee.scheduler.impl.MonitoringHelper;
import eu.fiesta_iot.platform.eee.scheduler.impl.SecurityHelper;

@Path("/monitoring")
public class MonitoringServices {
	Logger log = LoggerFactory.getLogger(MonitoringServices.class);

	@GET
	@Produces("text/plain")
	@Path("/test")
	public String welcomeMessage() {
		String welcomeText = "Welcome to monitoring Services\n" + "=============================\n\n";
		log.info(welcomeText);
		return welcomeText;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getJobIDStatus")
	public Response getJobIDStatus(@QueryParam("jobID") String jobID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.info("getJobIDStatus service");
		if (jobID.isEmpty() || !JobScheduler.existJobID(jobID))
			return ErrorCodes.noSuchJobID(jobID);
		return JobScheduler.getScheduledJobStatus(jobID);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getAllSubscriptionsOfFISMO")
	public Response getAllSubscriptionsOfFISMO(@QueryParam("fismoID") String fismoID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.info("getSubscribersFromFISMOID service");

		if (fismoID.isEmpty() || !SubscriptionStorage.existFISMOID(fismoID))
			return ErrorCodes.noSuchServiceModelObjectIDOrNotSubscribed(fismoID);
		try {
			String ret = MonitoringHelper.executeQueryForExperimenters(fismoID);
			return Response.ok("{" + ret + "}").build();
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getAllSubscriptionsOfExperimenter")
	public Response getAllSubscriptionsOfExperimenter(@HeaderParam("iPlanetDirectoryPro") String token) {
		log.info("getMySubscriptions service");

		String userID = SecurityHelper.getUserID(token);

		if (userID.isEmpty() || !SubscriptionStorage.existUserID(userID))
			return ErrorCodes.noSuchUserID(userID);
		try {
			String ret = MonitoringHelper.executeQueryForSubscriptions(userID);
			return Response.ok("{" + ret + "}").build();
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}

	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getMySubscriptionsforExperiment")
	public Response getMySubscriptionsforExperiment(@QueryParam("femoID") String femoID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.info("getMySubscriptions service");

		String userID = SecurityHelper.getUserID(token);

		if (userID.isEmpty() || !SubscriptionStorage.existUserID(userID))
			return ErrorCodes.noSuchUserID(userID);
		if (femoID.isEmpty() || !SubscriptionStorage.existFEMOID(femoID))
			return ErrorCodes.noSuchExperimentID(femoID);

		try {
			String ret = MonitoringHelper.executeQueryForSubscriptionsInExperiment(userID, femoID);
			return Response.ok("{" + ret + "}").build();
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getJobExecutionLog")
	public Response getJobExecutionLog(@QueryParam("jobID") String jobID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.info("getFISMOExecutionLog service");

		if (jobID.isEmpty() || !JobScheduler.existJobID(jobID))
			return ErrorCodes.noSuchJobID(jobID);

		try {
			String ret = MonitoringHelper.executeQueryForLogs(jobID);
			return Response.ok("{" + ret + "}").build();
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}

	}
	
	
}

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

import java.net.HttpURLConnection;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.ws.rs.DefaultValue;
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

import eu.fiesta_iot.platform.eee.scheduler.impl.Constants;
import eu.fiesta_iot.platform.eee.scheduler.impl.ErrorCodes;
import eu.fiesta_iot.platform.eee.scheduler.impl.JobScheduler;
import eu.fiesta_iot.platform.eee.scheduler.impl.MonitoringHelper;
import eu.fiesta_iot.platform.eee.scheduler.impl.SecurityHelper;

@Path("/accounting")
public class AccountingServices {
	Logger log = LoggerFactory.getLogger(AccountingServices.class);

	@GET
	@Produces("text/plain")
	@Path("/test")
	public String welcomeMessage() {
		String welcomeText = "Welcome to Accounting Services\n" + "=============================\n\n";
		log.info(welcomeText);
		return welcomeText;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getUserExecutionCount")
	public Response getUserExecutionCount(@QueryParam("fromTime") String fromTime,
			@QueryParam("toTime") @DefaultValue("") String toTime, @HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("getFISMOExecutioncount service");

		DateFormat formatter = new SimpleDateFormat(Constants.timeFormat);
		if (toTime.isEmpty() || toTime == null)
			toTime = formatter.format(new Date()).toString();
		if (fromTime.isEmpty() || fromTime == null)
			return Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
					.entity("{\"response\":\"FromTime should be specified\"}")
					.type(MediaType.APPLICATION_JSON).build();

		try {
			Date startTime = formatter.parse(fromTime);
			Date stopTime = formatter.parse(toTime);
			if (startTime.compareTo(stopTime) >= 0)
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST)
						.entity("{\"response\":\"FromTime should be less than ToTime\"}")
						.type(MediaType.APPLICATION_JSON).build();
			
			String userID = SecurityHelper.getUserID(token);
			String ret = MonitoringHelper.executeQueryForUserExecutionCount(userID, startTime, stopTime);
			return Response.ok("{" + ret + "}").build();
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		} catch (ParseException e) {
			return ErrorCodes.unParsableDate(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}

	}
	
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getJobExecutionCount")
	public Response getJobExecutionCount(@QueryParam("jobID") String jobID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("getFISMOExecutioncount service");

		if (jobID.isEmpty())
			return ErrorCodes.noSuchJobID(jobID);
		try {
			String ret = MonitoringHelper.executeQueryForCount(jobID);
			return Response.ok("{" + ret + "}").build();
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}
	
}

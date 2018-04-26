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
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.HibernateException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

//import org.apache.log4j.Logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.eee.scheduler.db.OwnerJobModel;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.OwnerJobStorage;
import eu.fiesta_iot.platform.eee.scheduler.impl.Constants;
import eu.fiesta_iot.platform.eee.scheduler.impl.ERM;
import eu.fiesta_iot.platform.eee.scheduler.impl.ErrorCodes;
import eu.fiesta_iot.platform.eee.scheduler.impl.JobScheduler;
import eu.fiesta_iot.platform.eee.scheduler.impl.SchedulerHelper;
import eu.fiesta_iot.platform.eee.scheduler.impl.SecurityHelper;
import eu.fiesta_iot.platform.eee.scheduler.impl.TimeSchedule;
import eu.fiestaiot.commons.fedspec.model.FISMO;

@Path("/scheduler")
public class SchedulerServices {
	static Logger log = LoggerFactory.getLogger(SchedulerServices.class);

	@GET
	@Produces("text/plain")
	@Path("/test")
	public String welcomeMessage() {
		String welcomeText = "Welcome to Scheduler Services\n" + "=============================\n\n";
		log.debug(welcomeText);
		return welcomeText;
	}

	// sample {"startTime":"2016-09-15T10:30:00.0Z", "stopTime":"2016-09-15T
	// 12:30:00.0Z","periodicity":10}
	// {"startTime":"2016-09-15T13:57:00.0Z", "stopTime":"2016-09-15T
	// 16:30:00.0Z","periodicity":60}
	// {"startTime":"2016-10-19T14:23:00.0Z", "stopTime":"2016-10-20T
	// 19:30:00.0Z","periodicity":120}
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/scheduleFISMOExecution")
	public Response scheduleFISMOExecution(@HeaderParam("fismoID") String fismoID, @HeaderParam("femoID") String femoID,
			@HeaderParam("iPlanetDirectoryPro") String token,
			@HeaderParam("timeSchedulePayload") @DefaultValue("") String timeSchedulePayload) {
		log.debug("startFISMOExecution service");

		String userID = SecurityHelper.getUserID(token);

		if (fismoID.isEmpty())
			return ErrorCodes.noSuchServiceModelObjectID(fismoID);
		if (femoID.isEmpty() || !ERM.existFEMOID(femoID, token))
			return ErrorCodes.noSuchExperimentID(femoID);
		//if (userID.isEmpty() || !ERM.existUserID(userID, token))
		//	return ErrorCodes.noSuchUserID(userID);

		TimeSchedule ts = null;
		
		if (timeSchedulePayload.isEmpty()) {
			log.debug("TimeSchedule was not set. it will now  calling ERM API");
			ResteasyClient clientERM = new ResteasyClientBuilder().build();
			ResteasyWebTarget targetERM = clientERM.target(Constants.getERMServiceModelPath());
			targetERM = targetERM.queryParam("fismoID", fismoID);
			Response response = targetERM.request().header("iPlanetDirectoryPro", token).get();

			if (response.getStatus() == HttpURLConnection.HTTP_OK) {
				FISMO fismo = response.readEntity(FISMO.class);

				ts = new TimeSchedule(fismo.getExperimentControl().getScheduling().getStartTime().getTime(),
						fismo.getExperimentControl().getScheduling().getStopTime().getTime(),
						fismo.getExperimentControl().getScheduling().getPeriodicity());
				response.close();
				clientERM.close();
			} else {
				String message = response.readEntity(String.class);
				response.close();
				clientERM.close();
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(message)
						.type(MediaType.APPLICATION_JSON).build();
			}

		} else {
			JSONObject json;
			log.debug("TimeSchedule was set. not calling ERM API");
			try {
				json = new JSONObject(timeSchedulePayload);
			} catch (JSONException je) {
				return ErrorCodes.jsonException();
			}
			//DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			DateFormat formatter = new SimpleDateFormat(Constants.timeFormat);
			
			if (!json.has("startTime"))
				return ErrorCodes.invalidTimeScheduleStructure("no startTime");
			if (!json.has("stopTime"))
				return ErrorCodes.invalidTimeScheduleStructure("no stopTime");
			if (!json.has("periodicity"))
				return ErrorCodes.invalidTimeScheduleStructure("no periodicity");

			try {
				Date startTime = formatter.parse(json.getString("startTime"));
				Date stopTime = formatter.parse(json.getString("stopTime"));
				Date now = new Date();

				int periodicity = json.getInt("periodicity");

				if (startTime.compareTo(stopTime) >= 0)
					return ErrorCodes.invalidTimeScheduleStructure("start date>= stop date");
				if (stopTime.compareTo(now) <= 0)
					return ErrorCodes.invalidTimeScheduleStructure("stop date already passed");

				ts = new TimeSchedule(startTime, stopTime, periodicity);

			} catch (ParseException pe) {
				return ErrorCodes.unParsableDate(pe);
			} catch (Exception e) {
				return ErrorCodes.implementationException(e);
			}
		}
		String jobID = UUID.randomUUID().toString();
		OwnerJobModel ojm = new OwnerJobModel();
		ojm.setFemoID(femoID);
		ojm.setExperimenterID(userID);
		ojm.setFismoID(fismoID);
		ojm.setJobID(jobID);
		log.debug("experimenterID=" + ojm.getExperimenterID());
		try {
			String jobIDstored = OwnerJobStorage.getInstance().getJobIDForFISMO(fismoID, femoID);
			if (jobIDstored.isEmpty())
				OwnerJobStorage.getInstance().save(ojm);
			else {
				OwnerJobStorage.getInstance().delete(jobIDstored);
				OwnerJobStorage.getInstance().save(ojm);
			}
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		}

		SchedulerHelper schedulerHelper = new SchedulerHelper(ts, jobID, fismoID, false);
		return schedulerHelper.schedule();
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getJobIDsfromFISMOID")
	public Response getJobIDsfromFISMOID(@HeaderParam("fismoID") String fismoID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("getJobIDfromFISMOID service");
		if (fismoID.isEmpty() || !ERM.existFISMOID(fismoID, token))
			return ErrorCodes.noSuchServiceModelObjectID(fismoID);
		return JobScheduler.getScheduledJobIDsForFISMOID(fismoID);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getJobIDfromFISMOIDUserIDandFEMOID")
	public Response getJobIDfromFISMOIDUserIDandFEMOID(@HeaderParam("fismoID") String fismoID,
			@HeaderParam("femoID") String femoID, @HeaderParam("iPlanetDirectoryPro") String token,
			@QueryParam("owner") @DefaultValue("true") boolean owner) {
		log.debug("getJobIDfromFISMOID service");

		String userID = SecurityHelper.getUserID(token);

		if (fismoID.isEmpty() || !ERM.existFISMOID(fismoID, token))
			return ErrorCodes.noSuchServiceModelObjectID(fismoID);
		if (femoID.isEmpty() || !ERM.existFEMOID(femoID, token))
			return ErrorCodes.noSuchExperimentID(femoID);

		if (owner)
			return JobScheduler.getScheduledJobIDsForFISMOIDandUserID(fismoID, userID, femoID, true);

		return JobScheduler.getScheduledJobIDsForFISMOIDandUserID(fismoID, userID, femoID, false);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getJobIDDetails")
	public Response getJobIDDetails(@HeaderParam("jobID") String jobID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("getJobIDfromFISMOID service");
		if (jobID.isEmpty() || !JobScheduler.existJobID(jobID))
			return ErrorCodes.noSuchJobID(jobID);
		return JobScheduler.getScheduledJobMetadata(jobID);
	}

	// should only be for ADMIN
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getAllJobIDDetails")
	public Response getAllJobIDDetails(@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("getJobIDfromFISMOID service");
		return JobScheduler.getScheduledJobsWithDetails();
	}

	// should only be for ADMIN
	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/getJobIDs")
	public Response getJobIDs(@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("getJobIDs service");
		return JobScheduler.getScheduledJobsIDs();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/stopJobExecution")
	public Response stopJobExecution(@HeaderParam("jobID") String jobID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("stopExperimentExecution service");
		if (jobID.isEmpty() || !JobScheduler.existJobID(jobID))
			return ErrorCodes.noSuchJobID(jobID);
		return JobScheduler.pauseScheduledJob(jobID);
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/resumeJobExecution")
	public Response resumeJobExecution(@HeaderParam("jobID") String jobID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("stopExperimentExecution service");
		if (jobID.isEmpty() || !JobScheduler.existJobID(jobID))
			return ErrorCodes.noSuchJobID(jobID);
		return JobScheduler.resumeScheduledJob(jobID);
	}
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_XML)
	@Path("/fISMOUpdateTrigger")
	public Response FISMOUpdateTrigger(FISMO fismo) {
		log.debug("FISMOUpdateTrigger service");

		if (fismo==null){
			return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("The Fismo object is null").type(MediaType.APPLICATION_JSON)
					.build();
		}
		
		String jobID = OwnerJobStorage.getInstance().getJobIDForFISMO(fismo.getId());
		
		return JobScheduler.rescheduleJob(jobID,
					fismo.getExperimentControl().getScheduling().getStartTime().getTime(),
					fismo.getExperimentControl().getScheduling().getStopTime().getTime(),
					fismo.getExperimentControl().getScheduling().getPeriodicity());
	}

	/*@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/fISMOUpdateTrigger")
	public Response FISMOUpdateTrigger(@HeaderParam("fismoID") String fismoID,
			@HeaderParam("femoID") String femoID) {
		log.debug("FISMOUpdateTrigger service");

		// String userID=SecurityHelper.getUserID(token);

		if (fismoID.isEmpty())
			return ErrorCodes.noSuchServiceModelObjectID(fismoID);
		if (femoID.isEmpty())
			return ErrorCodes.noSuchExperimentID(femoID);
		
		ResteasyClient clientERM = new ResteasyClientBuilder().build();
		ResteasyWebTarget targetERM = clientERM.target(Constants.getERMServiceModelPath());
		targetERM = targetERM.queryParam("fismoID", fismoID);
		Response response = targetERM.request().get();

		if (response.getStatus() == HttpURLConnection.HTTP_OK) {
			FISMO fismo = response.readEntity(FISMO.class);

			String jobID = OwnerJobStorage.getInstance().getJobIDForFISMO(fismoID, femoID);

			response.close();
			clientERM.close();
			return JobScheduler.rescheduleJob(jobID,
					fismo.getExperimentControl().getScheduling().getStartTime().getTime(),
					fismo.getExperimentControl().getScheduling().getStopTime().getTime(),
					fismo.getExperimentControl().getScheduling().getPeriodicity());
		} else {
			String message = response.readEntity(String.class);
			response.close();
			clientERM.close();
			return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(message).type(MediaType.APPLICATION_JSON)
					.build();
		}
	}*/

	/*@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/fEMOUpdateTrigger")
	public Response fEMOUpdateTrigger(@HeaderParam("femoID") String femoID, @HeaderParam("userID") String userID) {
		// String userID=SecurityHelper.getUserID(token);

		if (userID.isEmpty())
			return ErrorCodes.noSuchUserID(userID);
		if (femoID.isEmpty())
			return ErrorCodes.noSuchExperimentID(femoID);
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(Constants.getERMServiceModelPath());
		target = target.queryParam("femoID", femoID);
		Response response = target.request().get();
		boolean status = true;
		Object message = null;
		if (response.getStatus() == HttpURLConnection.HTTP_OK) {
			FemoDescriptiveID femo = response.readEntity(FemoDescriptiveID.class);
			List<FismoDescriptiveID> fismoIDs = femo.getFismoDescriptiveID();
			for (FismoDescriptiveID fdi : fismoIDs) {
				Response rs = FISMOUpdateTrigger(fdi.getId(), femoID);
				if (rs.getStatus() == HttpURLConnection.HTTP_BAD_REQUEST) {
					status = false;
					message = rs.getEntity();
				}
			}
			response.close();
			client.close();
			if (status)
				return Response.ok("{\"response\" : \"FEMO rescheduled successfully.\"}", MediaType.APPLICATION_JSON)
						.build();
			return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(message).type(MediaType.APPLICATION_JSON)
					.build();
		}
		String messageERM = response.readEntity(String.class);
		response.close();
		client.close();
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(messageERM).type(MediaType.APPLICATION_JSON)
				.build();

	}*/

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/rescheduleJob")
	public Response rescheduleJob(@HeaderParam("jobID") String jobID,
			@HeaderParam("timeSchedulePayload") String timeSchedulePayload,
			@HeaderParam("iPlanetDirectoryPro") String token) {

		if (jobID.isEmpty() || !JobScheduler.existJobID(jobID))
			return ErrorCodes.noSuchJobID(jobID);

		JSONObject json;
		try {
			json = new JSONObject(timeSchedulePayload);
		} catch (JSONException je) {
			return ErrorCodes.jsonException();
		}
		DateFormat formatter = new SimpleDateFormat(Constants.timeFormat);

		if (!json.has("startTime"))
			return ErrorCodes.invalidTimeScheduleStructure("no startTime");
		if (!json.has("stopTime"))
			return ErrorCodes.invalidTimeScheduleStructure("no stopTime");
		if (!json.has("periodicity"))
			return ErrorCodes.invalidTimeScheduleStructure("no periodicity");

		TimeSchedule ts = null;
		try {
			Date startTime = formatter.parse(json.getString("startTime"));
			Date stopTime = formatter.parse(json.getString("stopTime"));
			int periodicity = json.getInt("periodicity");
			if (startTime.compareTo(stopTime) >= 0)
				return ErrorCodes.invalidTimeScheduleStructure("start date>= stop date");
			ts = new TimeSchedule(startTime, stopTime, periodicity);

		} catch (ParseException pe) {
			return ErrorCodes.unParsableDate(pe);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}

		return JobScheduler.rescheduleJob(jobID, ts.getStartTime(), ts.getStopTime(), ts.getPeriodicity());
	}

	// owner jobs are also deleted.
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/deleteScheduledJob")
	public Response deleteScheduledJob(@HeaderParam("jobID") String jobID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("deleteScheduledJob service");
		if (jobID.isEmpty() || !JobScheduler.existJobID(jobID))
			return ErrorCodes.noSuchJobID(jobID);
		return JobScheduler.deleteScheduledJob(jobID);
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/deleteFismoJobTrigger")
	public Response deleteFismoJobTrigger(@HeaderParam("fismoID") String fismoID) {
		log.debug("deleteFismoJobTrigger service");

		// String userID=SecurityHelper.getUserID(token);

		if (fismoID.isEmpty())
			return ErrorCodes.noSuchServiceModelObjectID(fismoID);
		
		try {
			String jobID = OwnerJobStorage.getInstance().getJobIDForFISMO(fismoID);
			if (jobID.isEmpty()) {
				return ErrorCodes.noSuchJobID(jobID);

			} else {
				return JobScheduler.deleteScheduledJob(jobID);
			}
		} catch (HibernateException he) {
			return ErrorCodes.persistenceException(he);
		}
	}

	/*@GET
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/deleteFemoJobTrigger")
	public Response deletefemoJobTrigger(@HeaderParam("femoID") String femoID, @HeaderParam("userID") String userID) {
		// String userID=SecurityHelper.getUserID(token);
		log.debug("delete Femo Job Trigger");
		if (userID.isEmpty())
			return ErrorCodes.noSuchUserID(userID);
		if (femoID.isEmpty())
			return ErrorCodes.noSuchExperimentID(femoID);
		ResteasyClient client = new ResteasyClientBuilder().build();
		ResteasyWebTarget target = client.target(Constants.getERMServiceModelPath());
		target = target.queryParam("femoID", femoID);
		//TODO: fix this this token is missing so openam is blocking
		Response response = target.request().get();
		boolean status = true;
		Object message = null;
		if (response.getStatus() == HttpURLConnection.HTTP_OK) {
			FemoDescriptiveID femo = response.readEntity(FemoDescriptiveID.class);
			List<FismoDescriptiveID> fismoIDs = femo.getFismoDescriptiveID();
			for (FismoDescriptiveID fdi : fismoIDs) {
				Response rs = deleteFismoJobTrigger(fdi.getId());
				if (rs.getStatus() == HttpURLConnection.HTTP_BAD_REQUEST) {
					status = false;
					message = rs.getEntity();
				}
			}
			response.close();
			client.close();
			if (status)
				return Response.ok("{\"response\" : \"FEMO associated jobs deleted successfully.\"}",
						MediaType.APPLICATION_JSON).build();
			return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(message).type(MediaType.APPLICATION_JSON)
					.build();
		}
		String messageERM = response.readEntity(String.class);
		response.close();
		client.close();
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(messageERM).type(MediaType.APPLICATION_JSON)
				.build();

	}*/

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Consumes(MediaType.APPLICATION_JSON)
	@Path("/deletefismoJobTriggerlist")
	public Response deletefismoJobTriggerlist(String fismoIDs) {
		// String userID=SecurityHelper.getUserID(token);
		log.debug("delete Femo Job Trigger, input String:"+fismoIDs);
		boolean status = true;
		Object message = null;
		
		//JSONObject jObject=fismoIDs);
		JSONArray obj=new JSONArray(fismoIDs);
		
		if (obj.length()==0) {
			status=false; 
			return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("no FISMOs specified").type(MediaType.APPLICATION_JSON)
					.build();
		}
			
		for (int i=0;i<obj.length();i++){
			Response rs = deleteFismoJobTrigger(obj.get(i).toString());
			if (rs.getStatus() == HttpURLConnection.HTTP_BAD_REQUEST) {
				status = false;
				log.debug("from the list this fismoID not deleted :"+fismoIDs);
				message = rs.getEntity();
			}
		}
			
		if (status){
			log.debug("all fismos deleted");
			return Response.ok("{\"response\" : \"FISMOs deleted successfully.\"}",
					MediaType.APPLICATION_JSON).build();
		}
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(message).type(MediaType.APPLICATION_JSON)
				.build();
	}
	
	// owner jobs are also deleted
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/deleteScheduledJobsOfFISMO")
	public Response deleteScheduledJobsOfFismo(@HeaderParam("fismoID") String fismoID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("deleteScheduledJobsofFISMO service");
		if (fismoID.isEmpty() || !ERM.existFISMOID(fismoID, token))
			return ErrorCodes.noSuchServiceModelObjectID(fismoID);
		Response rs = JobScheduler.getScheduledJobIDsForFISMOID(fismoID);
		if (rs.getStatus() == HttpURLConnection.HTTP_OK) {
			JSONObject jsonobject = new JSONObject(rs.getEntity());
			if (jsonobject.has("jobIDs")) {
				JSONArray jsonarray = jsonobject.getJSONArray("jobsIDs");
				for (Object id : jsonarray) {
					JobScheduler.deleteScheduledJob(id.toString());// delete all
																	// jobs
				}
				return Response
						.ok("{\"response\":\"All jobs associated to Fismo are deleted\"}", MediaType.APPLICATION_JSON)
						.build();
			}
			return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(rs.getEntity())
					.type(MediaType.APPLICATION_JSON).build();
		}
		return ErrorCodes.implementationException();
	}

	// for the Administrator
	// deletes all jobs (owner, scheduler, subscriptions.)
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/deleteAllScheduledJob")
	public Response deleteAllScheduledJob(@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("deleteScheduledJob service");
		return JobScheduler.deleteAllScheduledJobs();
	}

	// for the Administrator
	@GET
	@Path("/getCurrentlyExecutingJobs")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getCurrentlyExecutingJobs(@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("Get currently executing Job Details.");
		return JobScheduler.currentlyExecutingJobs();
	}
}
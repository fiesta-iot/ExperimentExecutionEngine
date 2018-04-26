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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorCodes {
	
	static Logger log = LoggerFactory.getLogger(ErrorCodes.class);

	public static Response noSuchServiceModelObjectID(String fismoID) {
		log.error("FISMO id " + fismoID + " does not exist.");
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.NoSuchServiceModelObjectID)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response noSuchExperimentID(String femoID) {
		log.error("femo with id " + femoID + " does not exist.");
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.NoSuchExperimentID)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response noSuchUserID(String userID) {
		log.error("user with id " + userID + " does not exist.");
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.NoSuchUserID).type(MediaType.APPLICATION_JSON)
				.build();
	}

	public static Response jsonException() {
		log.error("Json Exception");
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.JsonException)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response invalidTimeScheduleStructure(String message) {
		log.error(message);
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.InvalidTimeScheduleStructure)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response unParsableDate(Exception e) {
		log.error("date parse exception",e);
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.UnParsableDate)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response implementationException(Exception e) {
		log.error("",e);
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.ImplementationException)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response implementationException() {
		log.error("Some exception");
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.ImplementationException)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response persistenceException(Exception e) {
		log.error("hibernate exception ",e);
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.PersistenceException)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response noSuchJobID(String jobID) {
		log.error("Job with id " + jobID + " does not exist.");
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.NoSuchJobID).type(MediaType.APPLICATION_JSON)
				.build();
	}

	public static Response invalidURL() {
		log.error("invalid URL");
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.InvalidURL).type(MediaType.APPLICATION_JSON)
				.build();
	}

	public static Response invalidExperimentOutputJson(Exception e) {
		log.error("Invalid Experiment Output Json",e);
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.InvalidExperimentOutputJson)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response metaCloudException(Exception e) {
		log.error("Invalid Experiment Output Json",e);
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.MetaCloudException)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response subscriptionNotFound(Exception e) {
		log.error("Subscription NotFound",e);
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.SubscriptionNotFound)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response subscriptionNotFound() {
		log.error("Subscription NotFound");
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.SubscriptionNotFound)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response alreadySubscribed() {
		log.error("Already Subscribed");
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.AlreadySubscribed)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response noSuchServiceModelObjectIDOrNotSubscribed(String fismoID) {
		log.error("FISMO id " + fismoID + " does not exist.");
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.NoSuchServiceModelObjectIDOrNotSubscribed)
				.type(MediaType.APPLICATION_JSON).build();
	}

	public static Response schedulerException(Exception e) {
		log.error("Scheduler Exception",e);
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.SchedulerException)
				.type(MediaType.APPLICATION_JSON).build();
	}
	
	public static Response queryException(Exception e) {
		log.error("Query Exception:",e);
		return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(Constants.QueryException)
				.type(MediaType.APPLICATION_JSON).build();
	}
}

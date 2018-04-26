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
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;

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
import org.json.JSONObject;

//import org.apache.log4j.Logger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.eee.scheduler.db.CallLogModel;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.SubscriptionStorage;
import eu.fiesta_iot.platform.eee.scheduler.impl.Commons;
import eu.fiesta_iot.platform.eee.scheduler.impl.Constants;
import eu.fiesta_iot.platform.eee.scheduler.impl.ERM;
import eu.fiesta_iot.platform.eee.scheduler.impl.ErrorCodes;
import eu.fiesta_iot.platform.eee.scheduler.impl.KatHelper;
import eu.fiesta_iot.platform.eee.scheduler.impl.MetaCloud;
import eu.fiesta_iot.platform.eee.scheduler.impl.SecurityHelper;
import eu.fiestaiot.commons.fedspec.model.FISMO;
import eu.fiestaiot.commons.fedspec.model.PresentationAttr;
import eu.fiestaiot.commons.fedspec.model.Widget;

@Path("/polling")
public class PollingServices {

	Logger log = LoggerFactory.getLogger(getClass());

	@GET
	@Produces("text/plain")
	@Path("/test")
	public String welcomeMessage() {
		String welcomeText = "Welcome to Polling Services\n" + "=============================\n\n";
		log.debug(welcomeText);
		return welcomeText;
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/pollForReport")
	public Response pollForReport(@HeaderParam("fismoID") String fismoID, @HeaderParam("femoID") String femoID,
			@HeaderParam("iPlanetDirectoryPro") String token,
			@QueryParam("owner") @DefaultValue("true") boolean owner) {
		log.debug("pollForReport service");

		CallLogModel logging = new CallLogModel();
		logging.setFismoID(fismoID);
		String jobID=UUID.randomUUID().toString();
		logging.setJobID(jobID);

		logging.setStartTime(new Date());
		
		String userID = SecurityHelper.getUserID(token);

		if (fismoID.isEmpty())
			return ErrorCodes.noSuchServiceModelObjectID(fismoID);
		if (femoID.isEmpty() || !ERM.existFEMOID(femoID, token))
			return ErrorCodes.noSuchExperimentID(femoID);

		String query = "";
		String urlPath = "";
		String returnType = Constants.jsonContentType;
		boolean reportIfEmpty=true;
		String latitude="0";
		String longitude="0";
		int intervalNowToPast=0;
		Long fromTime=new Date().getTime();
		Long toTime= new Date().getTime();
		String kATInput="";
		String otherParameters="";

		Date now = new Date();
		
		if (!owner){
			log.debug("Polling in for subscription realm");
			urlPath = SubscriptionStorage.getInstance().getURL(fismoID, userID, femoID);
			returnType = SubscriptionStorage.getInstance().getFileType(fismoID, userID, femoID);
			reportIfEmpty = SubscriptionStorage.getInstance().getReportIfEmpty(fismoID, userID, femoID);
			
			query = Commons.stripCDATA(Commons.stripCDATA(SubscriptionStorage.getInstance().getQuery(fismoID, userID, femoID)));
			fromTime=SubscriptionStorage.getInstance().getFromTime(fismoID, userID, femoID);
			toTime=SubscriptionStorage.getInstance().getToTime(fismoID, userID, femoID);
			intervalNowToPast=SubscriptionStorage.getInstance().getIntervalToPast(fismoID, userID, femoID);
			longitude=SubscriptionStorage.getInstance().getLatitude(fismoID, userID, femoID);
			latitude=SubscriptionStorage.getInstance().getLongitude(fismoID, userID, femoID);
			otherParameters=SubscriptionStorage.getInstance().getOtherAttributes(fismoID, userID, femoID);
			try {
				//processQuery(String query, Long fromTime,Long toTime,int intervalNowToPast,String longitude,String latitude,String otherParameters)
				query=processQuery(query, fromTime, toTime, intervalNowToPast,longitude,latitude,otherParameters);
			}catch(Exception e){
				return ErrorCodes.queryException(e);
			}
			
			kATInput=SubscriptionStorage.getInstance().getKatInput(fismoID, userID, femoID);
			
			}
		else{
			log.debug("Polling In for owner realm");
			ResteasyClient clientERM = new ResteasyClientBuilder().build();
			ResteasyWebTarget targetERM = clientERM.target(Constants.getERMServiceModelPath());
			targetERM = targetERM.queryParam("fismoID", fismoID);
			Response response = targetERM.request().header(Constants.tokenName, token).get();
			if (response.getStatus() == HttpURLConnection.HTTP_OK) {
				FISMO fismo = response.readEntity(FISMO.class);
				response.close();
				clientERM.close();
				
				urlPath = fismo.getExperimentOutput().getLocation();
				log.debug("urlPath:" + urlPath);
				log.debug("fismo ID:" + fismo.getId());
				log.debug("femo ID:" + femoID);
				query = fismo.getQueryControl().getQueryRequest().getQuery();
				query = Commons.stripCDATA(Commons.stripCDATA(query));
				try {
					query=Commons.processQuery(query, fismo);
				}catch(Exception e){
					return ErrorCodes.queryException(e);
				}
				log.debug("query=" + query);
				try{
					returnType=fismo.getExperimentOutput().getFile().get(0).getType();
					log.debug("returnType in the fismo: "+returnType);
					if (!Constants.getAcceptedTypes().contains(returnType)){
						log.debug("file type was wrongly set");
					}
				}catch(Exception e){
					log.debug("No file type was set");
				}
				try{
					reportIfEmpty=fismo.getExperimentControl().isReportIfEmpty();
				}catch(Exception e){
					log.debug("No report if empty set");
				}
				try{
					List<Widget> lwidget=fismo.getExperimentOutput().getWidget();
					for(Widget widget:lwidget ){
						if (widget.getWidgetID().equals(Constants.getKATWidgetID())){
							List<PresentationAttr> wpr=widget.getPresentationAttr();
							for(PresentationAttr pr:wpr){
								if (pr.getName().equals(Constants.getPresentationAttrKAT())){
									kATInput=pr.getValue();
									kATInput = kATInput.replaceAll("&quot;","\"");
								}
							}
						}
					}
					log.debug("kat Input: "+ kATInput);
				}catch(Exception e){
					log.debug("No Kat input set");
				}
			}
			else{
				String message = response.readEntity(String.class);
				response.close();
				clientERM.close();
				return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(message).type(MediaType.APPLICATION_JSON)
						.build();
			}
		}
			
		try {
			if (kATInput.isEmpty()){
				if (MetaCloud.callMetaCloud(femoID, token, logging, jobID, userID, query, urlPath, returnType, reportIfEmpty, now))
					return Response.ok("{\"response\":\"Polled successfully\", \"jobID\":\""+jobID+"\"}").build();
				else
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("{\"response\":\"Something went wrong\"}").type(MediaType.APPLICATION_JSON)
							.build();
			}
			else{
				if (KatHelper.kATFunction(jobID, now, token, query, kATInput,
						 femoID, logging))
						return Response.ok("{\"response\":\" Polled successfully\", \"jobID\":\""+jobID+"\"}").build();
					else
						return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("{\"response\":\"FIESTA-IoT Analytics tool was not invoked correctly. Thus polling failed.\"}")
								.type(MediaType.APPLICATION_JSON).build();
				}
			
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}
	
	
	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/dynamicPollForReport")
	public Response dynamicPollForReport(@HeaderParam("fismoID") String fismoID, 
			@HeaderParam("femoID") String femoID,
			@HeaderParam("iPlanetDirectoryPro") String token,
			@QueryParam("owner") @DefaultValue("true") boolean owner, 
			@QueryParam("geoLatitude") @DefaultValue("0") String latitude,
			@QueryParam("geoLongitude") @DefaultValue("0") String longitude, 
			@QueryParam("intervalNowToPast") @DefaultValue("0") int intervalNowToPast,
			@QueryParam("fromTime") @DefaultValue("0L") Long fromTime,
			@QueryParam("toTime") @DefaultValue("0L") Long toTime,
			String other) { 
		
		/*others is in the body this includes KATInput and otherParameters input in the format 
		 *{"KATInput":{"Method":[""], "Parameters":[""]}},
		 *"otherParameters":{<key>:<value>}}
		 */
		
		log.debug("Dynamic pollForReport service");

		CallLogModel logging = new CallLogModel();
		logging.setFismoID(fismoID);
		String jobID=UUID.randomUUID().toString();
		logging.setJobID(jobID);

		logging.setStartTime(new Date());
		
		String userID = SecurityHelper.getUserID(token);

		if (fismoID.isEmpty())
			return ErrorCodes.noSuchServiceModelObjectID(fismoID);
		if (femoID.isEmpty() || !ERM.existFEMOID(femoID, token))
			return ErrorCodes.noSuchExperimentID(femoID);

		String query = "";
		String urlPath = "";
		String returnType = Constants.jsonContentType;
		boolean reportIfEmpty=true;
		String katInput="";
		String otherParam="";

		ResteasyClient clientERM = new ResteasyClientBuilder().build();
		ResteasyWebTarget targetERM = clientERM.target(Constants.getERMServiceModelPath());
		targetERM = targetERM.queryParam("fismoID", fismoID);
		Response response = targetERM.request().header(Constants.tokenName, token).get();

		Date now = new Date();

		if (response.getStatus() == HttpURLConnection.HTTP_OK) {
			FISMO fismo = response.readEntity(FISMO.class);
			response.close();
			clientERM.close();
			if (!owner){
				urlPath = SubscriptionStorage.getInstance().getURL(fismoID, userID, femoID);
				returnType = SubscriptionStorage.getInstance().getFileType(fismoID, userID, femoID);
				reportIfEmpty = SubscriptionStorage.getInstance().getReportIfEmpty(fismoID, userID, femoID);
				query = Commons.stripCDATA(Commons.stripCDATA(SubscriptionStorage.getInstance().getQuery(fismoID, userID, femoID)));
			}
			else{
				urlPath = fismo.getExperimentOutput().getLocation();
				try{
					returnType=fismo.getExperimentOutput().getFile().get(0).getType();
					log.debug("returnType in the fismo: "+returnType);
					if (!Constants.getAcceptedTypes().contains(returnType)){
						log.debug("file type was wrongly set");
					}
				}catch(Exception e){
					log.debug("No file type was set");
				}
				try{
					reportIfEmpty=fismo.getExperimentControl().isReportIfEmpty();
				}catch(Exception e){
					log.debug("No report if empty set");
				}
				query = fismo.getQueryControl().getQueryRequest().getQuery();
				query = Commons.stripCDATA(Commons.stripCDATA(query));
				
				try{
					returnType=fismo.getExperimentOutput().getFile().get(0).getType();
					log.debug("returnType in the fismo: "+returnType);
					if (!Constants.getAcceptedTypes().contains(returnType)){
						log.debug("file type was wrongly set");
					}
				}catch(Exception e){
					log.debug("No file type was set");
				}
				try{
					reportIfEmpty=fismo.getExperimentControl().isReportIfEmpty();
				}catch(Exception e){
					log.debug("No report if empty set");
				}
			}
		}
		else{
			String message = response.readEntity(String.class);
			response.close();
			clientERM.close();
			return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(message).type(MediaType.APPLICATION_JSON)
					.build();
		}

		log.debug("urlPath:" + urlPath);

		try{
			JSONObject jsonObject = new JSONObject(other);
			if (jsonObject.has("KATInput")){
				katInput=jsonObject.getString("KATInput");
				log.debug("kat Input: "+ katInput);
			}
			if (jsonObject.has("otherParameters")){
				otherParam=jsonObject.getString("otherParameters");
				log.debug("otherParameters Input: "+ otherParam);
			}
		}catch(Exception e){
			return ErrorCodes.jsonException();
		}
		
		try {
			query=processQuery(query, fromTime,toTime,intervalNowToPast,longitude,latitude,otherParam);
		}catch(Exception e){
			return ErrorCodes.queryException(e);
		}
		
		log.debug("query=" + query);
		
		
		try {
			
			if (katInput.isEmpty()){
				if (MetaCloud.callMetaCloud(femoID, token, logging, jobID, userID, query, urlPath, returnType, reportIfEmpty, now))
					return Response.ok("{\"response\":\"Dynamically Polled successfully\", \"jobID\":\""+jobID+"\"}").build();
				else
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("{\"response\":\"Something went wrong\"}").type(MediaType.APPLICATION_JSON)
							.build();
			}
			else{
				if (KatHelper.kATFunction(jobID, now, token, query, katInput,
					 femoID, logging))
					return Response.ok("{\"response\":\"Dynamically Polled successfully\", \"jobID\":\""+jobID+"\"}").build();
				else
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity("{\"response\":\"FIESTA-IoT Analytics tool was not invoked correctly. Thus Dynamic polling failed.\"}")
							.type(MediaType.APPLICATION_JSON).build();
				
			}

			
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}
	
	private String processQuery(String query, Long fromTime,Long toTime,int intervalNowToPast,String longitude,String latitude,String otherParameters) throws Exception{
		String processedQuery=query;
		
		if(query.contains(Constants.dynamicQueryChar)){
			String querytemp = query.replaceAll(Constants.dynamicQueryReplaceString, "");
			List<String> variables = Arrays.asList( querytemp.split(Constants.dynamicQuerySplitterString));
			
			JSONObject jsonObject=new JSONObject(otherParameters);
			
			if (jsonObject.length()!=0){
				for (String key: JSONObject.getNames(jsonObject)){
					for (String variable: variables){
						if (key.equals(variable)){
							processedQuery = processedQuery.replaceAll(Constants.dynamicQueryChar+variable+Constants.dynamicQueryChar,jsonObject.get(key).toString());
						}
					}	
				}
			}
			processedQuery=Commons.replaceAttributes(latitude, longitude, intervalNowToPast, fromTime, toTime, processedQuery);
		}
		
		if(processedQuery.contains(Constants.dynamicQueryChar)){
			//raise there is exception
			log.error("processedQuery="+processedQuery);
			throw new Exception("Incorrect Query");
		}
	return processedQuery;
	}
}

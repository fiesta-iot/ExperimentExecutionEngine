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
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

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
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//import eu.fiesta_iot.platform.eee.scheduler.db.OwnerJobModel;
import eu.fiesta_iot.platform.eee.scheduler.db.SubscriptionModel;
//import eu.fiesta_iot.platform.eee.scheduler.dbUtils.OwnerJobStorage;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.SubscriptionStorage;
import eu.fiesta_iot.platform.eee.scheduler.impl.Constants;
import eu.fiesta_iot.platform.eee.scheduler.impl.ERM;
import eu.fiesta_iot.platform.eee.scheduler.impl.ErrorCodes;
import eu.fiesta_iot.platform.eee.scheduler.impl.JobScheduler;
import eu.fiesta_iot.platform.eee.scheduler.impl.SchedulerHelper;
import eu.fiesta_iot.platform.eee.scheduler.impl.SecurityHelper;
import eu.fiesta_iot.platform.eee.scheduler.impl.TimeSchedule;
import eu.fiestaiot.commons.fedspec.model.DynamicAttr;
import eu.fiestaiot.commons.fedspec.model.DynamicAttrs;
import eu.fiestaiot.commons.fedspec.model.FISMO;
import eu.fiestaiot.commons.fedspec.model.PredefinedDynamicAttr;
import eu.fiestaiot.commons.fedspec.model.PresentationAttr;
import eu.fiestaiot.commons.fedspec.model.Widget;

@Path("/subscription")
public class SubscriptionServices {
	Logger log = LoggerFactory.getLogger(SubscriptionServices.class);

	@GET
	@Produces("text/plain")
	@Path("/test")
	public String welcomeMessage() {
		String welcomeText = "Welcome to Subscription Services\n" + "=============================\n\n";
		log.debug(welcomeText);
		return welcomeText;
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/subscribeToFISMOReport")
	public Response subscribeToFISMOReport(@HeaderParam("femoID") String femoID, @HeaderParam("fismoID") String fismoID,
			@HeaderParam("experimentOutput") String experimentOutput,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("subscribeToFISMOReport service");

		String userID = SecurityHelper.getUserID(token);

		log.debug("Subscription userID=" + userID);

		if (fismoID.isEmpty())
			return ErrorCodes.noSuchServiceModelObjectID(fismoID);
		if (femoID.isEmpty() || !ERM.existFEMOID(femoID, token))
			return ErrorCodes.noSuchExperimentID(femoID);

		JSONObject json;

		try {
			json = new JSONObject(experimentOutput);
			if (!json.has("url"))
				return ErrorCodes.invalidURL();
			else {
				if (SubscriptionStorage.getInstance().checkSubscriptionByUserIDForFISMO(fismoID, userID, femoID))
					return ErrorCodes.alreadySubscribed();

				// "{\"startTime\":\"2016-09-17T03:35:00.0Z\",
				// \"stopTime\":\"2016-09-17T20:30:00.0Z\",\"periodicity\":300}";

				ResteasyClient clientERM = new ResteasyClientBuilder().build();
				ResteasyWebTarget targetERM = clientERM.target(Constants.getERMServiceModelPath());
				targetERM = targetERM.queryParam("fismoID", fismoID);
				Response response = targetERM.request().header(Constants.tokenName, token).get();
				TimeSchedule ts = null;
				String query = "";
				String fileType=Constants.jsonContentType;
				DynamicAttrs attribute;
				PredefinedDynamicAttr pda;
				List<DynamicAttr> lda=null;
				String latitude="0";
				String longitude="0";
				int intervalNowToPast=0;
				Long fromTime=new Date().getTime();
				Long toTime= new Date().getTime();
				String kATInput="";
				boolean reportIfEmpty=false;
				
				if (response.getStatus() == HttpURLConnection.HTTP_OK) {
					FISMO fismo = response.readEntity(FISMO.class);
					response.close();
					clientERM.close();

					int periodicity = fismo.getExperimentControl().getScheduling().getPeriodicity();
					query = fismo.getQueryControl().getQueryRequest().getQuery();
					
					try{
						fileType=fismo.getExperimentOutput().getFile().get(0).getType();
						log.debug("returnType in the fismo: "+fileType);
						if (!Constants.getAcceptedTypes().contains(fileType)){
							log.debug("file type was wrongly set");
							
						}
					}catch(Exception e){
						log.debug("No file type was set");
						
					}
					try{
						reportIfEmpty=fismo.getExperimentControl().isReportIfEmpty();
					}catch(Exception e){
						log.debug("No report if empty set");
						reportIfEmpty=true;
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
						log.debug("KatInput="+kATInput);
					}catch(Exception e){
						log.debug("No Kat set or an Exception");
					}
					
					if(query.contains(Constants.dynamicQueryChar)){
						try {
							attribute=fismo.getQueryControl().getDynamicAttrs();
							try {
								pda=attribute.getPredefinedDynamicAttr();
								try {
									latitude=pda.getDynamicGeoLocation().getLatitude();
									longitude=pda.getDynamicGeoLocation().getLongitude();
								}
								catch(Exception e){
									log.debug("DynamicGeoLocation not set"); 
								}
								try{
									intervalNowToPast = pda.getDynamicQueryInterval().getIntervalNowToPast();
								}catch(Exception e){
									log.debug("intervalNowToPast not set"); 
								}
								try{
									fromTime=pda.getDynamicQueryInterval().getFromDateTime().getTime().getTime();
									toTime=pda.getDynamicQueryInterval().getToDateTime().getTime().getTime();
								}catch(Exception e){
									log.debug("either from time to time not set"); 
								}
							}
							catch(Exception e){
								log.debug("PredefinedDynamicAttr not set"); 
							}
							try {
								lda=attribute.getDynamicAttr();
							}
							catch(Exception e){
								log.debug("DynamicAttr not set"); 
							}
						}
						catch(Exception e){
							log.debug("DynamicAttrs not set"); 
						}
					}
					
					Date startTimeDate = fismo.getExperimentControl().getScheduling().getStartTime().getTime();
					Date stopTimeDate = fismo.getExperimentControl().getScheduling().getStopTime().getTime();
					ts = new TimeSchedule(startTimeDate, stopTimeDate, periodicity);

				} else {
					String message = response.readEntity(String.class);
					response.close();
					clientERM.close();
					return Response.status(HttpURLConnection.HTTP_BAD_REQUEST).entity(message)
							.type(MediaType.APPLICATION_JSON).build();
				}

				String jobID = UUID.randomUUID().toString();
				

				SchedulerHelper schedulerHelper = new SchedulerHelper(ts, jobID, fismoID, true);
				Response rs = schedulerHelper.schedule();

				try {
					if (rs.getStatus() == 200) {
						SubscriptionModel subscribe = new SubscriptionModel();
						subscribe.setExperimenterID(userID);
						subscribe.setFismoID(fismoID);
						subscribe.setFemoID(femoID);
						subscribe.setJobID(jobID);
						subscribe.setUrl(json.getString("url").toString());
						subscribe.setquery(query);
						subscribe.setFileType(fileType);
						subscribe.setReportIfEmpty(reportIfEmpty);
						subscribe.setkATInput(kATInput);
						if (fromTime!=0L)
							subscribe.setFromTime(fromTime);
						if (toTime!=0L)
							subscribe.setToTime(toTime);
						if (intervalNowToPast!=0)
							subscribe.setIntervalNowToPast(intervalNowToPast);
						//if (!latitude.equals("0"))
						subscribe.setGeoLatitude(latitude);
						//if (!longitude.equals("0"))
						subscribe.setGeoLongitude(longitude);
						Map<String, Object> jsonList = new HashMap<>();
						if (lda!=null){
							if (lda.size()!=0){
								for (int i=0;i<lda.size();i++){
									jsonList.put(lda.get(i).getName(),lda.get(i).getValue());
								}
								subscribe.setOtherAttributes(jsonList.toString());
							}
						}
						log.debug(subscribe.getExperimenterID());

						SubscriptionStorage.getInstance().save(subscribe);
						return Response.ok("{\"response\":\"subscribed\", \"FISMOID:\"" + fismoID + "\", "
								+ "\"JobID\":\"" + jobID + "\"}").build();
					}
				} catch (Exception e) {
					return ErrorCodes.implementationException(e);
				}

				return rs; // these are the scheduler exceptions
			}
		} catch (JSONException e) {
			return ErrorCodes.invalidExperimentOutputJson(e);
		} catch (HibernateException e) {
			return ErrorCodes.alreadySubscribed();
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	@Path("/unsubscribeToFISMOReport")
	public Response unsubscribeToFISMOReport(@QueryParam("femoID") String femoID, @QueryParam("fismoID") String fismoID,
			@HeaderParam("iPlanetDirectoryPro") String token) {
		log.debug("unsubscribeToFISMOReport service");

		String userID = SecurityHelper.getUserID(token);

		if (fismoID.isEmpty() || !ERM.existFISMOID(fismoID, token))
			return ErrorCodes.noSuchServiceModelObjectID(fismoID);
		if (femoID.isEmpty() || !ERM.existFEMOID(femoID, token))
			return ErrorCodes.noSuchExperimentID(femoID);

		try {
			log.debug("unsubscribeToFISMOReport define subscribe model");
			String jobID = SubscriptionStorage.getInstance().getJobIDByUserIDForFISMO(fismoID, userID, femoID);
			log.debug(jobID);
			if (jobID != null) {
				// SubscriptionStorage.getInstance().delete(fismoID,femoID,userID);
				Response rs = JobScheduler.deleteScheduledJob(jobID);
				if (rs.getStatus() == HttpURLConnection.HTTP_OK)
					return Response.ok("{\"response\":\"Unsubscribed\"}").build();
				else
					return rs; // these are the scheduler exceptions
			}
			return ErrorCodes.subscriptionNotFound();
		} catch (HibernateException e) {
			return ErrorCodes.subscriptionNotFound(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}
}

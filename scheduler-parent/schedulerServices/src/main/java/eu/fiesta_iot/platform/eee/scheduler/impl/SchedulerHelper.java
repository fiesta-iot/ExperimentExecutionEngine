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
import java.util.Date;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.HibernateException;
import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.jboss.resteasy.client.jaxrs.ResteasyClientBuilder;
import org.jboss.resteasy.client.jaxrs.ResteasyWebTarget;
import org.quartz.Job;

import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.eee.scheduler.db.CallLogModel;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.OwnerJobStorage;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.SubscriptionStorage;
import eu.fiestaiot.commons.fedspec.model.FISMO;
import eu.fiestaiot.commons.fedspec.model.PresentationAttr;
import eu.fiestaiot.commons.fedspec.model.Widget;

public class SchedulerHelper extends Thread implements Job {

	final static Logger logger = LoggerFactory.getLogger(SchedulerHelper.class);

	private String fismoID;
	private String jobID;
	private TimeSchedule timeSchedule;
	private boolean subscribed;

	public SchedulerHelper(TimeSchedule timeSchedule, String jobID, String fismoID, boolean subscribed) {
		this.timeSchedule = timeSchedule;
		this.jobID = jobID;
		this.fismoID = fismoID;
		this.subscribed = subscribed;
	}

	public SchedulerHelper() {
	}

	@Override
	public void execute(JobExecutionContext context) throws JobExecutionException {
		logger.debug("Execute scheduled job.");
		Date now = new Date();

		String token = SecurityHelper.login();
		String query = "";
		String fileType = Constants.jsonContentType;
		boolean reportIfEmpty = false;
		String kATInput = "";
		String url = "";

		if (this.subscribed) {
			logger.debug("in the subscription realm");

			fileType = SubscriptionStorage.getInstance().getFileType(context.getJobDetail().getKey().getName());
			reportIfEmpty = SubscriptionStorage.getInstance()
					.getReportIfEmpty(context.getJobDetail().getKey().getName());

			query = Commons.stripCDATA(Commons
					.stripCDATA(SubscriptionStorage.getInstance().getQuery(context.getJobDetail().getKey().getName())));

			try {
				query = Commons.processQuerySubscribe(query, context.getJobDetail().getKey().getName());
			} catch (Exception e) {
				throw new JobExecutionException("Incorrect Query ");
			}

			kATInput = SubscriptionStorage.getInstance().getKatInput(context.getJobDetail().getKey().getName());

			url = SubscriptionStorage.getInstance().getURL(context.getJobDetail().getKey().getName());
			queryAndExecute(context, now, token, query, fileType, reportIfEmpty, kATInput, url, false);
		} else {
			logger.debug("in the owner realm");
			ResteasyClient clientERM = new ResteasyClientBuilder().build();
			ResteasyWebTarget targetERM = clientERM.target(Constants.getERMServiceModelPath());
			targetERM = targetERM.queryParam("fismoID", context.getJobDetail().getKey().getGroup());
			Response response = targetERM.request().header(Constants.tokenName, token).get();//

			if (response.getStatus() == HttpURLConnection.HTTP_OK) {
				FISMO fismo = response.readEntity(FISMO.class);
				logger.debug("femo ID:" + fismo.getId());

				query = Commons.stripCDATA(Commons.stripCDATA(fismo.getQueryControl().getQueryRequest().getQuery()));
				try {
					query = Commons.processQuery(query, fismo);
				} catch (Exception e) {
					throw new JobExecutionException("Incorrect Query ");
				}

				logger.debug("query after preprocessing =" + query);

				try {
					fileType = fismo.getExperimentOutput().getFile().get(0).getType();
					if (!Constants.getAcceptedTypes().contains(fileType)) {
						logger.debug("file type was wrongly set, setting to default");
					}
				} catch (Exception e) {
					logger.debug("No file type was set, setting to default");
				}
				try {
					reportIfEmpty = fismo.getExperimentControl().isReportIfEmpty();
				} catch (Exception e) {
					logger.debug("No report if empty set, setting to default");
				}
				try {
					List<Widget> lwidget = fismo.getExperimentOutput().getWidget();
					for (Widget widget : lwidget) {
						if (widget.getWidgetID().equals(Constants.getKATWidgetID())) {
							List<PresentationAttr> wpr = widget.getPresentationAttr();
							for (PresentationAttr pr : wpr) {
								if (pr.getName().equals(Constants.getPresentationAttrKAT())) {
									kATInput = pr.getValue();
									kATInput = kATInput.replaceAll("&quot;", "\"");
								}
							}
						}
					}
				} catch (Exception e) {
					logger.debug("No Kat input set");
				}
				url = fismo.getExperimentOutput().getLocation();

				queryAndExecute(context, now, token, query, fileType, reportIfEmpty, kATInput, url, true);
			}
			response.close();
			clientERM.close();
		}
		SecurityHelper.logout(token);
	}

	private void queryAndExecute(JobExecutionContext context, Date now, String token, String query, String fileType,
			boolean reportIfEmpty, String kATInput, String url, boolean owner) {
		String femoID = "";
		String userID = "";
		if (!owner) {
			OwnerJobStorage ojs = new OwnerJobStorage();
			femoID = ojs.getFEMOID(context.getJobDetail().getKey().getGroup());
			userID = ojs.getUserID(context.getJobDetail().getKey().getGroup());
		} else {
			SubscriptionStorage subscriptionStorage = new SubscriptionStorage();
			femoID = subscriptionStorage.getFEMOID(context.getJobDetail().getKey().getGroup());
			userID = subscriptionStorage.getUserID(context.getJobDetail().getKey().getGroup());
		}
		try {
			logger.debug("query And execute module");
			CallLogModel logging = new CallLogModel();
			logging.setFismoID(context.getJobDetail().getKey().getGroup());
			logging.setJobID(context.getJobDetail().getKey().getName());
			logging.setStartTime(new Date());

			if (kATInput.isEmpty()) {
				if (MetaCloud.callMetaCloud(context, now, token, query, fileType, reportIfEmpty, url, femoID, userID,
						logging))
					logger.debug("IoT-Registry/sending/storage call successful");
				else
					logger.debug("IoT-Registry/sending/storage call failed");
			} else {
				if (KatHelper.kATFunction(context.getJobDetail().getKey().getName(), now, token, query, kATInput,
						femoID, logging))
					logger.debug("KAT call successful");
				else
					logger.debug("KAT call failed");

			}

		} catch (HibernateException e) {
			logger.error("Hibernate Exception:", e);
		} catch (Exception e) {
			logger.error("Exception:", e);
		}
	}

	public Response schedule() {
		logger.debug("Schedule");

		try {
			if (timeSchedule != null) {
				JobScheduler js = new JobScheduler(this.timeSchedule, this.jobID, this.fismoID);
				js.scheduleJob();
				return Response.ok("{\"response\" : \"Job scheduled.\" , \n \"jobID\" :" + " \"" + this.jobID + "\"}",
						MediaType.APPLICATION_JSON).build();
			} else {
				logger.debug("No time schedule for this Job");
			}
		} catch (SchedulerException se) {
			return ErrorCodes.schedulerException(se);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
		return Response.serverError().build();
	}
}

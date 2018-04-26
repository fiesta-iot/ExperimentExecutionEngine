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

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.hibernate.HibernateException;

import eu.fiesta_iot.platform.eee.scheduler.db.CallLogModel;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.CallLogStorage;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.OwnerJobStorage;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.SubscriptionStorage;

public class MonitoringHelper {

	public static String executeQueryForCount(String jobID) throws Exception {
		String count = CallLogStorage.getInstance().getNumberOfCallsfor(jobID);
		String jsonString = "\"count\": " + count;
		return jsonString;
	}
	
	public static String executeQueryForUserExecutionCount(String userID,Date fromTime, Date toTime) throws Exception {
		List<String> sub=SubscriptionStorage.getInstance().getAllJobIDofUserID(userID);
		List<String> owner=OwnerJobStorage.getInstance().getAllJobIDOfExperimenter(userID);
		List<String> newList = new ArrayList<String>(sub);
		newList.addAll(owner);
		
		String count = CallLogStorage.getInstance().getNumberOfCallsforUser(newList,fromTime, toTime);
		String jsonString = "\"count\": " + count;
		return jsonString;
	}

	public static String executeQueryForExperimenters(String fismoID) throws HibernateException, Exception {
		List<String> subscriptions = SubscriptionStorage.getInstance().getAllSubscriptionsOfFISMO(fismoID);
		Iterator<String> subscriptionModels = subscriptions.iterator();
		String jsonString = "\"UserIDs\": [\"";

		int len = subscriptions.size();
		int index = 0;
		for (; subscriptionModels.hasNext();) {
			String fismo = subscriptionModels.next();
			index++;
			jsonString += fismo;
			if (index < len) {
				jsonString += "\", \"";
			}
		}
		jsonString += "\"]";
		return jsonString;
	}

	public static String executeQueryForSubscriptions(String userID) throws HibernateException, Exception {
		List<String> subscriptions = SubscriptionStorage.getInstance().getAllSubscriptionsOfExperimenter(userID);
		Iterator<String> subscriptionModels = subscriptions.iterator();
		String jsonString = "\"FISMOIDs\": [\"";

		int len = subscriptions.size();
		int index = 0;
		for (; subscriptionModels.hasNext();) {
			String fismo = subscriptionModels.next();
			index++;
			jsonString += fismo;
			if (index < len) {
				jsonString += "\", \"";
			}
		}
		jsonString += "\"]";
		return jsonString;
	}

	public static String executeQueryForSubscriptionsInExperiment(String userID, String femoID)
			throws HibernateException, Exception {
		List<String> subscriptions = SubscriptionStorage.getInstance()
				.getAllSubscriptionsOfExperimenterInExperimentForFISMO(femoID, userID);
		Iterator<String> subs = subscriptions.iterator();
		String jsonString = "\"Subscriptions\": [";

		int len = subscriptions.size();
		int index = 0;
		for (; subs.hasNext();) {
			String subscr = subs.next();
			index++;
			jsonString += subscr;
			if (index < len) {
				jsonString += ", ";
			}
		}
		jsonString += "]";

		return jsonString;
	}

	public static String executeQueryForLogs(String jobID) throws HibernateException, Exception {
		List<CallLogModel> collectionslog = CallLogStorage.getInstance().getCalls(jobID);
		Iterator<CallLogModel> callLogModels = collectionslog.iterator();
		String jsonString = "\"ExecutionLog\": [";

		int len = collectionslog.size();
		int index = 0;
		for (; callLogModels.hasNext();) {
			CallLogModel callLogModel = callLogModels.next();
			index++;
			jsonString += "{\"executionTime\":" + callLogModel.getTimeConsumed() + ",\"dataConsumed\":\""
					+ callLogModel.getDataConsumed() + "\"}";
			if (index < len) {
				jsonString += ", ";
			}
		}
		jsonString += "]";
		return jsonString;
	}
}

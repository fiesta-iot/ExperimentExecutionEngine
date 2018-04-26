/*
===========================================================
This file is part of the FIESTA-IoT platform.
The FIESTA-IoT platform software contains proprietary and confidential information
of Inria. All rights reserved. Reproduction, adaptation or distribution, in
whole or in part, is forbidden except by express written permission of Inria.
Version v0.0.1, October 2016.
Authors: Rachit Agarwal.
Copyright (C) 2016, Inria.
===========================================================
*/
package eu.fiesta_iot.platform.eee.scheduler.dbUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import eu.fiesta_iot.platform.eee.scheduler.db.CallLogModel;

public class CallLogStorage {

	private static final Logger log = Logger.getLogger(CallLogStorage.class);
	
	public static CallLogStorage getInstance() {
		return SingletonHolder.INSTANCE;
	}

	/**
	 * Saves statistic information.
	 * 
	 * @param logging
	 * @return <code>true</code> if the method successfully saves statistics,
	 *         and <code>false</code> otherwise.
	 */
	public boolean save(CallLogModel logging) {
		log.debug("Starting Save");
		if (logging == null)
			return false;
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();
			session.beginTransaction();

			session.save(logging);

			session.getTransaction().commit();
			// session.close();
			log.debug("Done Save");
		} catch (HibernateException e) {
			log.error("",e);
			session.getTransaction().rollback();
			throw e;
		} finally {
			if (session != null)
				session.close();
		}

		log.debug("Exit Save");
		return true;
	}

	/**
	 * Gets a list of log calls.
	 * 
	 * @param FISMOID
	 * @return
	 */
	public List<CallLogModel> getCalls(String jobID) {
		Session session = null;
		try {
			List<CallLogModel> loggings = new ArrayList<CallLogModel>();
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "FROM CallLogModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				loggings.add((CallLogModel) obj);
			}
			return loggings;
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	/**
	 * Gets a list of log calls.
	 * 
	 * @param jobID
	 * @return
	 */
	public String getNumberOfCallsfor(String jobID) {
		String loggings = "";
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT count(fismoID) FROM CallLogModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				loggings = obj.toString();
			}
			return loggings;
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	/**
	 * Gets a list of log calls.
	 * 
	 * @param jobID
	 * @return
	 */
	public String getNumberOfCallsforUser(List<String> jobID, Date fromTime, Date toTime) {
		String loggings = "";
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT count(fismoID) FROM CallLogModel WHERE jobID in (:ids) and startTime BETWEEN :ftime and :stime";

			Query query = session.createQuery(sql).setParameterList("ids", jobID).setDate("ftime",fromTime).setDate("stime",toTime);

			List<?> list = query.list();

			for (Object obj : list) {
				loggings = obj.toString();
			}
			return loggings;
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} 
		finally {
			if (session != null)
				session.close();
		}
	}

	/** Singleton holder for lazy initiation */
	private static class SingletonHolder {
		private static final CallLogStorage INSTANCE = new CallLogStorage();
	}
}

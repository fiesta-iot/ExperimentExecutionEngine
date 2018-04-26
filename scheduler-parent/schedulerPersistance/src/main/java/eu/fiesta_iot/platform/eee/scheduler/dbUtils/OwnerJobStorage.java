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
import java.util.List;

import org.apache.log4j.Logger;
import org.hibernate.HibernateException;
import org.hibernate.Query;
import org.hibernate.Session;

import eu.fiesta_iot.platform.eee.scheduler.db.OwnerJobModel;

public class OwnerJobStorage {
	private static final Logger log = Logger.getLogger(OwnerJobStorage.class);
	
	public static OwnerJobStorage getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public boolean save(OwnerJobModel owner) {
		log.debug("Start Save");
		if (owner == null)
			return false;
		Session session = null;
		boolean ok = false;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			session.beginTransaction();

			session.save(owner);

			session.getTransaction().commit();
			ok = true;
			log.debug("Done Save");
		} catch (HibernateException he) {
			log.error("",he);
			session.getTransaction().rollback();
			throw he;
		} finally {
			if (session != null)
				session.close();
		}

		log.debug("Exit Save");
		return ok;
	}

	public boolean delete(String fismoID, String femoID, String userID) {
		log.debug("Start Delete");
		Session session = null;
		boolean ok = false;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			session.beginTransaction();

			String hql = "DELETE OwnerJobModel WHERE fismoID = ? AND femoID = ? AND experimenterID = ?";

			session.createQuery(hql).setString(0, fismoID).setString(1, femoID).setString(2, userID).executeUpdate();

			session.getTransaction().commit();
			ok = true;
			log.debug("Done Delete");
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			session.getTransaction().rollback();
			throw he;
		} finally {
			if (session != null)
				session.close();
		}

		log.debug("Exit Delete");
		return ok;
	}

	public boolean delete(String jobID) {
		log.debug("Start Delete");
		Session session = null;
		boolean ok = false;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			session.beginTransaction();

			String hql = "DELETE OwnerJobModel WHERE jobID = ?";
			session.createQuery(hql).setString(0, jobID).executeUpdate();

			session.getTransaction().commit();
			ok = true;
			log.debug("Done Delete");
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			session.getTransaction().rollback();
			throw he;
		} finally {
			if (session != null)
				session.close();
		}

		log.debug("Exit Delete");
		return ok;
	}

	public List<String> getAllFismosOfExperimenter(String experimenterID) {
		Session session = null;
		try {
			List<String> subscriptions = new ArrayList<String>();
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT DISTINCT fismoID FROM OwnerJobModel WHERE experimenterID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, experimenterID).list();

			for (Object obj : list) {
				subscriptions.add(obj.toString());
			}
			return subscriptions;
		} catch (HibernateException he) {
			log.error("Parameter userID:"+experimenterID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}

	}
	
	public List<String> getAllJobIDOfExperimenter(String experimenterID) {
		Session session = null;
		try {
			List<String> jobIDs = new ArrayList<String>();
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT DISTINCT jobID FROM OwnerJobModel WHERE experimenterID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, experimenterID).list();

			for (Object obj : list) {
				jobIDs.add(obj.toString());
			}
			return jobIDs;
		} catch (HibernateException he) {
			log.error("Parameter userID:"+experimenterID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}

	}
	

	public String getJobIDByUserIDForFISMO(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT jobID FROM OwnerJobModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "";
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	public String getJobIDForFISMO(String fismoID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT jobID FROM OwnerJobModel WHERE fismoID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, femoID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "";
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID:"+fismoID+", "+femoID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public String getJobIDForFISMO(String fismoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT jobID FROM OwnerJobModel WHERE fismoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "";
		} catch (HibernateException he) {
			log.error("Parameters fismoID:"+fismoID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public String getFEMOID(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT femoID FROM OwnerJobModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "";
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public String getUserID(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT userID FROM OwnerJobModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "";
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	public boolean existFISMOID(String fismoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT jobID FROM OwnerJobModel WHERE fismoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).list();

			if (!list.isEmpty())
				return true;
			return false;
		} catch (HibernateException he) {
			log.error("Parameters fismoID:"+fismoID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	public  boolean existFEMOID(String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT fismoID FROM OwnerJobModel WHERE femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, femoID).list();

			if (!list.isEmpty())
				return true;
			return false;
		} catch (HibernateException he) {
			log.error("Parameters femoID:"+femoID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	public  boolean existUserID(String userID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT fismoID FROM OwnerJobModel WHERE experimenterID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, userID).list();

			if (!list.isEmpty())
				return true;
			return false;
		} catch (HibernateException he) {
			log.error("Parameters userID:"+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	public  boolean existJobID(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT jobID FROM OwnerJobModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			if (!list.isEmpty())
				return true;
			return false;
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	/** Singleton holder for lazy initiation */
	private static class SingletonHolder {
		private static final OwnerJobStorage INSTANCE = new OwnerJobStorage();
	}

}

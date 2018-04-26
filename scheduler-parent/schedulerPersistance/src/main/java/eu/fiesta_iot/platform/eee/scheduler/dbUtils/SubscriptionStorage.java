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

import eu.fiesta_iot.platform.eee.scheduler.db.SubscriptionModel;

public class SubscriptionStorage {
	private static final Logger log = Logger.getLogger(SubscriptionStorage.class);
	
	public static SubscriptionStorage getInstance() {
		return SingletonHolder.INSTANCE;
	}

	public boolean save(SubscriptionModel subscribe) {
		log.debug("Start Save");
		if (subscribe == null)
			return false;
		Session session = null;
		boolean ok = false;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			session.beginTransaction();

			session.save(subscribe);

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

			String hql = "DELETE SubscriptionModel WHERE fismoID = ? AND femoID = ? AND experimenterID = ?";
			
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

	public boolean deleteSubscription(String jobID) {
		log.debug("Begin delete Subscription");
		Session session = null;
		boolean ok = false;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			session.beginTransaction();

			String hql = "DELETE SubscriptionModel WHERE jobID = ?";
			
			session.createQuery(hql).setString(0, jobID).executeUpdate();

			session.getTransaction().commit();
			ok = true;
			log.debug("Done delete Subscription");
		} catch (HibernateException he) {
			log.error("Parameter jobID:"+jobID+"\n",he);
			session.getTransaction().rollback();
			
			throw he;
		} finally {
			if (session != null)
				session.close();
		}

		log.debug("Exit delete Subscription");
		return ok;
	}

	public boolean delete(String fismoID) {
		log.debug("Start Delete");
		Session session = null;
		boolean ok = false;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			session.beginTransaction();

			String hql = "DELETE SubscriptionModel WHERE fismoID = ?";
			
			session.createQuery(hql).setString(0, fismoID).executeUpdate();

			session.getTransaction().commit();
			ok = true;
			log.debug("Done delete");
		} catch (HibernateException he) {
			log.debug("Parameter fismoID:"+fismoID+"\n",he);
			session.getTransaction().rollback();
			throw he;
		} finally {
			if (session != null)
				session.close();
		}

		log.debug("Exit delete");
		return ok;
	}

	public boolean deleteJob(String jobID) {
		log.debug("start delete job");
		Session session = null;
		boolean ok = false;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			session.beginTransaction();

			String hql = "DELETE SubscriptionModel WHERE jobID = ?";
			
			session.createQuery(hql).setString(0, jobID).executeUpdate();

			session.getTransaction().commit();
			ok = true;
			log.debug("Done delete job");
		} catch (HibernateException he) {
			log.error("Parameter jobID:"+jobID+"\n",he);
			session.getTransaction().rollback();
			throw he;
		} finally {
			if (session != null)
				session.close();
		}

		log.debug("Exit delete job");
		return ok;
	}

	public List<String> getAllSubscriptionsOfExperimenter(String experimenterID) {
		Session session = null;
		try {
			List<String> subscriptions = new ArrayList<String>();
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT DISTINCT fismoID FROM SubscriptionModel WHERE experimenterID = ?";

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

	public List<String> getAllSubscriptionsOfFISMO(String FISMOID) {
		Session session = null;
		try {
			List<String> subscriptions = new ArrayList<String>();
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT DISTINCT experimenterID FROM SubscriptionModel WHERE fismoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, FISMOID).list();

			for (Object obj : list) {
				subscriptions.add(obj.toString());
			}
			return subscriptions;
		} catch (HibernateException he) {
			log.error("Parameter fismoID:"+FISMOID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	public List<String> getAllSubscriptionsOfExperimenterInExperimentForFISMO(String femoID, String userID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT jobID, fismoID FROM SubscriptionModel WHERE femoID = ? and experimenterID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, femoID).setString(1, userID).list();
			final List<String> listJobsDetails = new ArrayList<String>();
			for (Object obj : list) {
				Object[] row = (Object[]) obj;
				String sb = "{\"jobID\":\"" + (String) row[0] + "\",\"fismoID\":\"" + (String) row[1] + "\"}";

				listJobsDetails.add(sb);
			}
			return listJobsDetails;
		} catch (HibernateException he) {
			log.error("Parameter femoID, userID:"+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	public boolean checkSubscriptionByUserIDForFISMO(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT jobID FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			if (!list.isEmpty())
				return true;
			return false;
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
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

			String sql = "SELECT jobID FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return null;
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public List<String> getAllJobIDofUserID(String userID) {
		Session session = null;
		try {
			List<String> jobIDs = new ArrayList<String>();
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT DISTINCT jobID FROM SubscriptionModel WHERE experimenterID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, userID).list();

			for (Object obj : list) {
				jobIDs.add(obj.toString());
			}
			return jobIDs;
		} catch (HibernateException he) {
			log.error("Parameters userID:"+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	public String getURL(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT url FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return null;
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	public String getURL(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT url FROM SubscriptionModel WHERE jobID = ?";

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

	public String getQuery(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT query FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

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

	public String getQuery(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT query FROM SubscriptionModel WHERE jobID = ?";

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
	
	public String getFileType(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT fileType FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "application/json";
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	public String getFileType(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT fileType FROM SubscriptionModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "application/json";
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}

	public boolean getReportIfEmpty(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT reportIfEmpty FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return (Boolean)obj;
			}
			return true;
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	public boolean getReportIfEmpty(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT reportIfEmpty FROM SubscriptionModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				 return (Boolean) obj;
			}
			return true;
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public String getOtherAttributes(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT otherAttributes FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "{}";
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public String getOtherAttributes(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT otherAttributes FROM SubscriptionModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "{}";
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public String getLongitude(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT geoLongitude FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "0";
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public String getLongitude(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT geoLongitude FROM SubscriptionModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "0";
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public String getLatitude(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT geoLatitude FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "0";
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public String getLatitude(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT geoLatitude FROM SubscriptionModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "0";
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	
	public Long getFromTime(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT fromTime FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return Long.parseLong(obj.toString());
			}
			return 0L;
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public Long getFromTime(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT fromTime FROM SubscriptionModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				return Long.parseLong(obj.toString());
			}
			return 0L;
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public Long getToTime(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT toTime FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return Long.parseLong(obj.toString());
			}
			return 0L;
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	public Long getToTime(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT toTime FROM SubscriptionModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				return Long.parseLong(obj.toString());
			}
			return 0L;
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public int getIntervalToPast(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT intervalToPast FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return Integer.parseInt(obj.toString());
			}
			return 0;
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	public int getIntervalToPast(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT intervalToPast FROM SubscriptionModel WHERE jobID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, jobID).list();

			for (Object obj : list) {
				return Integer.parseInt(obj.toString());
			}
			return 0;
		} catch (HibernateException he) {
			log.error("Parameters jobID:"+jobID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	
	public String getKatInput(String fismoID, String userID, String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT kATInput FROM SubscriptionModel WHERE fismoID = ? and experimenterID = ? and femoID = ?";

			Query query = session.createQuery(sql);

			List<?> list = query.setString(0, fismoID).setString(1, userID).setString(2, femoID).list();

			for (Object obj : list) {
				return obj.toString();
			}
			return "{\"Method\":[\"\"], \"Parameters\":[\"\"]}";
		} catch (HibernateException he) {
			log.error("Parameters fismoID, femoID, userID:"+fismoID+", "+femoID+", "+userID+"\n",he);
			throw he;
		} finally {
			if (session != null)
				session.close();
		}
	}
	public String getKatInput(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT kATInput FROM SubscriptionModel WHERE jobID = ?";

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
	
	public String getFEMOID(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT femoID FROM SubscriptionModel WHERE jobID = ?";

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

			String sql = "SELECT experimenterID FROM SubscriptionModel WHERE jobID = ?";

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
	
	public static boolean existFISMOID(String fismoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT jobID FROM SubscriptionModel WHERE fismoID = ?";

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

	public static boolean existFEMOID(String femoID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT fismoID FROM SubscriptionModel WHERE femoID = ?";

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
	
	public boolean existJobID(String jobID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT jobID FROM SubscriptionModel WHERE jobID = ?";

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

	public static boolean existUserID(String userID) {
		Session session = null;
		try {
			session = HibernateUtil.getSessionFactory().openSession();

			String sql = "SELECT fismoID FROM SubscriptionModel WHERE experimenterID = ?";

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

	/** Singleton holder for lazy initiation */
	private static class SingletonHolder {
		private static final SubscriptionStorage INSTANCE = new SubscriptionStorage();
	}

}

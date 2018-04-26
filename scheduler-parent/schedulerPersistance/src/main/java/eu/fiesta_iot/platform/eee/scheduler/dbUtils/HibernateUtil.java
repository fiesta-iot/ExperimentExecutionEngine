/*
===========================================================
This file is part of the FIESTA-IoT platform.
The FIESTA-IoT platform software contains proprietary and confidential information
of Inria. All rights reserved. Reproduction, adaptation or distribution, in
whole or in part, is forbidden except by express written permission of Inria.
Version v0.0.1, October 2016.
Authors: Modified by Rachit Agarwal.
Copyright (C) 2016, Inria.
===========================================================
*/
package eu.fiesta_iot.platform.eee.scheduler.dbUtils;

import org.apache.log4j.Logger;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.AnnotationConfiguration;

public class HibernateUtil {
	private static final SessionFactory sessionFactory = buildSessionFactory();
	private static final Logger log = Logger.getLogger(HibernateUtil.class.getName());
	private static SessionFactory buildSessionFactory() {
		try {
			// Create the SessionFactory from hibernate.cfg.xml
			return new AnnotationConfiguration().configure().buildSessionFactory();

		} catch (Throwable ex) {
			// Make sure you log the exception, as it might be swallowed
			log.error("Initial SessionFactory creation failed.", ex);
			throw new ExceptionInInitializerError(ex);
		}
	}

	public static SessionFactory getSessionFactory() {
		return sessionFactory;
	}

	public static void shutdown() {
		// Close caches and connection pools
		getSessionFactory().close();
	}
}

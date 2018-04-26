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

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.eee.scheduler.impl.JobScheduler;

@ApplicationPath("/")
public class JaxRsActivator extends Application {

	final static Logger logger = LoggerFactory.getLogger(JaxRsActivator.class);

	Set<Object> singletons = new HashSet<Object>();
	Set<Class<?>> empty = new HashSet<Class<?>>();

	public JaxRsActivator() {
		logger.info("************constructor**********");
		singletons.add(new JobScheduler());
		singletons.add(new SchedulerServices());
		singletons.add(new PollingServices());
		singletons.add(new SubscriptionServices());
		singletons.add(new MonitoringServices());
		singletons.add(new AccountingServices());
	}

	public Set<Class<?>> getClasses() {
		return empty;
	}

	public Set<Object> getSingletons() {
		logger.info("**********************");
		return singletons;
	}
}

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

import java.io.Serializable;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TimeSchedule implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	final static Logger logger = LoggerFactory.getLogger(TimeSchedule.class);
	/**
	 * The start time.
	 */
	private Date startTime;

	/**
	 * The the stop time.
	 */
	private Date stopTime;

	/**
	 * The the frequency.
	 */
	private int periodicity;

	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	public Date getStopTime() {
		return stopTime;
	}

	public void setStopTime(Date stopTime) {
		this.stopTime = stopTime;
	}

	public int getPeriodicity() {
		return periodicity;
	}

	public void setPeriodicity(int periodicity) {
		this.periodicity = periodicity;
	}

	public TimeSchedule() {

	}

	public TimeSchedule(Date startTime, Date stopTime, int periodicity) {
		this.startTime = startTime;
		this.stopTime = stopTime;
		this.periodicity = periodicity;
	}

	public boolean hasStopTime() {
		return this.stopTime != null;
	}

	public boolean hasStartTime() {
		return this.startTime != null;
	}

	public boolean hasPeriodicity() {
		return this.periodicity > 0;
	}
}

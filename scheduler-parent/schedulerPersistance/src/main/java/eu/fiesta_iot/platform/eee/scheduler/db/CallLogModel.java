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

package eu.fiesta_iot.platform.eee.scheduler.db;

import static javax.persistence.GenerationType.IDENTITY;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "logcall")
public class CallLogModel implements java.io.Serializable {

	private static final long serialVersionUID = -4453661883917543487L;

	private Integer id;
	private String fismoID;
	private String jobID;
	private Date startTime;
	private long timeConsumed;
	private String dataConsumed;

	public CallLogModel() {
		startTime = Calendar.getInstance().getTime();
	}

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "fismoID", unique = false, nullable = false, length = 128)
	public String getFismoID() {
		return fismoID;
	}

	public void setFismoID(String fismoID) {
		this.fismoID = fismoID;
	}

	@Column(name = "jobID", unique = false, nullable = false, length = 128)
	public String getJobID() {
		return jobID;
	}

	public void setJobID(String jobID) {
		this.jobID = jobID;
	}

	@Column(name = "startTime", unique = false, nullable = false)
	public Date getStartTime() {
		return startTime;
	}

	public void setStartTime(Date startTime) {
		this.startTime = startTime;
	}

	@Column(name = "timeConsumed", unique = false, nullable = false)
	public long getTimeConsumed() {
		return timeConsumed;
	}

	public void setTimeConsumed(long timeConsumed) {
		this.timeConsumed = timeConsumed;
	}

	@Column(name = "dataConsumed", unique = false, nullable = false)
	public String getDataConsumed() {
		return dataConsumed;
	}

	public void setDataConsumed(String dataConsumed) {
		this.dataConsumed = dataConsumed;
	}
}

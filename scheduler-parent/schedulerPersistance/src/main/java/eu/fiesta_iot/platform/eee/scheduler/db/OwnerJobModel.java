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

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name = "owner", uniqueConstraints = @UniqueConstraint(columnNames = { "fismoID", "femoID", "experimenterID" }))
public class OwnerJobModel implements Serializable {

	private static final long serialVersionUID = -44561883917543487L;

	private Integer id;
	private String fismoID;
	private String femoID;
	private String jobID;
	private String experimenterID;

	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "fismoID", unique = false, nullable = false)
	public String getFismoID() {
		return fismoID;
	}

	public void setFismoID(String fISMOID) {
		this.fismoID = fISMOID;
	}

	@Column(name = "femoID", unique = false, nullable = false)
	public String getFemoID() {
		return femoID;
	}

	public void setFemoID(String fEMOID) {
		this.femoID = fEMOID;
	}

	@Column(name = "jobID", unique = false, nullable = false, length = 128)
	public String getJobID() {
		return jobID;
	}

	public void setJobID(String jobID) {
		this.jobID = jobID;
	}

	@Column(name = "experimenterID", unique = false, nullable = false)
	public String getExperimenterID() {
		return experimenterID;
	}

	public void setExperimenterID(String experimenterID) {
		this.experimenterID = experimenterID;
	}

	public OwnerJobModel() {
	}
}

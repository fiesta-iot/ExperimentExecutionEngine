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
@Table(name = "subscribe", uniqueConstraints = @UniqueConstraint(columnNames = { "fismoID", "femoID",
		"experimenterID" }))
public class SubscriptionModel implements Serializable {

	private static final long serialVersionUID = -44561883917543487L;

	private Integer id;
	private String fismoID;
	private String femoID;
	private String jobID;
	private String experimenterID;
	private String url;
	private String query;
	private String optionalFile;
	private String optionalWidget;
	private boolean reportIfEmpty;
	private String fileType;
	private String kATInput; 
	
	private String geoLatitude;
	private String geoLongitude;
	private Long fromTime;
	private Long toTime;
	private int intervalNowToPast;
	private String otherAttributes;
	
	@Id
	@GeneratedValue(strategy = IDENTITY)
	@Column(name = "id", unique = true, nullable = false)
	public Integer getId() {
		return this.id;
	}

	public void setId(Integer id) {
		this.id = id;
	}

	@Column(name = "url", unique = false, nullable = false)
	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	@Column(name = "file", unique = false, nullable = true)
	public String getOptionalFile() {
		return optionalFile;
	}

	public String getOptionalWidget() {
		return optionalWidget;
	}

	@Column(name = "widget", unique = false, nullable = true)
	public void setOptionalWidget(String optionalWidget) {
		this.optionalWidget = optionalWidget;
	}

	public void setOptionalFile(String optionalFile) {
		this.optionalFile = optionalFile;
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

	@Column(name = "query", unique = false, nullable = false, length = 4000)
	public String getQuery() {
		return query;
	}

	public void setquery(String query) {
		this.query = query;
	}
	
	@Column(name = "filetype", unique = false, nullable = false)
	public String getFileType() {
		return fileType;
	}

	public void setFileType(String fileType) {
		this.fileType = fileType;
	}

	@Column(name = "reportIfEmpty", unique = false, nullable = false)
	public boolean getReportIfEmpty() {
		return reportIfEmpty;
	}

	public void setReportIfEmpty(boolean reportIfEmpty) {
		this.reportIfEmpty = reportIfEmpty;
	}
	
	
	@Column(name = "kATInput", unique = false, nullable = false, length = 4000)
	public String getkATInput() {
		return kATInput;
	}

	public void setkATInput(String kATInput) {
		this.kATInput = kATInput;
	}

	@Column(name = "geoLatitude", unique = false, nullable = true)
	public String getGeoLatitude() {
		return geoLatitude;
	}

	public void setGeoLatitude(String geoLatitude) {
		this.geoLatitude = geoLatitude;
	}

	@Column(name = "geoLongitude", unique = false, nullable = true)
	public String getGeoLongitude() {
		return geoLongitude;
	}

	public void setGeoLongitude(String geoLongitude) {
		this.geoLongitude = geoLongitude;
	}

	@Column(name = "fromTime", unique = false, nullable = true)
	public Long getFromTime() {
		return fromTime;
	}

	public void setFromTime(Long fromTime) {
		this.fromTime = fromTime;
	}

	@Column(name = "toTime", unique = false, nullable = true)
	public Long getToTime() {
		return toTime;
	}

	public void setToTime(Long toTime) {
		this.toTime = toTime;
	}

	@Column(name = "intervalNowToPast", unique = false, nullable = true)
	public int getIntervalNowToPast() {
		return intervalNowToPast;
	}

	public void setIntervalNowToPast(int intervalNowToPast) {
		this.intervalNowToPast = intervalNowToPast;
	}

	@Column(name = "otherAttributes", unique = false, nullable = true, length = 4000)
	public String getOtherAttributes() {
		return otherAttributes;
	}

	public void setOtherAttributes(String otherAttributes) {
		this.otherAttributes = otherAttributes;
	}
	
	public SubscriptionModel() {
	}
}

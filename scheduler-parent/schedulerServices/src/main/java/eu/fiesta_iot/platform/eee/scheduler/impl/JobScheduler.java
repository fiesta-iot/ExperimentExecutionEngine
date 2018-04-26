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

import static org.quartz.JobBuilder.newJob;

import java.io.IOException;
import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.hibernate.HibernateException;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.Trigger.TriggerState;
import org.quartz.TriggerBuilder;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.fiesta_iot.platform.eee.scheduler.dbUtils.OwnerJobStorage;
import eu.fiesta_iot.platform.eee.scheduler.dbUtils.SubscriptionStorage;

public class JobScheduler {

	final static Logger logger = LoggerFactory.getLogger(JobScheduler.class);

	private TimeSchedule timeSchedule;
	private String jobID;
	private String groupID;

	private static Properties props = new Properties();
	private static SchedulerFactory schFactory;
	private static Scheduler scheduler;

	private static void init() {
		try {
			logger.debug("Job Scheduler setup initializing.");
			InputStream in = JobScheduler.class.getResourceAsStream(Constants.quartzPropertyFilePath);
			props.load(in);
			in.close();
			schFactory = new StdSchedulerFactory(props);
			scheduler = schFactory.getScheduler();
			reschedule();
		} catch (IOException e) {
			logger.error("IO Exception: quartz.properties not found", e);
		} catch (SchedulerException se) {
			logger.error("Scheduler Exception:\n", se);
		}
	}

	public static void destroy() {
		try {
			scheduler.shutdown();
		} catch (SchedulerException se) {
			logger.error("Scheduler Exception:\n", se);
		} catch (Exception e) {
			logger.error("", e);
		}
	}

	public JobScheduler() {
		init();
	}

	/**
	 * Constructs a JobScheduler.
	 * 
	 * @param timeSchedule
	 *            the timeschedule.
	 * @param id
	 *            the id.
	 * @param sensorIDs
	 *            the sensor IDs list.
	 */
	public JobScheduler(TimeSchedule timeSchedule, String jobID, String groupID) {
		logger.debug("Job Scheduler initializing.");
		this.timeSchedule = timeSchedule;
		this.jobID = jobID;
		this.groupID = groupID;
	}

	/**
	 * Pauses the given scheduled Job.
	 * 
	 * @param id
	 *            the id of the scheduled job that should get paused.
	 */
	public static Response pauseScheduledJob(String jobID) {
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					if (jobKey.toString().compareToIgnoreCase(group + "." + jobID) == 0) {
						scheduler.pauseJob(jobKey);
						logger.debug("Job id: " + jobID + " status: paused");
						return Response.ok("{\"response\" : \"Job paused successfully.\"}", MediaType.APPLICATION_JSON)
								.build();
					}
				}
			}
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
		return ErrorCodes.noSuchJobID(jobID);
	}

	/**
	 * Resumes the given scheduled Job.
	 * 
	 * @param id
	 *            the id of the scheduled job that should get resumed.
	 */
	public static Response resumeScheduledJob(String jobID) {
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					if (jobKey.toString().compareToIgnoreCase(group + "." + jobID) == 0) {
						scheduler.resumeJob(jobKey);
						logger.debug("Job id: " + jobID + " status: normal");
						return Response.ok("{\"response\" : \"Job resumed successfully.\"}", MediaType.APPLICATION_JSON)
								.build();
					}
				}
			}
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
		return ErrorCodes.noSuchJobID(jobID);
	}

	/**
	 * Returns a list containing all currently executing jobs, if any.
	 * 
	 * @return a list containing all currently executing jobs.
	 */
	public static Response currentlyExecutingJobs() {
		try {
			String listJobs = scheduler.getCurrentlyExecutingJobs().toString();
			logger.debug("Currently Executing Job: " + listJobs);
			return Response.ok("{\"response\" : \"Currently Executing Jobs.\", \n\"Jobs\" : \"" + listJobs + "\"}",
					MediaType.APPLICATION_JSON).build();
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	/**
	 * Checks if the scheduler has any scheduled job.
	 * 
	 * @return true if the scheduler has at least one scheduled job.
	 */
	@SuppressWarnings("unused")
	private static boolean hasScheduledJobs() {
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					logger.debug("Scheduler has scheduled jobs");
					return true;
				}
			}
		} catch (SchedulerException e) {
			logger.error("Scheduler Exception:", e);
		} catch (Exception e) {
			logger.error("Implementation Exception: ", e);
		}
		return false;
	}

	/**
	 * Gets details for the scheduled jobs.
	 * 
	 * @return the details for the scheduled jobs.
	 */
	public static Response getScheduledJobsWithDetails() {
		boolean scheduledJobsExist = false;
		Map<String, Object> jsonList = new HashMap<>();
		List<Object> listJobsDetails = new ArrayList<>();
		try {
			DateFormat DATE_FORMAT = new SimpleDateFormat(Constants.timeFormat);
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					//logger.debug("[Job key]: " + jobKey);
					Map<String, Object> metadata = new HashMap<>();

					@SuppressWarnings("unchecked")
					List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
					if (!triggers.isEmpty()) {
						Trigger trigger = triggers.get(0);

						metadata.put("jobID", jobKey.getName());
						metadata.put("Group", jobKey.getGroup());
						Map<String, Object> timeSchedule = new HashMap<>();
						timeSchedule.put("startTime", DATE_FORMAT.format(trigger.getStartTime()));
						timeSchedule.put("stopTime",DATE_FORMAT.format(trigger.getEndTime()));
						timeSchedule.put("periodicity",
								(trigger.getNextFireTime().getTime() - trigger.getPreviousFireTime().getTime()) / 1000);
						metadata.put("timeSchedule", timeSchedule);

						// job state
						TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
						metadata.put("state",triggerState.toString());
						listJobsDetails.add(metadata);
						scheduledJobsExist = true;
					}
				}
			}
			if (scheduledJobsExist) {
				logger.debug("Scheduled Jobs : " + listJobsDetails.toString());
				jsonList.put("JobsScheduled", listJobsDetails);
				return Response.ok(jsonList, MediaType.APPLICATION_JSON).build();
			} else {
				logger.debug("No Scheduled Jobs. ");
				jsonList.put("response", "No Scheduled Jobs.");
				return Response.ok(jsonList, MediaType.APPLICATION_JSON).build();
			}
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	/**
	 * Gets details for the scheduled jobs.
	 * 
	 * @return the details for the scheduled jobs.
	 */
	public static Response getScheduledJobsIDs() {
		boolean scheduledJobsExist = false;
		Map<String, Object> map = new HashMap<>();
		List<Object> listIDs = new ArrayList<>();
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					logger.debug("[Job key]: " + jobKey);
					Map<String, Object> metadata = new HashMap<>();
					metadata.put("jobID", jobKey.getName());
					metadata.put("FISMOID", jobKey.getGroup());
					listIDs.add(metadata);
					scheduledJobsExist = true;
				}
			}
			if (scheduledJobsExist) {
				logger.debug("Scheduled Jobs exist.");
				map.put("jobIDs", listIDs);
				return Response.ok(map, MediaType.APPLICATION_JSON).build();
			} else {
				logger.debug("No Scheduled Jobs.");
				map.put("response", "No Scheduled Jobs.");
				return Response.ok(map, MediaType.APPLICATION_JSON).build();
			}
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	/**
	 * Gets details for the scheduled job with the given id.
	 * 
	 * @return the details for the scheduled job with the given id.
	 */
	public static Response getScheduledJobMetadata(String id) {
		logger.debug("Get details for job with id " + id);
		boolean scheduledJobsExist = false;
		Map<String, Object> map = new HashMap<>();
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					if (jobKey.toString().compareToIgnoreCase(group + "." + id) == 0) {
						DateFormat DATE_FORMAT = new SimpleDateFormat(Constants.timeFormat);

						@SuppressWarnings("unchecked")
						List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
						if (!triggers.isEmpty()) {
							Trigger trigger = triggers.get(0);

							map.put("JobID", id);
							map.put("Group",jobKey.getGroup());
							Map<String, Object> timeSchedule = new HashMap<>();
							timeSchedule.put("startTime", DATE_FORMAT.format(trigger.getStartTime()));
							timeSchedule.put("stopTime", DATE_FORMAT.format(trigger.getEndTime()));
							timeSchedule.put("periodicity",
									(trigger.getNextFireTime().getTime() - trigger.getPreviousFireTime().getTime())
											/ 1000);
							map.put("timeSchedule", timeSchedule);
							// job state
							TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());

							map.put("state", triggerState.toString());
							scheduledJobsExist = true;
						}
					}
				}
			}
			if (scheduledJobsExist) {
				logger.debug("Scheduled Jobs : " + map.toString());
				return Response.ok(map, MediaType.APPLICATION_JSON).build();
			} else {
				logger.debug("No Scheduled Jobs. ");
				map.put("response", "No Job information found.");
				return Response.ok(map, MediaType.APPLICATION_JSON).build();
			}
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	/**
	 * Gets details for the scheduled job with the given id. state will be from
	 * [BLOCKED, COMPLETE, ERROR, NONE, NORMAL, PAUSED]
	 * 
	 * @return the details for the scheduled job with the given id.
	 */
	public static Response getScheduledJobStatus(String id) {
		logger.debug("Get status for job with id " + id);
		boolean scheduledJobsExist = false;
		Map<String, Object> map = new HashMap<>();
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					if (jobKey.toString().compareToIgnoreCase(group + "." + id) == 0) {
						@SuppressWarnings("unchecked")
						List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
						if (!triggers.isEmpty()) {
							Trigger trigger = triggers.get(0);
							TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
							map.put("JobID", id);
							map.put("state", triggerState.toString());
							scheduledJobsExist = true;
						}
					}
				}
			}
			if (scheduledJobsExist) {
				logger.debug("Scheduled Jobs : " + map.toString());
				return Response.ok(map, MediaType.APPLICATION_JSON).build();
			} else {
				logger.debug("No Scheduled Jobs.");
				map.put("response", "Job Not Scheduled.");
				return Response.ok(map, MediaType.APPLICATION_JSON).build();
			}
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	/**
	 * Gets details for the scheduled job with the given id. state will be from
	 * [BLOCKED, COMPLETE, ERROR, NONE, NORMAL, PAUSED]
	 * 
	 * @return the details for the scheduled job with the given id.
	 */
	public static Response getAllScheduledJobStatus() {
		logger.debug("Get status for all jobs ");
		Map<String, Object> jsonList = new HashMap<>();
		List<Object> listJobsDetails = new ArrayList<>();
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					Map<String, Object> map = new HashMap<>();
					@SuppressWarnings("unchecked")
					List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
					if (!triggers.isEmpty()) {
						Trigger trigger = triggers.get(0);
						TriggerState triggerState = scheduler.getTriggerState(trigger.getKey());
						map.put("JobID", jobKey.getName());
						map.put("state", triggerState.toString());
						listJobsDetails.add(map);
					}
				}
			}
			if (listJobsDetails.isEmpty()) {
				jsonList.put("response", "No Jobs");
				return Response.ok(jsonList, MediaType.APPLICATION_JSON).build();
			}
			jsonList.put("JobsStatus",listJobsDetails);
			return Response.ok(jsonList, MediaType.APPLICATION_JSON).build();
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	/**
	 * Deletes the given scheduled Job.
	 * 
	 * @param id
	 *            the id of the scheduled job that should get deleted.
	 */
	public static Response deleteScheduledJob(String jobID) {
		boolean scheduledJobsExist = false;
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					if (jobKey.toString().compareToIgnoreCase(group + "." + jobID) == 0) {
						scheduler.deleteJob(jobKey);
						if (OwnerJobStorage.getInstance().existJobID(jobID)) OwnerJobStorage.getInstance().delete(jobID);
						else if (SubscriptionStorage.getInstance().existJobID(jobID)) SubscriptionStorage.getInstance().deleteJob(jobID);
						logger.debug("[Job id]: " + jobID + " deleted");
						scheduledJobsExist = true;
					}
				}
			}
			if (scheduledJobsExist) {
				logger.debug("Scheduled Jobs : " + jobID);
				return Response.ok("{\"response\" : \"Job deleted successfully.\"}", MediaType.APPLICATION_JSON)
						.build();
			} else {
				logger.debug("No Scheduled Jobs. ");
				return ErrorCodes.noSuchJobID(jobID);
				// return Response.ok("{\"response\" : \"No Job found.\"}",
				// MediaType.APPLICATION_JSON).build();
			}
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	/**
	 * Deletes all the scheduled Jobs. use this before shutting down the
	 * scheduler for graceful shutdown
	 * 
	 */
	public static Response deleteAllScheduledJobs() {
		logger.debug("Shutting down all running scheduled jobs.");
		boolean scheduledJobsExist = false;
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					scheduler.deleteJob(jobKey);
					if (OwnerJobStorage.getInstance().existJobID(jobKey.getName())) OwnerJobStorage.getInstance().delete(jobKey.getName());
					else if (SubscriptionStorage.getInstance().existJobID(jobKey.getName())) SubscriptionStorage.getInstance().deleteJob(jobKey.getName());
					//OwnerJobStorage.getInstance().delete(jobKey.getName());
					//SubscriptionStorage.getInstance().delete(jobKey.getGroup());
					scheduledJobsExist = true;
				}
			}
			if (scheduledJobsExist) {
				logger.debug("Scheduled Jobs ");
				return Response.ok("{\"response\" : \"All Job deleted successfully.\"}", MediaType.APPLICATION_JSON)
						.build();
			} else {
				logger.debug("No Scheduled Jobs. ");
				return Response.ok("{\"response\" : \"No Jobs found.\"}", MediaType.APPLICATION_JSON).build();
			}
		} catch (HibernateException e) {
			return ErrorCodes.persistenceException(e);
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	public static boolean reschedule() {
		logger.debug("Initial Rescheduling Jobs ");
		boolean scheduledJobsExist = false;
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					logger.debug("Initial Rescheduling Jobs " + jobKey + " " + group);
					for (Trigger trigger : scheduler.getTriggersOfJob(jobKey)) {
						logger.debug("Initial Rescheduling Jobs " + trigger.getKey());
						if (!scheduler.checkExists(trigger.getKey())) {
							scheduler.rescheduleJob(trigger.getKey(), trigger);
							logger.debug(" Rescheduled " + jobKey.toString());
							scheduledJobsExist = true;
						}
					}
				}
			}
			if (scheduledJobsExist) {
				logger.debug("Scheduled Jobs ");
				return true;
			} else {
				logger.debug("No jobs to be Scheduled. ");
				return true;
			}
		} catch (SchedulerException e) {
			logger.error("Scheduler Exception:", e);
		} catch (Exception e) {
			logger.error("Exception:", e);
		}
		logger.debug("All Jobs successfully rescheduled.");
		logger.debug("DONE.");
		return false;
	}

	public static Response rescheduleJob(String id, Date startTime, Date stopTime, int periodicity) {
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					if (jobKey.toString().compareToIgnoreCase(group + "." + id) == 0) {
						@SuppressWarnings("unchecked")
						List<Trigger> triggers = (List<Trigger>) scheduler.getTriggersOfJob(jobKey);
						Trigger oldTrigger = triggers.get(0);

						logger.debug(startTime.toString());
						Trigger newTrigger = TriggerBuilder.newTrigger()
								.withIdentity(jobKey.getName(), jobKey.getGroup()).startAt(startTime)
								.withSchedule(SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(periodicity)
										.repeatForever())
								.endAt(stopTime).build();
						logger.debug(newTrigger.getStartTime().toString() + newTrigger.getEndTime().toString());

						scheduler.rescheduleJob(oldTrigger.getKey(), newTrigger);

						return Response
								.ok("{\"response\" : \"Job rescheduled successfully.\"}", MediaType.APPLICATION_JSON)
								.build();
					}
				}
			}
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
		return ErrorCodes.noSuchJobID(id);
	}

	public static Response getScheduledJobIDsForFISMOID(String fISMOID) {
		Map<String, Object> jsonList = new HashMap<>();
		List<Object> listJobsDetails = new ArrayList<>();
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					if (jobKey.getGroup().equals(fISMOID))
						listJobsDetails.add(jobKey.getName());
				}
			}
			if (listJobsDetails.isEmpty()) {
				jsonList.put("response", "No Jobs");
				return Response.ok(jsonList, MediaType.APPLICATION_JSON).build();
			}
			jsonList.put("jobsIDs", listJobsDetails);
			return Response.ok(jsonList, MediaType.APPLICATION_JSON).build();
		} catch (SchedulerException e) {
			return ErrorCodes.schedulerException(e);
		} catch (Exception e) {
			return ErrorCodes.implementationException(e);
		}
	}

	public static Response getScheduledJobIDsForFISMOIDandUserID(String fismoID, String userID, String femoID,
			boolean owner) {
		Map<String,Object> map = new HashMap<>();
		String jobID = "";
		try {
			if (owner) {
				jobID = OwnerJobStorage.getInstance().getJobIDByUserIDForFISMO(fismoID, userID, femoID);
			} else {
				jobID = SubscriptionStorage.getInstance().getJobIDByUserIDForFISMO(fismoID, userID, femoID);
			}
			if (!jobID.isEmpty())
				map.put("jobID",jobID);
			else
				map.put("response", "No JobID");
			return Response.ok(map, MediaType.APPLICATION_JSON).build();
		} catch (HibernateException he) {
			return ErrorCodes.persistenceException(he);
		}
	}

	/**
	 * Initializes the scheduled Job and schedules its trigger event.
	 */
	public void scheduleJob() throws SchedulerException, Exception {
		JobDetail job = newJob(SchedulerHelper.class).withIdentity(jobID, groupID).build();

		job.getJobDataMap().put("timeSchedule", timeSchedule);
		Trigger trigger = buildTrigger(job);

		scheduler.scheduleJob(job, trigger);
		scheduler.start();
	}

	/**
	 * Builds a Trigger for the scheduled job.
	 * 
	 * @param job
	 *            the scheduled job.
	 * @return returns a Trigger for the scheduled job
	 */
	private Trigger buildTrigger(JobDetail job) {
		try {
			int periodicity = 0;
			logger.debug("Inside Builder");
			if (!timeSchedule.hasStartTime()) {
				logger.error("Failed to schedule the job. Start Date not specified");
				return null;
			}

			if (!timeSchedule.hasPeriodicity()) {
				logger.error("Failed to schedule the job. Periodicity not specified");
				return null;
			} else {
				periodicity = timeSchedule.getPeriodicity();
			}

			if (timeSchedule.hasStopTime()) {

				logger.debug("Job started id :  " + jobID);

				// repeat forever is shadowed by endtime. repeat will run the
				// job for specified time only
				return TriggerBuilder.newTrigger().withIdentity(jobID, groupID)
						.startAt(timeSchedule.getStartTime()).withSchedule(SimpleScheduleBuilder.simpleSchedule()
								.withIntervalInSeconds(periodicity).repeatForever())
						.endAt(timeSchedule.getStopTime()).build();
			} else {
				logger.debug("Job started id :  " + jobID);

				return TriggerBuilder.newTrigger().withIdentity(jobID, groupID).startAt(timeSchedule.getStartTime())
						.withSchedule(SimpleScheduleBuilder.simpleSchedule()
								.withIntervalInSeconds(timeSchedule.getPeriodicity()).repeatForever())
						.build();
			}
		} catch (Exception e) {
			logger.error("Exception: ",e);
		}
		return null;
	}

	public static boolean existJobID(String jobID) {
		try {
			for (String group : scheduler.getJobGroupNames()) {
				for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.groupEquals(group))) {
					if (jobKey.getName().toString().equals(jobID))
						return true;
				}
			}
			return false;
		} catch (SchedulerException e) {
			logger.error("Scheduler Exception:",e);
			return false;
		} catch (Exception e) {
			logger.error("Exception:",e);
			return false;
		}
	}

}

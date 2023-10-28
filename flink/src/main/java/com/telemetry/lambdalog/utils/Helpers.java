package com.telemetry.lambdalog.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class Helpers
{

	private static final Logger logger = LoggerFactory.getLogger(Helpers.class);

	private Helpers()
	{
	}

	// Determine when to clear the state
	public static Long getTimeToClearState(int days)
	{
		Instant now = Instant.now();
		Instant futureInstant = now.plus(days, ChronoUnit.DAYS);
		long milliseconds = futureInstant.toEpochMilli();

		logger.info("Time for to trigger timer: " + milliseconds);
		return milliseconds;
	}

}

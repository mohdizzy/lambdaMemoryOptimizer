package com.telemetry.lambdalog.functions;

import com.jayway.jsonpath.JsonPath;
import com.telemetry.lambdalog.utils.Helpers;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import org.apache.flink.api.common.state.*;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;
import java.util.stream.StreamSupport;

/**
 * ProcessFunction is a Flink operator that is used process every incoming Flink event This function holds the logic for
 * pattern detection, builds the output structure, manages state and clears the state using timer.
 */

public class LogProcessFunction extends KeyedProcessFunction<String, String, String>
{
	private static final ObjectMapper mapper = new ObjectMapper();

	private static final Logger logger = LoggerFactory.getLogger(LogProcessFunction.class);

	private MapState<String, Integer> lambdaMemory;

	private ValueState<String> lambdaName;

	@Override
	public void open(org.apache.flink.configuration.Configuration parameters) throws Exception
	{
		super.open(parameters);
		MapStateDescriptor<String, Integer> lambdaMemoryStateDescriptor = new MapStateDescriptor<>("lambdaState",
				String.class, Integer.class);
		ValueStateDescriptor<String> lambdaNameStateDescriptor = new ValueStateDescriptor<>("lambdaName",
				String.class);

		lambdaMemory = getRuntimeContext().getMapState(lambdaMemoryStateDescriptor);
		lambdaName = getRuntimeContext().getState(lambdaNameStateDescriptor);
	}

	@Override
	public void processElement(String input, Context ctx, Collector<String> out) throws Exception
	{

		Object document = Configuration.defaultConfiguration().jsonProvider().parse(input);
		logger.info("Lambda logs received: "+mapper.writeValueAsString(input));
		try
		{

			List<Integer> memoryUsedBasedLogMetrics = JsonPath.read(document,
					"$.records[?(@.type=='platform.report')].record.metrics.maxMemoryUsedMB");

			// get the max memory used if log contains multiple invocations
			int maxMemoryUsedBasedOnLogInvocations = Collections.max(memoryUsedBasedLogMetrics);

			boolean isStateEmpty = lambdaMemory.isEmpty();


			if (isStateEmpty) {
				String functionName = JsonPath.read(document,"$.functionName");
				lambdaName.update(functionName);
				// set timer to 2 weeks from now if state is empty
				// this timer will send out the payload containing the memory to update for a given function
				ctx.timerService().registerProcessingTimeTimer(Helpers.getTimeToClearState(14));
			}


			ZonedDateTime utc = ZonedDateTime.now(ZoneId.of("UTC"));
			String currentDate = utc.toString().split("T")[0];
			boolean isCurrentDateLoggedInState = !isStateEmpty && lambdaMemory.contains(currentDate);
			if (isCurrentDateLoggedInState)
			{
				int memoryLoggedInStateForCurrentDate = lambdaMemory.get(currentDate);
				if (memoryLoggedInStateForCurrentDate > maxMemoryUsedBasedOnLogInvocations) return;
			}

			logger.info("Logging maxMemory used for the date: "+currentDate+ ", memory: "+maxMemoryUsedBasedOnLogInvocations);
			lambdaMemory.put(currentDate,maxMemoryUsedBasedOnLogInvocations);

			// uncomment this line for tests
//			ctx.timerService().registerProcessingTimeTimer(Instant.now().toEpochMilli() + 2000);
		}
		catch (Exception e)
		{
			logger.error("Something went wrong while processing" + e);
		}
	}

	@Override
	public void onTimer(long timestamp, OnTimerContext ctx, Collector<String> out) throws Exception
	{
		Map<String,String> functionInfo = new LinkedHashMap<>();
		functionInfo.put("functionName",lambdaName.value());
		Iterable<Integer> memoryValues = lambdaMemory.values();

		// get the average of the all maxUsedMemory values
		double averageMemory = StreamSupport.stream(memoryValues.spliterator(), false)
				.mapToInt(Integer::intValue)
				.average()
				.orElse(Double.NaN);

		// increasing the memory for some added buffer
		double memoryToSet = averageMemory + 100;

		functionInfo.put("memory", String.valueOf(memoryToSet));

		String result = mapper.writeValueAsString(functionInfo);
		logger.info("Optimal lambda memory to update: "+result);

		// clean up state
		lambdaName.clear();
		lambdaMemory.clear();

		out.collect(result);
	}

}

package com.telemetry.lambdalog;

import com.amazonaws.services.kinesisanalytics.flink.connectors.config.AWSConfigConstants;
import com.amazonaws.services.kinesisanalytics.runtime.KinesisAnalyticsRuntime;
import com.jayway.jsonpath.Configuration;
import com.telemetry.lambdalog.functions.LogProcessFunction;
import com.telemetry.lambdalog.utils.JsonpathHandler;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.connector.kinesis.sink.KinesisStreamsSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer;
import org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.Properties;

public class LambdaLogJob
{
	private static final Logger logger = LoggerFactory.getLogger(LambdaLogJob.class);

	public static void main(String[] args) throws Exception
	{
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		Map<String, Properties> properties = KinesisAnalyticsRuntime.getApplicationProperties();
		String sourceStreamName = properties.get("SourceKinesis").getProperty("name");
		String destinationStreamName = properties.get("DestinationKinesis").getProperty("name");

		Properties kinesisConsumerConfig = new Properties();
		kinesisConsumerConfig.setProperty(ConsumerConfigConstants.AWS_REGION, "eu-west-1");
		kinesisConsumerConfig.setProperty(ConsumerConfigConstants.STREAM_INITIAL_POSITION, "LATEST");

		FlinkKinesisConsumer<String> sourceStream = new FlinkKinesisConsumer<>(sourceStreamName,
				new SimpleStringSchema(), kinesisConsumerConfig);

		Properties kinesisProducerConfig = new Properties();
		kinesisProducerConfig.put(AWSConfigConstants.AWS_REGION, "eu-west-1");

		KinesisStreamsSink<String> destinationStream = KinesisStreamsSink.<String>builder()
				.setKinesisClientProperties(kinesisProducerConfig)
				.setSerializationSchema(new SimpleStringSchema())
				.setStreamName(destinationStreamName)
				.setPartitionKeyGenerator(element -> String.valueOf(element.hashCode()))
				.build();

		SingleOutputStreamOperator<String> lambdaCommandOutputStream = env.addSource(sourceStream).keyBy(x -> {
			try
			{
				Object document = Configuration.defaultConfiguration().jsonProvider().parse(x);

				return JsonpathHandler.getValue(document,"$.functionName");
			}
			catch (Exception e)
			{
				logger.info("Something went wrong, could not key event");
				return "";
			}

		}).process(new LogProcessFunction()).uid("log-processor");

		lambdaCommandOutputStream.sinkTo(destinationStream);

		env.execute("Lambda log processor");
	}

}

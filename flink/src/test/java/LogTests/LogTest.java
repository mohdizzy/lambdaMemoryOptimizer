package LogTests;

import com.telemetry.lambdalog.functions.LogProcessFunction;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.telemetry.lambdalog.utils.JsonpathHandler;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class LogTest
{

	static List<String> output = new ArrayList<>();

	private static SourceFunction<String> lamdaLogSource()
	{
		return new SourceFunction<String>()
		{
			@Override
			public void run(SourceContext<String> ctx) throws Exception
			{
				int count = 0;
				while (count < 3)
				{
					String deserializedString = "";
					String input = "";
					String filePath = new File("./").getCanonicalPath();
					SimpleStringSchema deserializerSchema = new SimpleStringSchema();

					if (count == 0)
					{
						filePath += "/src/test/java/events/logTest.txt";
						input = new String(Files.readAllBytes(Paths.get(filePath)));
						deserializedString = deserializerSchema.deserialize(input.getBytes());
					}

					if (count == 1)
					{
						filePath += "/src/test/java/events/logTest_2.txt";
						input = new String(Files.readAllBytes(Paths.get(filePath)));
						deserializedString = deserializerSchema.deserialize(input.getBytes());
					}

					if (count == 2)
					{
						filePath += "/src/test/java/events/logTest_3.txt";
						input = new String(Files.readAllBytes(Paths.get(filePath)));
						deserializedString = deserializerSchema.deserialize(input.getBytes());
					}
					ctx.collect(deserializedString);
					Thread.sleep(1000);
					count++;
				}
			}

			@Override
			public void cancel()
			{
			}
		};
	}

	@Test
	void endToEndTest() throws Exception
	{
		StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

		SinkFunction<String> fakeSink = new SinkFunction<String>()
		{
			@Override
			public void invoke(String value, Context context) throws Exception
			{
				output.add(value);
			}
		};

		buildFlinkJobCore(env, lamdaLogSource(), fakeSink);
		env.execute();
		String jsonFromKafka = new ObjectMapper().writeValueAsString(output);
		System.out.println("Flink Output: " + jsonFromKafka);
		assertThat(output.size()).isEqualTo(1);
	}

	private void buildFlinkJobCore(StreamExecutionEnvironment env, SourceFunction<String> logSource,
			SinkFunction<String> sink)
	{

		SingleOutputStreamOperator<String> lambdaLogStream = env.addSource(logSource).keyBy(x -> {
			try
			{
				Object document = Configuration.defaultConfiguration().jsonProvider().parse(x);

				return JsonpathHandler.getValue(document,"$.functionName");
			}
			catch (Exception e)
			{
				System.out.println("cannot key by");
				return "";
			}

		}).process(new LogProcessFunction());
		lambdaLogStream.addSink(sink);
		lambdaLogStream.print();

	}
}

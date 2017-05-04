package com.datafibers.test_tool;
import net.openhft.compiler.CompilerUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.DataSet;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.api.java.table.BatchTableEnvironment;
import org.apache.flink.api.java.table.StreamTableEnvironment;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.table.Table;
import org.apache.flink.api.table.TableEnvironment;
import org.apache.flink.api.table.Types;
import org.apache.flink.api.table.sinks.CsvTableSink;
import org.apache.flink.api.table.sinks.TableSink;
import org.apache.flink.api.table.sources.CsvTableSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.runtime.tasks.ExceptionInChainedOperatorException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

/**
 * The goal is to define a function with parameter TABLE, Transformation Script and return TABLE
 * This function will dynamically generate code and run.
 */
public class WordCountStream {


	public static void main(String args[]) {

		final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
		StreamTableEnvironment tableEnv = TableEnvironment.getTableEnvironment(env);

		// Create a DataStream from a list of elements
		//DataStream<Integer> ds = env.fromElements(1, 2, 3, 4, 5);

		CsvTableSource csvTableSource = new CsvTableSource(
				"/Users/will/Downloads/file.csv",
				new String[] { "name", "id", "score", "comments" },
				new TypeInformation<?>[] {
						Types.STRING(),
						Types.STRING(),
						Types.STRING(),
						Types.STRING()
				}); // lenient

		tableEnv.registerTableSource("mycsv", csvTableSource);



		TableSink sink = new CsvTableSink("/Users/will/Downloads/out.csv", "|");


		//tableEnv.registerDataStream("tbl", ds, "a");
		//Table ingest = tableEnv.fromDataStream(ds, "name");
		Table in = tableEnv.ingest("mycsv");
		//Table in = tableEnv.ingest("tbl");
		//Table in = tableEnv.fromDataStream(ds, "a");

		Table result = in.select("name");
		result.writeToSink(sink);
		try {
			env.execute();
		} catch (Exception e) {

		}

		System.out.print("DONE");
	}
}
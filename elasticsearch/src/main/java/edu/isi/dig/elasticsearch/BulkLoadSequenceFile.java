package edu.isi.dig.elasticsearch;

import java.io.IOException;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.BytesWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.Text;
import org.elasticsearch.action.bulk.BulkRequestBuilder;
import org.elasticsearch.client.Client;
import org.elasticsearch.client.transport.TransportClient;
import org.elasticsearch.common.transport.InetSocketTransportAddress;


public class BulkLoadSequenceFile {

	public static void main(String[] args) throws IllegalArgumentException, IOException, InterruptedException {
		Options options = createCommandLineOptions();
		CommandLine cl = parse(args, options, BulkLoadSequenceFile.class.getSimpleName());
		if(cl == null)
		{
			return;
		}
		String filePath = (String)cl.getOptionValue("filepath");
		String index = (String)cl.getOptionValue("index");
		String type = (String)cl.getOptionValue("type");
		String hostname = (String)cl.getOptionValue("hostname");
		SequenceFile.Reader reader = new SequenceFile.Reader(new Configuration(), SequenceFile.Reader.file(new Path(filePath)));
		BytesWritable key = new BytesWritable();
		Text val = new Text();
		Client client = new TransportClient().addTransportAddress(new InetSocketTransportAddress(hostname, 9300));
		BulkRequestBuilder bulkRequest = client.prepareBulk();
		int counter = 0;
		while (reader.next(key, val)) {			
			bulkRequest.add(client.prepareIndex(index, type).setSource(val.toString()));
			counter++;
			if (counter == 1000) {
				counter = 0;
				System.out.println("1000 resources processed");
				bulkRequest.execute().actionGet();
				bulkRequest = client.prepareBulk();
				Thread.sleep(100);
			}	
		}
		bulkRequest.execute().actionGet();
		reader.close();
	}
	
	private static Options createCommandLineOptions() {
		Options options = new Options();
				options.addOption(new Option("filepath", "filepath", true, "location of the input file directory"));
				options.addOption(new Option("type", "type", true, "elasticsearch type"));
				options.addOption(new Option("index", "index", true, "elasticsearch index"));
				options.addOption(new Option("hostname", "hostname", true, "elasticsearch hostname"));

		return options;
	}
	
	public static CommandLine parse(String args[], Options options, String commandName)
	{
		CommandLineParser parser = new BasicParser();
		 CommandLine cl = null;
		try {
	        cl = parser.parse(options, args);
	        if (cl == null || cl.getOptions().length == 0 || cl.hasOption("help")) {
	        	HelpFormatter hf = new HelpFormatter();
	        	hf.printHelp(commandName, options);
	            return null;
	        }
		
	} catch (Exception e) {
		return cl;
	}
		return cl;
	}

}

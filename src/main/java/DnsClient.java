import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

public class DnsClient {

	private static final String DEFAULT_TIMEOUT = "5";
	private static final String DEFAULT_MAX_RETRIES = "3";
	private static final String DEFAULT_PORT = "53";
	private static final String DEFAULT_SERVER_TYPE = "a";
	private static final int MAX_UDP_DNS_BYTE_SIZE = 512;

	private String timeout;
	private String max_retries;
	private String port;
	private String serverType;
	private String server;
	private String name;

	private long timeTookToReceivePacket;
	private int numberOfRetries;
	private ArrayList<String> error;

	public static void main(String[] args) {
		DnsClient dns = new DnsClient();
		dns.runDnsClient(args);
	}

	public DnsClient() {
		// Set all the default values
		timeout = DEFAULT_TIMEOUT;
		max_retries = DEFAULT_MAX_RETRIES;
		port = DEFAULT_PORT;
		serverType = DEFAULT_SERVER_TYPE;
		error = new ArrayList<String>();
	}

	/**
	 * This method receives the information off the command line and parses it.
	 * The information parsed will be used to set the class fields.
	 * 
	 * @param args
	 * @throws IOException
	 */
	private void parseCommandLine(String[] args) throws IOException {

		Option timeout = Option.builder("t")
			.longOpt("timeout")
			.valueSeparator(' ')
			.desc("How long to wait before retransmision")
			.hasArg()
			.build();

		Option maxRetries = Option.builder("r")
			.longOpt("maxretries")
			.valueSeparator(' ')
			.desc("Maximum number of times to retransmit an unanswered query before giving up")
			.hasArg()
			.build();

		Option port = Option.builder("p")
			.longOpt("port")
			.valueSeparator(' ')
			.desc("UDP port number of the DNS server")
			.hasArg()
			.build();

		Option mx = new Option("mx", "Send a MX (mail server) query, " + 
			"if neither mx or ns is given then A (IP address) query sent");

		Option ns = new Option("ns", "Send a NS (name server), " + 
			"if neither mx or ns is given then a type A (IP address) query sent");

		Option server = Option.builder("ip")
			.desc("The IPv4 address of the DNS server, in a.b.c.d. format")
			.valueSeparator(' ')
			.required()
			.hasArg()
			.build();

		Option name = Option.builder("n")
			.longOpt("name")
			.desc("The domain name to query for")
			.hasArg()
			.valueSeparator(' ')
			.required()
			.build();
		
		Option help = new Option("h", "help", false, "Print the help");

		Options helpOptions = new Options();
		helpOptions.addOption(help);

		Options dnsOptions = new Options();
		dnsOptions.addOption(timeout);
		dnsOptions.addOption(maxRetries);
		dnsOptions.addOption(port);
		dnsOptions.addOption(mx);
		dnsOptions.addOption(ns);
		dnsOptions.addOption(help);
		
		CommandLineParser cmdLineParser = new DefaultParser();
		
		CommandLine cmd = null;
		HelpFormatter helpFormatter = new HelpFormatter();
		try {
			// parse the command line arguments
			CommandLine helpCmd = new DefaultParser().parse(helpOptions, args, true);
			
			if (helpCmd.getOptions().length == 1 && helpCmd.hasOption(help.getOpt()) || helpCmd.hasOption(help.getLongOpt())) {
				helpFormatter.printHelp("dns-client", dnsOptions);
			} else {
				cmd = cmdLineParser.parse(dnsOptions, args, false);
			}
		}
		catch( ParseException exp ) {
			// oops, something went wrong
			System.err.println( "Parsing failed.  Reason: " + exp.getMessage() );
			helpFormatter.printHelp("dns-client", dnsOptions);
		}

		List<String> parsedOptions = cmd.getArgList();
		String ipAddress = "";
		String serverName = "";
		for (String option : parsedOptions) {
			if (option.charAt(0) == '@') {
				ipAddress = option.replace("@", "");
			} else {
				serverName = option;
			}
		}

		// // Regular expression to find the required IP and Server Name
		Pattern ipPattern = Pattern
				.compile("(\\d{1,3}[.]\\d{1,3}[.]\\d{1,3}[.]\\d{1,3})");
		Pattern serverNamePattern = Pattern
				.compile("([a-z0-9]+(-[a-z0-9]+)*\\.)+[a-z]{2,}");

		// // Checks the String for the required arguments
		// // If not found it throws an exception
		if (ipPattern.matcher(ipAddress).find())
			if (serverNamePattern.matcher(serverName).find()) {
				this.server = ipAddress;
				this.name = serverName;
			} else
				throw new IOException(
						"Please enter the required arguments @a.b.c.d serverName");
	}

	/**
	 * This method converts the IP Address from a string to a byte array of size
	 * 4
	 * 
	 * @param ipAddress
	 * @return byte[] containing the 4 matching IP address bytes
	 * @throws NumberFormatException
	 */
	private byte[] convertStringIpAddressToByteIpAddress(String ipAddress)
			throws NumberFormatException {
		// Split the IP address into an array of size 4 removing the periods
		String[] ip = ipAddress.split("[.]");

		// Ensure that the IP parts are a byte of information [0, 255]
		for (String i : ip) {
			int ipValue = Integer.parseInt(i);
			if (ipValue < 0 || ipValue > 255)
				throw new NumberFormatException(
						"The ipAddress has one or more dotted-decimal entries > 255 or < 0");
		}

		byte[] byteIpAddress = new byte[4];
		byteIpAddress[3] = (byte) Integer.parseInt(ip[3]);
		byteIpAddress[2] = (byte) Integer.parseInt(ip[2]);
		byteIpAddress[1] = (byte) Integer.parseInt(ip[1]);
		byteIpAddress[0] = (byte) Integer.parseInt(ip[0]);

		return byteIpAddress;
	}

	private void printOutput(DnsPacketHeader header, DnsQuestion question,
			DnsAnswer answer) {
		String auth;
		if (header.getAA() == 1)
			auth = "AUTH";
		else 
			auth = "NONAUTH";
		
		System.out.println("DnsClient sending request for " + this.name);
		System.out.println("Server: " + this.server);
		System.out.println("Request type: " + this.serverType.toUpperCase());
		System.out.println();

		System.out.println("Response received after "
				+ this.timeTookToReceivePacket + " milliseconds " + "("
				+ numberOfRetries + " retries)");
		System.out.println();

		System.out.println("***Answer Section (" + header.getANCOUNT()
				+ " records)***");
		Iterator<Hashtable<String, String>> iterator = answer.getAnswersList()
				.iterator();
		while (iterator.hasNext()) {
			Hashtable<String, String> ipInfo = iterator.next();
			if (Integer.parseInt(ipInfo.get("Type")) == 5)
				System.out.println("CNAME	" + ipInfo.get("Name") + "	"
						+ ipInfo.get("TTL") + "	" + auth);
			else if (Integer.parseInt(ipInfo.get("Type")) == 1)
				System.out.println("IP	" + ipInfo.get("IP") + "	"
						+ ipInfo.get("TTL") + "	" + auth);
			else if (Integer.parseInt(ipInfo.get("Type")) == 15)
				System.out.println("MX	" + ipInfo.get("Name") + "	"
						+ ipInfo.get("Preference") + "	" + ipInfo.get("TTL")
						+ "	" + auth);
			else if (Integer.parseInt(ipInfo.get("Type")) == 2)
				System.out.println("NS	" + ipInfo.get("Name") + "	"
						+ ipInfo.get("TTL") + "	" + auth);
		}

		System.out.println("***Additional Section (" + header.getARCOUNT()
				+ " records)***");
		if (header.getARCOUNT() == 0)
			System.out.println("NOTFOUND");

		System.out.println();
		if (error != null)
			for (String i : error)
				System.out.println("ERROR" + "	" + i);
	}

	private void runDnsClient(String[] args) {
		// Create reader to read user input from command line
		BufferedReader inCommand = new BufferedReader(new InputStreamReader(
				System.in));

		// Extract the arguments from the command line
		try {
			parseCommandLine(args);
		} catch (IOException e) {
			error.add(e.getMessage());
			System.err.println("ERROR	" + e.getMessage());
			return;
		}

		// Create the InetAddress by converting the IP argument into 4 bytes
		InetAddress serverIpAddress = null;
		try {
			serverIpAddress = InetAddress
					.getByAddress(convertStringIpAddressToByteIpAddress(server));
		} catch (UnknownHostException e) {
			error.add("The IP address cannot be resolved");
			System.err.println("ERROR	" + "The IP address cannot be resolved");
			return;
		} catch (NullPointerException f) {
			error.add("The IpAddress is missing dotted-decimal entries");
			System.err.println("ERROR" + "	"
					+ "The IpAddress is missing dotted-decimal entries");
			return;
		}

		// Create a UDP connection socket
		// (Note, when no port number is specified, the OS will assign an
		// arbitrary one)
		DatagramSocket dnsSocket = null;
		try {
			dnsSocket = new DatagramSocket();
			// Set the timeout in seconds
			dnsSocket.setSoTimeout(Integer.parseInt(this.timeout) * 1000);
		} catch (NumberFormatException n) {
			System.err.println("ERROR" + "	"
					+ "The timeout value needs to be a number");
		} catch (SocketException e) {
			error.add("ERROR" + "	" + "The socket didn't bind to a port");
			System.err.println("ERROR" + "	"
					+ "The socket didn't bind to a port");
			return;
		}

		// /////////////////////////////////
		// Build Packet //
		// /////////////////////////////////
		Random id = new Random(Short.MAX_VALUE + 1);
		DnsPacketHeader dnsHeader = new DnsPacketHeader((short) id.nextInt(),
				(byte) 0, (byte) 0, (byte) 0, (byte) 0, (byte) 1, (byte) 0,
				(byte) 0, (byte) 0, (short) 1, (short) 0, (short) 0, (short) 0);

		DnsQuestion dnsQuestion = new DnsQuestion(this.name, this.serverType,
				(short) 0x0001);

		// Set the size of the answer as the max DNS packet byte - the size of
		// the other 2 packets
		int dnsAnswerSize = MAX_UDP_DNS_BYTE_SIZE
				- dnsHeader.getDnsHeader().length
				- dnsQuestion.getDnsQuestion().length;
		DnsAnswer dnsAnswer = new DnsAnswer(dnsAnswerSize);

		ByteBuffer dnsDataBuffer = ByteBuffer.allocate(MAX_UDP_DNS_BYTE_SIZE);
		dnsDataBuffer.put(dnsHeader.getDnsHeader());
		dnsDataBuffer.put(dnsQuestion.getDnsQuestion());
		dnsDataBuffer.put(dnsAnswer.getDnsAnswer());

		byte[] dnsData = dnsDataBuffer.array();

		// Create the dnsPacket to be sent
		DatagramPacket dnsPacket = new DatagramPacket(dnsData, dnsData.length,
				serverIpAddress, Integer.parseInt(this.port));

		// Create the dnsPacket to be received
		DatagramPacket dnsReceive = new DatagramPacket(dnsData, dnsData.length);

		// In a do-while to handle max retries if the connection timeout
		this.numberOfRetries = 0;
		Exception socketTimeoutError = new Exception();
		do {
			try {
				// Send the DNS packet
				dnsSocket.send(dnsPacket);
			} catch (IOException e) {
				error.add("ERROR" + "	"
						+ "The DNS packet did not successfully get sent");
				System.err.println("ERROR" + "	"
						+ "The DNS packet did not successfully get sent");
				return;
			}

			try {
				// Start timer
				long startReceive = System.currentTimeMillis();
				// Receive the DNS packet
				dnsSocket.receive(dnsReceive);
				// End timer
				long endReceive = System.currentTimeMillis();
				this.timeTookToReceivePacket = endReceive - startReceive;
			} catch (SocketTimeoutException timeoutError) {
				// Keep trying until number of retries exceeds max retries
				socketTimeoutError = timeoutError;
				numberOfRetries++;
				if (numberOfRetries >= Integer.parseInt(max_retries)) {
					error.add("ERROR" + "	" + "A timeout occured");
					System.err.println("ERROR" + "	" + "A timeout occured");
					return;
				}
			} catch (IOException e) {
				error.add("ERROR" + "	"
						+ "The DNS packet wasn't successfully received");
				System.err.println("ERROR" + "	"
						+ "The DNS packet wasn't successfully received");
				return;
			}
		} while (socketTimeoutError instanceof SocketTimeoutException
				&& numberOfRetries < Integer.parseInt(max_retries));

		// Close the socket
		dnsSocket.close();

		// ///////////////////////////////////
		// Parse Received Packet //
		// ///////////////////////////////////

		int headerSize = dnsHeader.getDnsHeader().length;
		int questionSize = dnsQuestion.getDnsQuestion().length;
		int answerSize = dnsAnswer.getDnsAnswer().length;
		byte[] newHeader = Arrays.copyOfRange(dnsReceive.getData(), 0,
				headerSize);
		byte[] newQuestion = Arrays.copyOfRange(dnsReceive.getData(),
				headerSize, headerSize + questionSize);
		byte[] newAnswer = Arrays.copyOfRange(dnsReceive.getData(), headerSize
				+ questionSize, headerSize + questionSize + answerSize);

		try {
			dnsHeader.parse(newHeader);
		} catch (Exception e) {
			error.add(e.getMessage());
		}
		dnsQuestion.parse(newQuestion);
		dnsAnswer.parse(newAnswer, dnsQuestion, dnsHeader);

		printOutput(dnsHeader, dnsQuestion, dnsAnswer);
	}
}
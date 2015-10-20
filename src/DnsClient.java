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
import java.util.Random;
import java.util.regex.Pattern;

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

		// Check if there are any command line arguments
		if (args.length == 0)
			throw new IOException("Dns Client is missing all arguments");

		// Regular expression to find the required IP and Server Name
		Pattern ip = Pattern
				.compile("((@)\\d{1,3}[.]\\d{1,3}[.]\\d{1,3}[.]\\d{1,3})");
		Pattern serverName = Pattern.compile("\\w*\\D[.]\\w*[.]\\w*");

		// Create string off all arguments after removing the useless characters
		String commandLineArguments = Arrays.toString(args).replace(",", " ")
				.replace("[", "").replace("]", "");

		// Checks the String for the required arguments
		// If not found it throws an exception
		if (ip.matcher(commandLineArguments.toString()).find())
			if (serverName.matcher(commandLineArguments.toString()).find()) {
				this.server = args[args.length - 2].replace("@", "");
				this.name = args[args.length - 1];
			} else
				throw new IOException(
						"Please enter the required arguments @a.b.c.d serverName");

		// Loop through arguments to find the optional information
		// If an argument given does not follow the syntax then
		// IO exception thrown
		for (int i = 0; i < args.length; i++) {
			if (args[i].equals("-t")) {
				this.timeout = args[i + 1];
				i++;
			} else if (args[i].equals("-r")) {
				this.max_retries = args[i + 1];
				i++;
			} else if (args[i].equals("-p")) {
				this.port = args[i + 1];
				i++;
			} else if (args[i].equals("-mx") || args[i].equals("-ns")) {
				this.serverType = args[i].replace("-", "");
			} else if (args[i].contains("@")) {
				break;
			} else
				throw new IOException(
						"Unsupported flag - Supported flags include -t, -r, -p, -mx, -ns");
		}
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
			System.err.println(e.getMessage());
			return;
		}

		// Create the InetAddress by converting the IP argument into 4 bytes
		InetAddress serverIpAddress = null;
		try {
			serverIpAddress = InetAddress
					.getByAddress(convertStringIpAddressToByteIpAddress(server));
		} catch (UnknownHostException e) {
			error.add("The IP address cannot be resolved");
			return;
		} catch (NullPointerException f) {
			error.add("The IpAddress is missing dotted-decimal entries");
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
		} catch (SocketException e) {
			error.add("The socket didn't bind to a port");
			return;
		}

		///////////////////////////////////
		//     Build Packet              //
		///////////////////////////////////
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
				error.add("The DNS packet did not successfully get sent");
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
					error.add("A timeout occured");
					return;
				}
			} catch (IOException e) {
				error.add("The DNS packet wasn't successfully received");
				return;
			}
		} while (socketTimeoutError instanceof SocketTimeoutException
				&& numberOfRetries < Integer.parseInt(max_retries));

		// Close the socket
		dnsSocket.close();

		/////////////////////////////////////
		//      Parse Received Packet      //
		/////////////////////////////////////
		
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
		dnsAnswer.parse(newAnswer);

		printOutput(dnsHeader, dnsQuestion, dnsAnswer);
	}
}
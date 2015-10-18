import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Random;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

public class DnsClient {

	private static final String DEFAULT_TIMEOUT = "5";
	private static final String DEFAULT_MAX_RETRIES = "3";
	private static final String DEFAULT_PORT = "53";
	private static final String DEFAULT_SERVER_TYPE = "A";

	private String timeout;
	private String max_retries;
	private String port;
	private String serverType;
	private String server;
	private String name;

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
			if (args[i].equals("-t"))
				this.timeout = args[i + 1];
			else if (args[i].equals("-r"))
				this.max_retries = args[i + 1];
			else if (args[i].equals("-p"))
				this.port = args[i + 1];
			else if (args[i].equals("-mx") || args[i].equals("-ns"))
				this.serverType = args[i].replace("-", "");
			else
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

	private void runDnsClient(String[] args) {
		// Create reader to read user input from command line
		BufferedReader inCommand = new BufferedReader(new InputStreamReader(
				System.in));

		// Extract the arguments from the command line
		try {
			parseCommandLine(args);
		} catch (IOException e) {
			System.err.println(e.getMessage());
		} catch (ArrayIndexOutOfBoundsException a) {
			System.err.println("blah");
		}

		// Create the InetAddress by converting the IP argument into 4 bytes
		InetAddress serverIpAddress = null;
		try {
			serverIpAddress = InetAddress
					.getByAddress(convertStringIpAddressToByteIpAddress(server));
		} catch (UnknownHostException e) {
			System.err.println("The IP address cannot be resolved");
		} catch (NullPointerException f) {
			System.err
					.println("The IpAddress is missing dotted-decimal entries");
		}

		// Create a UDP connection socket
		// (Note, when no port number is specified, the OS will assign an
		// arbitrary one)
		DatagramSocket dnsSocket = null;
		try {
			dnsSocket = new DatagramSocket();
		} catch (SocketException e) {
			System.err.println("The socket didn't bind to a port");
		}

		DnsPacketHeader dnsHeader = new DnsPacketHeader(new Random(
				Short.MAX_VALUE + 1), qR, oPCODE, aA, tC, rD, rA, z, rCODE,
				qDCOUNT, aNCOUNT, nSCOUNT, aRCOUNT);
		DnsQuestion dnsQuestion = new DnsQuestion(this.name, this.serverType,
				qCLASS);
		DnsAnswer dnsAnswer = new DnsAnswer(nAME, tYPE, cLASS, tTL, rDLENGTH,
				rDATA);
		int capacity = dnsHeader.getDnsHeader().length
				+ dnsQuestion.getDnsQuestion().length
				+ dnsAnswer.getDnsAnswer().length;

		ByteBuffer dnsDataBuffer = ByteBuffer.allocate(capacity);
		dnsDataBuffer.put(dnsHeader.getDnsHeader());
		dnsDataBuffer.put(dnsQuestion.getDnsQuestion());
		dnsDataBuffer.put(dnsAnswer.getDnsAnswer());

		byte[] dnsData = dnsDataBuffer.array();
		// Create the dnsPacket to be sent
		DatagramPacket dnsPacket = new DatagramPacket(dnsData, dnsData.length,
				serverIpAddress, Integer.parseInt(this.port));
		try {
			// Send the DNS packet
			dnsSocket.send(dnsPacket);
		} catch (IOException e) {
			System.err.println("The DNS packet did not successfully get sent");
			;
		}

		// Create the dnsPacket to be received
		DatagramPacket dnsReceive = new DatagramPacket(dnsData, dnsData.length);
		try {
			// Receive the DNS packet
			dnsSocket.receive(dnsReceive);
		} catch (IOException e) {
			System.err.println("The DNS packet wasn't successfully received");
			;
		}

		byte[] receivedInfo = Arrays.copyOfRange(dnsReceive.getData(),
				dnsReceive.getOffset(),
				dnsReceive.getOffset() + dnsReceive.getLength());
	}
}
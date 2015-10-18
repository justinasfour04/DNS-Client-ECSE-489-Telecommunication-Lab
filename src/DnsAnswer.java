import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

public class DnsAnswer {
	
	private static final String A_TYPE = "a";
	private static final String NS_TYPE = "ns";
	private static final String MX_TYPE = "mx";

	private byte[] NAME;
	private short TYPE;
	private short CLASS;
	private int TTL;
	private short RDLENGTH;
	private int RDATA;

	private byte[] dnsAnswer;

	public DnsAnswer(String nAME, String tYPE, short cLASS, int tTL,
			short rDLENGTH, int rDATA) {
		dnsAnswer = buildDnsAnswer(nAME, tYPE, cLASS, tTL, rDLENGTH, rDATA);
	}

	private byte[] buildDnsAnswer(String nAME, String tYPE, short cLASS,
			int tTL, short rDLENGTH, int rDATA) {
		
		this.NAME = nameStringToByteArray(nAME);
		switch (tYPE) {
		case A_TYPE:
			this.QTYPE = 0x0001;
			break;
		case NS_TYPE:
			this.QTYPE = 0x0002;
			break;
		case MX_TYPE:
			this.QTYPE = 0x000f;
			break;
		}

		

		byte[] dnsAnswer = new byte[6];

		return dnsAnswer;
	}

	private byte[] nameStringToByteArray(String name) {
		String[] nameMinusPeriods = name.split("[.]");
		int numberOfLabels = nameMinusPeriods.length;

		// Loop through domain name and count the number of characters
		int numberOfCharacters = 0;
		for (String i : nameMinusPeriods) {
			numberOfCharacters += i.length();
		}

		// Create the byte buffer that will contain all the domain name
		// information in bytes
		ByteBuffer nameByteArray = ByteBuffer.allocate(numberOfLabels
				+ numberOfCharacters + 1);
		// Loop through the domain name words
		// At every string store the length of the word into
		// the byte buffer followed by the actually word in bytes
		for (String j : nameMinusPeriods) {
			nameByteArray.put((byte) j.length());
			try {
				nameByteArray.put(j.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				System.out.println(e.getMessage());
			}
		}

		nameByteArray.put((byte) 0b0);
		return nameByteArray.array();
	}

	public static String getaType() {
		return A_TYPE;
	}

	public static String getNsType() {
		return NS_TYPE;
	}

	public static String getMxType() {
		return MX_TYPE;
	}

	public byte[] getNAME() {
		return NAME;
	}

	public short getTYPE() {
		return TYPE;
	}

	public short getCLASS() {
		return CLASS;
	}

	public int getTTL() {
		return TTL;
	}

	public short getRDLENGTH() {
		return RDLENGTH;
	}

	public int getRDATA() {
		return RDATA;
	}

	public byte[] getDnsAnswer() {
		return dnsAnswer;
	}

}

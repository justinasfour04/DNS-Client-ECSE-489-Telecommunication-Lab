import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

public class DnsQuestion {

	private static final String A_TYPE = "a";
	private static final String NS_TYPE = "ns";
	private static final String MX_TYPE = "mx";

	private byte[] QNAME;
	private short QTYPE;
	private short QCLASS;
	private byte[] dnsQuestion;

	public DnsQuestion(String qNAME, String qTYPE, short qCLASS) {
		dnsQuestion = buildDnsQuestion(qNAME, qTYPE, qCLASS);
	}

	private byte[] buildDnsQuestion(String qNAME, String qTYPE, short qCLASS) {

		switch (qTYPE) {
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

		this.QCLASS = qCLASS;
		this.QNAME = QNameStringToByteArray(qNAME);

		ByteBuffer dnsQuestion = ByteBuffer.allocate(this.QNAME.length + 2*Short.BYTES);
		dnsQuestion.put(this.QNAME);
		dnsQuestion.putShort(this.QTYPE);
		dnsQuestion.putShort(this.QCLASS);
		return dnsQuestion.array();
	}

	private byte[] QNameStringToByteArray(String qNAME) {
		String[] qNameMinusPeriods = qNAME.split("[.]");
		int numberOfLabels = qNameMinusPeriods.length;
		
		// Loop through domain name and count the number of characters
		int numberOfCharacters = 0;
		for (String i : qNameMinusPeriods) {
			numberOfCharacters += i.length();
		}

		// Create the byte buffer that will contain all the domain name
		// information in bytes
		ByteBuffer qNameByteArray = ByteBuffer.allocate(numberOfLabels
				+ numberOfCharacters + 1);
		// Loop through the domain name words
		// At every string store the length of the word into
		// the byte buffer followed by the actually word in bytes
		for (String j : qNameMinusPeriods) {
			qNameByteArray.put((byte) j.length());
			try {
				qNameByteArray.put(j.getBytes("UTF-8"));
			} catch (UnsupportedEncodingException e) {
				System.out.println(e.getMessage());
			}
		}
		qNameByteArray.put((byte) 0b0);
		return qNameByteArray.array();
	}

	@Override
	public String toString() {
		return "DnsQuestion [QNAME=" + Arrays.toString(QNAME) + ", QTYPE="
				+ QTYPE + ", QCLASS=" + QCLASS + "]";
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

	public byte[] getQNAME() {
		return QNAME;
	}

	public short getQTYPE() {
		return QTYPE;
	}

	public short getQCLASS() {
		return QCLASS;
	}

	public byte[] getDnsQuestion() {
		return dnsQuestion;
	}
}

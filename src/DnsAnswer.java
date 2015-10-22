import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Hashtable;

public class DnsAnswer {

	private static final String A_TYPE = "a";
	private static final String NS_TYPE = "ns";
	private static final String MX_TYPE = "mx";
	private static final byte SIZE_OF_HEADER = 12;

	private byte[] NAME;
	private short TYPE;
	private short CLASS;
	private int TTL;
	private short RDLENGTH;
	private byte[] RDATA;

	private ArrayList<Hashtable<String, String>> answersList = new ArrayList<Hashtable<String, String>>();

	private byte[] dnsAnswer;

	/**
	 * Constructor for sending empty answer of certain size
	 * 
	 * @param size
	 */
	public DnsAnswer(int size) {
		dnsAnswer = new byte[size];
	}

	public void parse(byte[] dnsAnswer, DnsQuestion dnsQuestion,
			DnsPacketHeader dnsHeader) {
		int i = 0;
		int aliasOffset = 0;
		for (byte n : dnsAnswer) {
			if (String.format("%02X", n).equals("C0")) {
				// if a C0 hex value is spotted which refers to the CNAME
				// pointer then there has to exist
				// another C0 pointer 2 bytes later. If this occurs continue to
				// the next value in the loop
				if (String.format("%02X", dnsAnswer[i + 2]).equals("C0")) {
					i++;
					continue;
				}

				// Place the information in a hashtable because it is easier to
				// get later
				Hashtable<String, String> answers = new Hashtable<String, String>();
				answers.put(
						"Type",
						Short.toString((short) ((dnsAnswer[i + 2] << 8) | dnsAnswer[i + 3])));
				answers.put(
						"Class",
						Short.toString((short) ((dnsAnswer[i + 4] << 8) | dnsAnswer[i + 5])));
				answers.put(
						"TTL",
						Integer.toString((((dnsAnswer[i + 6] << 24) | dnsAnswer[i + 7]) << 16 | dnsAnswer[i + 8]) << 8
								| dnsAnswer[i + 9]));
				answers.put(
						"RDLength",
						Short.toString((short) ((dnsAnswer[i + 10] << 8) | dnsAnswer[i + 11])));

				this.TYPE = (short) Integer.parseInt(answers.get("Type"));
				this.RDLENGTH = (short) Integer.parseInt(answers
						.get("RDLength"));

				// CNAME entries
				if (TYPE == 0x0005) {
					answers.put(
							"Name",
							nameBytetoString(dnsQuestion.getQNAME(),
									dnsAnswer[i + 13] - SIZE_OF_HEADER));
					aliasOffset = dnsAnswer[i + 13] - SIZE_OF_HEADER;
				}

				// IP entries
				else if (TYPE == 0x0001) {
					this.RDATA = new byte[RDLENGTH];
					for (int j = 0; j < RDLENGTH; j++)
						this.RDATA[j] = dnsAnswer[i
								+ dnsHeader.getDnsHeader().length + j];

					InetAddress ip = null;
					try {
						ip = InetAddress.getByAddress(this.RDATA);
						answers.put("IP", ip.getHostAddress());
					} catch (UnknownHostException e) {
						System.err
								.println("ERROR	The RData is not an ip address");
					}
				}
				// MX entries
				else if (TYPE == 0x000f) {
					answers.put(
							"Preference",
							Short.toString((short) ((dnsAnswer[i + 12] << 8) | dnsAnswer[i + 13])));
					ByteBuffer mxNameBuffer = ByteBuffer.allocate(RDLENGTH
							+ dnsQuestion.getQNAME().length);

					// Get the alias name
					String alias = nameBytetoString(dnsQuestion.getQNAME(),
							aliasOffset);
					// Convert to bytes
					byte[] aliasBytes = nameStringToByteArray(alias);
					// fill buffer with new bytes
					for (int l = 0; l < RDLENGTH; l++) {
						if (String.format("%02X", dnsAnswer[i + 14 + l])
								.equals("C0"))
							break;
						mxNameBuffer.put(dnsAnswer[i + 14 + l]);
					}

					// Append the old domain alias bytes to the new ones
					mxNameBuffer.put(aliasBytes);
					// Add to the hastable
					answers.put("Name",
							nameBytetoString(mxNameBuffer.array(), 0));
				}
				// NS Type
				else if (TYPE == 0x0002) {
					ByteBuffer mxNameBuffer = ByteBuffer.allocate(RDLENGTH
							+ dnsQuestion.getQNAME().length);

					// Get the alias name
					String alias = nameBytetoString(dnsQuestion.getQNAME(),
							aliasOffset);
					// Convert to bytes
					byte[] aliasBytes = nameStringToByteArray(alias);
					// fill buffer with new bytes
					for (int byteCount = 0; byteCount < RDLENGTH; byteCount++) {
						if (String
								.format("%02X", dnsAnswer[i + 12 + byteCount])
								.equals("C0"))
							break;
						mxNameBuffer.put(dnsAnswer[i + 12 + byteCount]);
					}

					// Append the old domain alias bytes to the new ones
					mxNameBuffer.put(aliasBytes);
					// Add to the hastable
					answers.put("Name",
							nameBytetoString(mxNameBuffer.array(), 0));
				}
				// Add the hashtable to an arraylist in order to store all the
				// answers and get them as needed
				answersList.add(answers);
			}
			i++;
		}
	}

	private String nameBytetoString(byte[] nameInBytes, int offset) {
		StringBuilder name = new StringBuilder();
		// iterate through the byte array
		for (int i = offset; i < nameInBytes.length - 1; i++) {
			if (nameInBytes[i] == 0)
				break;
			// The label entry gives the size of the word then increment by 1
			char[] words = new char[nameInBytes[i++]];
			// Iterate through the bytes to store the characters of that the
			// bytes represent
			for (int j = 0; j < words.length; j++)
				words[j] = (char) nameInBytes[i++];

			// build the word and then move back 1
			name.append(String.valueOf(words) + ".");
			i--;
		}
		// return word without period at the end
		return name.toString().substring(0, name.toString().length() - 1);
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

	public byte[] getRDATA() {
		return RDATA;
	}

	public byte[] getDnsAnswer() {
		return dnsAnswer;
	}

	public ArrayList<Hashtable<String, String>> getAnswersList() {
		return answersList;
	}
}

import java.io.NotActiveException;
import java.rmi.ServerException;
import java.util.BitSet;

public class DnsPacketHeader {

	private short ID;
	private byte QR;
	private byte OPCODE, AA, TC, RD, RA, Z, RCODE;
	private short QDCOUNT;
	private short ANCOUNT;
	private short NSCOUNT;
	private short ARCOUNT;
	private byte[] dnsHeader;

	public DnsPacketHeader(short iD, byte qR, byte oPCODE, byte aA, byte tC,
			byte rD, byte rA, byte z, byte rCODE, short qDCOUNT, short aNCOUNT,
			short nSCOUNT, short aRCOUNT) {

		ID = iD;
		QR = qR;
		OPCODE = oPCODE;
		AA = aA;
		TC = tC;
		RD = rD;
		RA = rA;
		Z = z;
		RCODE = rCODE;
		QDCOUNT = qDCOUNT;
		ANCOUNT = aNCOUNT;
		NSCOUNT = nSCOUNT;
		ARCOUNT = aRCOUNT;

		byte[] dnsHeader = new byte[12];
		dnsHeader[0] = (byte) (ID >>> 8);
		dnsHeader[1] = (byte) ID;
		dnsHeader[2] = (byte) ((QR << 7) | (OPCODE << 3) | (AA << 2)
				| (TC << 1) | RD);
		dnsHeader[3] = (byte) ((RA << 7) | (Z << 4) | RCODE);
		dnsHeader[4] = (byte) (QDCOUNT >>> 8);
		dnsHeader[5] = (byte) QDCOUNT;
		dnsHeader[6] = (byte) (ARCOUNT >>> 8);
		dnsHeader[7] = (byte) ARCOUNT;
		dnsHeader[8] = (byte) (ANCOUNT >>> 8);
		dnsHeader[9] = (byte) ANCOUNT;
		dnsHeader[10] = (byte) (NSCOUNT >>> 8);
		dnsHeader[11] = (byte) NSCOUNT;

		this.dnsHeader = dnsHeader;
	}

	public void parse(byte[] header) throws Exception {
		this.ID = (short) ((header[0] << 8) | header[1]);
		this.QR = (byte) ((header[2] & 0xff) >>> 7);
		this.OPCODE = (byte) (((header[2] & 0xff) >>> 3) & 0x0f);
		this.AA = (byte) (((header[2] & 0xff) >>> 2) & 0x01);
		this.TC = (byte) (((header[2] & 0xff) >>> 1) & 0x01);
		this.RD = (byte) ((header[2] & 0xff) & 0x01);
		this.RA = (byte) (((header[3] & 0xff) >>> 7) & 0x01);
		this.Z = (byte) (((header[3] & 0xff) >>> 4) & 0x07);
		this.RCODE = (byte) ((header[3] & 0xff) & 0x0f);
		this.QDCOUNT = (short) ((header[4] << 8) | header[5]);
		this.ANCOUNT = (short) ((header[6] << 8) | header[7]);
		this.NSCOUNT = (short) ((header[8] << 8) | header[9]);
		this.ARCOUNT = (short) ((header[10] << 8) | header[11]);

		if (RA == 1)
			throw new Exception("The server does not support recursion");

		switch (RCODE) {
		case 0:
			break;
		case 1:
			throw new Exception(
					"The name server was unable to interpret the query");
		case 2:
			throw new Exception(
					"The name server was unable to process this query due to a "
							+ "problem with the name server");
		case 3:
			throw new Exception(
					"The domain name referenced in the query does not exist");
		case 4:
			throw new Exception(
					"The name server does not support the requested kind of query");
		case 5:
			throw new Exception(
					"The name server refuses to perform the requested operation "
					+ "for policy reasons");
		}
	}

	@Override
	public String toString() {
		return "DnsPacketHeader [ID=" + ID + ", QR=" + QR + ", OPCODE="
				+ OPCODE + ", AA=" + AA + ", TC=" + TC + ", RD=" + RD + ", RA="
				+ RA + ", Z=" + Z + ", RCODE=" + RCODE + ", QDCOUNT=" + QDCOUNT
				+ ", ANCOUNT=" + ANCOUNT + ", NSCOUNT=" + NSCOUNT
				+ ", ARCOUNT=" + ARCOUNT + "]";
	}

	public short getID() {
		return ID;
	}

	public byte getQR() {
		return QR;
	}

	public byte getOPCODE() {
		return OPCODE;
	}

	public byte getAA() {
		return AA;
	}

	public byte getTC() {
		return TC;
	}

	public byte getRD() {
		return RD;
	}

	public byte getRA() {
		return RA;
	}

	public byte getZ() {
		return Z;
	}

	public byte getRCODE() {
		return RCODE;
	}

	public short getQDCOUNT() {
		return QDCOUNT;
	}

	public short getANCOUNT() {
		return ANCOUNT;
	}

	public short getNSCOUNT() {
		return NSCOUNT;
	}

	public short getARCOUNT() {
		return ARCOUNT;
	}

	public byte[] getDnsHeader() {
		return dnsHeader;
	}
}

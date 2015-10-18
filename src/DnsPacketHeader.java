import java.util.Random;

public class DnsPacketHeader {

	private short ID;
	private byte QR, OPCODE, AA, TC, RD, RA, Z, RCODE;
	private short QDCOUNT;
	private short ANCOUNT;
	private short NSCOUNT;
	private short ARCOUNT;
	private byte[] dnsHeader;

	public DnsPacketHeader(short iD, byte qR, byte oPCODE, byte aA, byte tC,
			byte rD, byte rA, byte z, byte rCODE, short qDCOUNT, short aNCOUNT,
			short nSCOUNT, short aRCOUNT) {

		dnsHeader = buildDnsPacketHeader(iD, qR, oPCODE, aA, tC, rD, rA, z,
				rCODE, qDCOUNT, aNCOUNT, nSCOUNT, aRCOUNT);
	}

	private byte[] buildDnsPacketHeader(short iD, byte qR, byte oPCODE,
			byte aA, byte tC, byte rD, byte rA, byte z, byte rCODE,
			short qDCOUNT, short aNCOUNT, short nSCOUNT, short aRCOUNT) {

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

		return dnsHeader;
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

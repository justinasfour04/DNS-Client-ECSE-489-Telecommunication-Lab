public class Test {
	public static void main(String[] args) {
		DnsPacketHeader d1 = new DnsPacketHeader((short) 0x827a, (byte) 0,
				(byte) 0b0000, (byte) 0, (byte) 0, (byte) 1, (byte) 0,
				(byte) 0b000, (byte) 0b0000, (short) 0x0001, (short) 0x0000,
				(short) 0x0000, (short) 0x0000);
		for (byte i : d1.getDnsHeader())
			System.out.print(String.format("%02X", i) + " ");
		System.out.println(d1.toString());
		
		DnsQuestion d = new DnsQuestion("www.mcgill.ca", "a", (short) 0x1);
		for (byte i : d.getDnsQuestion())
			System.out.print(String.format("%02X", i) + " ");
		System.out.println(d.toString());
	}
}

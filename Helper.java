import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

/**
 * A helper method that does the following things:
 * (1) Hashing - for string, for socket address, and integer number
 * (2) Computation - relative id (one node is how far behind another node), 
 * a address' hex string and its percentage position in the ring (so we can
 * easily draw the picture of ring!), address' 8-digit hex string, the ith 
 * start of a node's finger table, power of two (to avoid computation of power
 * of 2 everytime we need it)
 * (3) Network and address services - send request to a node to get desired
 * socket address/response, create socket address object using string, read 
 * string from an input stream.
 * @author Chuan Xia
 *
 */

public class Helper {

	private static HashMap<Integer, Long> powerOfTwo = null;

	/**
	 * Constructor
	 */
	public Helper() {
		//initialize power of two table
		powerOfTwo = new HashMap<Integer, Long>();
		long base = 1;
		for (int i = 0; i <= 32; i++) {
			powerOfTwo.put(i, base);
			base *= 2;
		}
	}

	/**
	 * Compute a socket address' 32 bit identifier
	 * @param addr: socket address
	 * @return 32-bit identifier in long type
	 */
	public static long hashSocketAddress (InetSocketAddress addr) {
		int i = addr.hashCode();
		return hashHashCode(i);
	}

	/**
	 * Compute a string's 32 bit identifier
	 * @param s: string
	 * @return 32-bit identifier in long type
	 */
	public static long hashString (String s) {
		int i = s.hashCode();
		return hashHashCode(i);
	}

	/**
	 * Compute a 32 bit integer's identifier
	 * @param i: integer
	 * @return 32-bit identifier in long type
	 */
	private static long hashHashCode (int i) {

		//32 bit regular hash code -> byte[4]
		byte[] hashbytes = new byte[4];
		hashbytes[0] = (byte) (i >> 24);
		hashbytes[1] = (byte) (i >> 16);
		hashbytes[2] = (byte) (i >> 8);
		hashbytes[3] = (byte) (i /*>> 0*/);

		// try to create SHA1 digest
		MessageDigest md =  null;
		try {
			md = MessageDigest.getInstance("SHA-1");
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// successfully created SHA1 digest
		// try to convert byte[4] 
		// -> SHA1 result byte[]
		// -> compressed result byte[4] 
		// -> compressed result in long type
		if (md != null) {
			md.reset();
			md.update(hashbytes);
			byte[] result = md.digest();

			byte[] compressed = new byte[4];
			for (int j = 0; j < 4; j++) {
				byte temp = result[j];
				for (int k = 1; k < 5; k++) {
					temp = (byte) (temp ^ result[j+k]);
				}
				compressed[j] = temp;
			}

			long ret =  (compressed[0] & 0xFF) << 24 | (compressed[1] & 0xFF) << 16 | (compressed[2] & 0xFF) << 8 | (compressed[3] & 0xFF);
			ret = ret&(long)0xFFFFFFFFl;
			return ret;
		}
		return 0;
	}

	/**
	 * Normalization, computer universal id's value relative to local id
	 * (regard local node as 0)
	 * @param original: original/universal identifier
	 * @param n: node's identifier
	 * @return relative identifier
	 */
	public static long computeRelativeId (long universal, long local) {
		long ret = universal - local;
		if (ret < 0) {
			ret += powerOfTwo.get(32);
		}
		return ret;
	}

	/**
	 * Compute a socket address' SHA-1 hash in hex 
	 * and its approximate position in string
	 * @param addr
	 * @return 
	 */
	public static String hexIdAndPosition (InetSocketAddress addr) {
		long hash = hashSocketAddress(addr);
		return (longTo8DigitHex(hash)+" ("+hash*100/Helper.getPowerOfTwo(32)+"%)");
	}

	/**
	 * 
	 * @param Generate a long type number's 8-digit hex string
	 * @return
	 */
	public static String longTo8DigitHex (long l) {
		String hex = Long.toHexString(l);
		int lack = 8-hex.length();
		StringBuilder sb = new StringBuilder(); 
			for (int i = lack; i > 0; i--) {
				sb.append("0");
			}
		sb.append(hex);
		return sb.toString();
	}
	
	/**
	 * Return a node's finger[i].start, universal
	 * @param node: node's identifier
	 * @param i: finger table index
	 * @return finger[i].start's identifier
	 */
	public static long ithStart (long nodeid, int i) {
		return (nodeid + powerOfTwo.get(i-1)) % powerOfTwo.get(32);
	}

	/**
	 * Get power of 2
	 * @param k
	 * @return 2^k
	 */
	public static long getPowerOfTwo (int k) {
		return powerOfTwo.get(k);
	}

	/**
	 * Generate requested address by sending request to server
	 * @param server
	 * @param req: request
	 * @return generated socket address, 
	 * might be null if 
	 * (1) invalid input
	 * (2) response is null (typically cannot send request)
	 * (3) fail to create address from reponse
	 */
	public static InetSocketAddress requestAddress (InetSocketAddress server, String req) {

		// invalid input, return null
		if (server == null || req == null) {
			return null;
		}

		// send request to server
		String response = sendRequest(server, req);

		// if response is null, return null
		if (response == null) {
			return null;
		}

		// or server cannot find anything, return server itself 
		else if (response.startsWith("NOTHING"))
			return server;

		// server find something, 
		// using response to create, might fail then and return null
		else {
			InetSocketAddress ret = Helper.createSocketAddress(response.split("_")[1]);
			return ret;
		}
	}

	/**
	 * Send request to server and read response
	 * @param server
	 * @param request
	 * @return response, might be null if
	 * (1) invalid input
	 * (2) cannot open socket or write request to it
	 * (3) response read by inputStreamToString() is null
	 */
	public static String sendRequest(InetSocketAddress server, String req) {

		// invalid input
		if (server == null || req == null)
			return null;

		Socket talkSocket = null;

		// try to open talkSocket, output request to this socket
		// return null if fail to do so
		try {
			talkSocket = new Socket(server.getAddress(),server.getPort());
			PrintStream output = new PrintStream(talkSocket.getOutputStream());
			output.println(req);
		} catch (IOException e) {
			//System.out.println("\nCannot send request to "+server.toString()+"\nRequest is: "+req+"\n");
			return null;
		}

		// sleep for a short time, waiting for response
		try {
			Thread.sleep(60);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}

		// get input stream, try to read something from it
		InputStream input = null;
		try {
			input = talkSocket.getInputStream();
		} catch (IOException e) {
			System.out.println("Cannot get input stream from "+server.toString()+"\nRequest is: "+req+"\n");
		}
		String response = Helper.inputStreamToString(input);

		// try to close socket
		try {
			talkSocket.close();
		} catch (IOException e) {
			throw new RuntimeException(
					"Cannot close socket", e);
		}
		return response;
	}

	/**
	 * Create InetSocketAddress using ip address and port number
	 * @param addr: socket address string, e.g. 127.0.0.1:8080
	 * @return created InetSocketAddress object; 
	 * return null if:
	 * (1) not valid input 
	 * (2) cannot find split input into ip and port strings
	 * (3) fail to parse ip address.
	 */
	public static InetSocketAddress createSocketAddress (String addr) {
		
		// input null, return null
		if (addr == null) {
			return null;
		}

		// split input into ip string and port string
		String[] splitted = addr.split(":");

		// can split string
		if (splitted.length >= 2) {

			//get and pre-process ip address string
			String ip = splitted[0];
			if (ip.startsWith("/")) {
				ip = ip.substring(1);
			}

			//parse ip address, if fail, return null
			InetAddress m_ip = null;
			try {
				m_ip = InetAddress.getByName(ip);
			} catch (UnknownHostException e) {
				System.out.println("Cannot create ip address: "+ip);
				return null;
			}

			// parse port number
			String port = splitted[1];
			int m_port = Integer.parseInt(port);

			// combine ip addr and port in socket address
			return new InetSocketAddress(m_ip, m_port);
		}

		// cannot split string
		else {
			return null;
		}

	}

	/**
	 * Read one line from input stream
	 * @param in: input steam
	 * @return line, might be null if:
	 * (1) invalid input
	 * (2) cannot read from input stream
	 */
	public static String inputStreamToString (InputStream in) {

		// invalid input
		if (in == null) {
			return null;
		}

		// try to read line from input stream
		BufferedReader reader = new BufferedReader(new InputStreamReader(in));
		String line = null;
		try {
			line = reader.readLine();
		} catch (IOException e) {
			System.out.println("Cannot read line from input stream.");
			return null;
		}

		return line;
	}



}

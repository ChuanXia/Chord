import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Scanner;

/**
 * Chord class that offers the UI to create chord node 
 * and join a existing chord ring.
 * @author Chuan Xia
 *
 */

public class Chord {
	
	private static Node m_node;
	private static InetSocketAddress m_contact;
	private static Helper m_helper;

	public static void main (String[] args) {
		
		m_helper = new Helper();
		
		// get local machine's ip 
		String local_ip = null;
		try {
			local_ip = InetAddress.getLocalHost().getHostAddress();

		} catch (UnknownHostException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		// create node
		m_node = new Node (Helper.createSocketAddress(local_ip+":"+args[0]));
		
		// determine if it's creating or joining a existing ring
		// create, contact is this node itself
		if (args.length == 1) {
			m_contact = m_node.getAddress();
		}
		
		// join, contact is another node
		else if (args.length == 3) {
			m_contact = Helper.createSocketAddress(args[1]+":"+args[2]);
			if (m_contact == null) {
				System.out.println("Cannot find address you are trying to contact. Now exit.");
				return;
			}	
		}
		
		else {
			System.out.println("Wrong input. Now exit.");
			System.exit(0);
		}
		
		// try to join ring from contact node
		boolean successful_join = m_node.join(m_contact);
		
		// fail to join contact node
		if (!successful_join) {
			System.out.println("Cannot connect with node you are trying to contact. Now exit.");
			System.exit(0);
		}
		
		// print join info
		System.out.println("Joining the Chord ring.");
		System.out.println("Local IP: "+local_ip);
		m_node.printNeighbors();
		
		// begin to take user input, "info" or "quit"
		Scanner userinput = new Scanner(System.in);
		while(true) {
			System.out.println("\nType \"info\" to check this node's data or \n type \"quit\"to leave ring: ");
			String command = null;
			command = userinput.next();
			if (command.startsWith("quit")) {
				m_node.stopAllThreads();
				System.out.println("Leaving the ring...");
				System.exit(0);
				
			}
			else if (command.startsWith("info")) {
				m_node.printDataStructure();
			}
		}
	}
}

import java.net.InetSocketAddress;
import java.util.Scanner;

/**
 * Query class that offers the interface by which users can do 
 * search by querying a valid chord node.
 * @author Chuan Xia
 *
 */

public class Query {

	private static InetSocketAddress localAddress;
	private static Helper helper;

	public static void main (String[] args) {

		helper = new Helper();

		// valid args
		if (args.length == 2) {

			// try to parse socket address from args, if fail, exit
			localAddress = Helper.createSocketAddress(args[0]+":"+args[1]);
			if (localAddress == null) {
				System.out.println("Cannot find address you are trying to contact. Now exit.");
				System.exit(0);;	
			}

			// successfully constructed socket address of the node we are 
			// trying to contact, check if it's alive
			String response = Helper.sendRequest(localAddress, "KEEP");

			// if it's dead, exit
			if (response == null || !response.equals("ALIVE"))  {
				System.out.println("\nCannot find node you are trying to contact. Now exit.\n");
				System.exit(0);
			}

			// it's alive, print connection info
			System.out.println("Connection to node "+localAddress.getAddress().toString()+", port "+localAddress.getPort()+", position "+Helper.hexIdAndPosition(localAddress)+".");

			// check if system is stable
			boolean pred = false;
			boolean succ = false;
			InetSocketAddress pred_addr = Helper.requestAddress(localAddress, "YOURPRE");			
			InetSocketAddress succ_addr = Helper.requestAddress(localAddress, "YOURSUCC");
			if (pred_addr == null || succ_addr == null) {
				System.out.println("The node your are contacting is disconnected. Now exit.");
				System.exit(0);	
			}
			if (pred_addr.equals(localAddress))
				pred = true;
			if (succ_addr.equals(localAddress))
				succ = true;

			// we suppose the system is stable if (1) this node has both valid 
			// predecessor and successor or (2) none of them
			while (pred^succ) {
				System.out.println("Waiting for the system to be stable...");
				pred_addr = Helper.requestAddress(localAddress, "YOURPRE");			
				succ_addr = Helper.requestAddress(localAddress, "YOURSUCC");
				if (pred_addr == null || succ_addr == null) {
					System.out.println("The node your are contacting is disconnected. Now exit.");
					System.exit(0);	
				}
				if (pred_addr.equals(localAddress))
					pred = true;
				else 
					pred = false;
				if (succ_addr.equals(localAddress))
					succ = true;
				else 
					succ = false;
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
				}

			}

			// begin to take user input
			Scanner userinput = new Scanner(System.in);
			while(true) {
				System.out.println("\nPlease enter your search key (or type \"quit\" to leave): ");
				String command = null;
				command = userinput.nextLine();
				
				// quit
				if (command.startsWith("quit")) {
					System.exit(0);				
				}
				
				// search
				else if (command.length() > 0){
					long hash = Helper.hashString(command);
					System.out.println("\nHash value is "+Long.toHexString(hash));
					InetSocketAddress result = Helper.requestAddress(localAddress, "FINDSUCC_"+hash);
					
					// if fail to send request, local node is disconnected, exit
					if (result == null) {
						System.out.println("The node your are contacting is disconnected. Now exit.");
						System.exit(0);
					}
					
					// print out response
					System.out.println("\nResponse from node "+localAddress.getAddress().toString()+", port "+localAddress.getPort()+", position "+Helper.hexIdAndPosition(localAddress)+":");
					System.out.println("Node "+result.getAddress().toString()+", port "+result.getPort()+", position "+Helper.hexIdAndPosition(result ));
				}
			}
		}
		else {
			System.out.println("\nInvalid input. Now exit.\n");
		}
	}
}

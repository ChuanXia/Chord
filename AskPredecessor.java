import java.net.InetSocketAddress;

/**
 * Ask predecessor thread that periodically asks for predecessor's keep-alive,
 * and delete predecessor if it's dead.
 * @author Chuan Xia
 *
 */
public class AskPredecessor extends Thread {
	
	private Node local;
	private boolean alive;
	
	public AskPredecessor(Node _local) {
		local = _local;
		alive = true;
	}
	
	@Override
	public void run() {
		while (alive) {
			InetSocketAddress predecessor = local.getPredecessor();
			if (predecessor != null) {
				String response = Helper.sendRequest(predecessor, "KEEP");
				if (response == null || !response.equals("ALIVE")) {
					local.clearPredecessor();	
				}

			}
			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void toDie() {
		alive = false;
	}
}



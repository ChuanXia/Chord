import java.net.InetSocketAddress;
import java.util.Random;

/**
 * Fixfingers thread that periodically access a random entry in finger table 
 * and fix it.
 * @author Chuan Xia
 *
 */

public class FixFingers extends Thread{

	private Node local;
	Random random;
	boolean alive;

	public FixFingers (Node node) {
		local = node;
		alive = true;
		random = new Random();
	}

	@Override
	public void run() {
		while (alive) {
			int i = random.nextInt(31) + 2;
			InetSocketAddress ithfinger = local.find_successor(Helper.ithStart(local.getId(), i));
			local.updateFingers(i, ithfinger);
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

package strategies;

import java.util.PriorityQueue;
import java.util.Comparator;

import automail.MailItem;
import automail.PriorityMailItem;
import automail.Robot;
import automail.StorageTube;
import exceptions.TubeFullException;
import java.util.List;
import java.util.ArrayList;

public class MyMailPool5 implements IMailPool {

	private PriorityQueue<MailItem> nonPriorityPool;
	private PriorityQueue<MailItem> priorityPool;
	private static final int MAX_TAKE = 4;

	/** The number of robots. **/
	private static final int ROBOT_NUM = 3;
	private Robot[] robotList = new Robot[ROBOT_NUM];

	/**
	 * Constructor for MyMailPool. Use one comparator to sort the mailItems in
	 * nonPriorityPool based on their destination floor and arrival time. Use
	 * another comparator to sort the mailItems in priorityPool based on their
	 * priority level.
	 */
	public MyMailPool5() {

		nonPriorityPool = new PriorityQueue<>(new Comparator<MailItem>() {

			@Override
			public int compare(MailItem m1, MailItem m2) {
				// The lower the destination floor, the higher the delivery order of the
				// mailItem.
				if (m1.getDestFloor() > m2.getDestFloor()) {
					return 1;
				} else if (m1.getDestFloor() < m2.getDestFloor()) {
					return -1;
				} else {
					// If two mailItem have the same destination floor, the closer the arrival time,
					// the higher the delivery order of the mailItem
					return m1.getArrivalTime() - m2.getArrivalTime();
				}
			}
		});

		priorityPool = new PriorityQueue<>(new Comparator<MailItem>() {

			@Override
			public int compare(MailItem p1, MailItem p2) {
				// The higher the priority of the mailItem, the higher the delivery order of the
				// mailItem.
				return ((PriorityMailItem) p2).getPriorityLevel() - ((PriorityMailItem) p1).getPriorityLevel();
			}
		});
	}

	/**
	 * Add mail items to two mail pools respectively.
	 * 
	 * @param mailItem the mail item being added.
	 */
	public void addToPool(MailItem mailItem) {
		// Check whether it has a priority or not
		if (mailItem instanceof PriorityMailItem) {
			// Add to priority items
			priorityPool.add((PriorityMailItem) mailItem);
		} else {
			// Add to nonpriority items
			nonPriorityPool.add(mailItem);
		}
	}

	/**
	 * Get the size of nonPriorityPool excluding overweight mail items.
	 * 
	 * @param weightLimit
	 * @return the size of nonPriorityPool excluding overweight mail items.
	 */
	private int getNonPriorityPoolSize(int weightLimit) {
		return getPoolSize(weightLimit, nonPriorityPool);
	}

	/**
	 * Get the size of priorityPool excluding overweight mail items.
	 * 
	 * @param weightLimit
	 * @return the size of priorityPool excluding overweight mail items.
	 */
	private int getPriorityPoolSize(int weightLimit) {
		return getPoolSize(weightLimit, priorityPool);
	}

	/**
	 * Get the size of mail pool excluding overweight mail items.
	 * 
	 * @param weightLimit
	 * @param             pool, either priorityPool or nonPriorityPool.
	 * @return the size of the mail pool excluding overweight mail items.
	 */
	private int getPoolSize(int weightLimit, PriorityQueue<MailItem> pool) {
		int size = 0;
		for (MailItem mailItem : pool) {
			if (mailItem.getWeight() <= weightLimit) {
				size++;
			} else {
				System.out.println("Weight out of limit!");
			}
		}
		return size;
	}

	/**
	 * Retrieves and removes the head of the nonPriorityPool excluding overweight
	 * mail items.
	 * 
	 * @param weightLimit
	 * @return the head mail item of the nonPriorityPool excluding overweight mail
	 *         items.
	 */
	private MailItem getNonPriorityMail(int weightLimit) {

		return getMail(weightLimit, nonPriorityPool);
	}

	/**
	 * Retrieves and removes mail item with highest priority of the priorityPool
	 * excluding overweight mail items.
	 * 
	 * @param weightLimit
	 * @return the head mail item of the priorityPool excluding overweight mail
	 *         items.
	 */
	private MailItem getHighestPriorityMail(int weightLimit) {
		return getMail(weightLimit, priorityPool);

	}

	/**
	 * Retrieves and removes the head of the mail pool excluding overweight mail
	 * items.
	 * 
	 * @param weightLimit
	 * @param             pool, either priorityPool or nonPriorityPool.
	 * @return the head of the mail pool excluding overweight mail items.
	 */
	private MailItem getMail(int weightLimit, PriorityQueue<MailItem> pool) {
		// Use an ArrayList to store the overweight mail items.
		List<MailItem> temp = new ArrayList<>();
		MailItem mail = null;
		if (getPoolSize(weightLimit, pool) > 0) {
			// Retrieves and removes the head of the mail pool excluding overweight mail
			// items and store the overweight mail items.
			while (!pool.isEmpty()) {
				if (pool.peek().getWeight() <= weightLimit) {
					mail = pool.poll();
					break;
				} else if (pool == priorityPool) {
					temp.add((PriorityMailItem) pool.poll());
				} else {
					temp.add(pool.poll());
				}
			}
			// Add overweight mail items back to the mail pool.
			for (MailItem mailItem : temp) {
				pool.add(mailItem);
			}
		}

		return mail;
	}

	/**
	 * Load up any waiting robots with mailItems, if any.
	 */
	@Override
	public void step() {

		for (int i = 0; i < ROBOT_NUM; i++) {
			if (robotList[i] != null) {
				fillStorageTube(robotList[i]);
			}
		}
	}

	/**
	 * Fill the storage tube of the robot with as many priority items as available
	 * or as fit and then fill the storage tube with as many non-priority items as
	 * available or as fit. After filling the storage tube, let the robot get ready
	 * to dispatch.
	 * 
	 * @param robot
	 */
	private void fillStorageTube(Robot robot) {
		StorageTube tube = robot.getTube();
		int max = robot.isStrong() ? Integer.MAX_VALUE : 2000; // max weight

		try {
			// Get as many priority items as available or as fit
			while (tube.getSize() < MAX_TAKE && getPriorityPoolSize(max) > 0) {

				tube.addItem(getHighestPriorityMail(max));

			}
			// Then get as many non-priority items as available or as fit
			while (tube.getSize() < MAX_TAKE && getNonPriorityPoolSize(max) > 0) {

				tube.addItem(getNonPriorityMail(max));
			}

			if (tube.getSize() > 0)
				robot.dispatch();
		} catch (TubeFullException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Register the waiting robot to tell the sorter the robot is ready.
	 * 
	 * @param robot refers to a robot which has arrived back ready for more
	 *              mailItems to deliver
	 */
	@Override
	public void registerWaiting(Robot robot) {

		boolean failed = true;
		for (int i = 0; i < ROBOT_NUM; i++) {
			if (robotList[i] == null) {
				robotList[i] = robot;
				failed = false;
				break;
			}
		}
		if (failed) {
			System.out.println("Register Waiting Failed!");
		}
	}

	/**
	 * Deregister the waiting robot to tell the sorter the robot is delivering.
	 * 
	 * @param robot refers to a robot which has left (with more mailItems to
	 *              deliver)
	 */
	@Override
	public void deregisterWaiting(Robot robot) {

		boolean failed = true;
		for (int i = 0; i < ROBOT_NUM; i++) {
			if (robotList[i] == robot) {
				robotList[i] = null;
				failed = false;
				break;
			}
		}
		if (failed) {
			System.out.println("Deregister Waiting Failed!");
		}

	}

}

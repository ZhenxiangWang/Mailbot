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

public class MyMailPool4 implements IMailPool {
	
	private PriorityQueue<MailItem> nonPriorityPool;
	private PriorityQueue<PriorityMailItem> priorityPool;
	private static final int MAX_TAKE = 4;
	
	/** The number of robots.**/
	private static final int ROBOT_NUM = 3;
	private Robot[] robotList = new Robot[ROBOT_NUM];

	/**
	 * Constructor for MyMailPool.
	 * Use one comparator to sort the mailItems in nonPriorityPool based on their arrival time.
	 * Use another comparator to sort the mailItems in priorityPool based on their priority level.
	 */
	public MyMailPool4() {

		nonPriorityPool = new PriorityQueue<MailItem>(new Comparator<MailItem>() {

			@Override
			public int compare(MailItem m1, MailItem m2) {
				return m1.getArrivalTime() - m2.getArrivalTime();
			}
		});

		priorityPool = new PriorityQueue<PriorityMailItem>(new Comparator<PriorityMailItem>() {

			@Override
			public int compare(PriorityMailItem p1, PriorityMailItem p2) {
				return p2.getPriorityLevel() - p1.getPriorityLevel();
			}
		});
	}

	/**
	 * Add mail items to two mail pools.
	 * @param mailItem mail item includes priority mail items and non-priority mail items.
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
	 * 
	 * @param weightLimit
	 * @return
	 */
	private int getNonPriorityPoolSize(int weightLimit) {
		int size = 0;
		for (MailItem mailItem : nonPriorityPool) {
			if (mailItem.getWeight() <= weightLimit) {
				size++;
			} else {
				System.out.println("Weight out of limit!");
			}
		}
		return size;

	}

	private int getPriorityPoolSize(int weightLimit) {
		int size = 0;
		for (MailItem mailItem : priorityPool) {
			if (mailItem.getWeight() <= weightLimit) {
				size++;
			} else {
				System.out.println("Weight out of limit!");
			}
		}
		return size;
	}

	private MailItem getNonPriorityMail(int weightLimit) {
		
		List<MailItem> temp = new ArrayList<>();
		MailItem mail = null;
		if (getNonPriorityPoolSize(weightLimit) > 0) {
			// Should I be getting the earliest one?
			// Surely the risk of the weak robot getting a heavy item is small!

			while(!nonPriorityPool.isEmpty()){
				if (nonPriorityPool.peek().getWeight() <= weightLimit) {
					mail = nonPriorityPool.poll();
					break;
				} else {
					temp.add(nonPriorityPool.poll());
				}
			}
			for (MailItem mailItem:temp) {
				nonPriorityPool.add(mailItem);
			}
		}

		return mail;
	}

	private MailItem getHighestPriorityMail(int weightLimit) {
//		if (getPriorityPoolSize(weightLimit) > 0) {
//			return priorityPool.poll();
//		} else {
//			return null;
//		}
		
		List<PriorityMailItem> temp = new ArrayList<>();
		MailItem mail = null;
		if (getPriorityPoolSize(weightLimit) > 0) {
			// Should I be getting the earliest one?
			// Surely the risk of the weak robot getting a heavy item is small!

			while(!priorityPool.isEmpty()){
				if (priorityPool.peek().getWeight() <= weightLimit) {
					mail = priorityPool.poll();
					break;
				} else {
					temp.add(priorityPool.poll());
				}
			}
			for (PriorityMailItem mailItem:temp) {
				priorityPool.add(mailItem);
			}
		}

		return mail;

	}

	@Override
	public void step() {
		
		for(int i=0;i<ROBOT_NUM;i++) {
			if(robotList[i]!=null) {
				fillStorageTube(robotList[i]);
			}
		}
	}

	private void fillStorageTube(Robot robot) {
		StorageTube tube = robot.getTube();
		int max = robot.isStrong() ? Integer.MAX_VALUE : 2000; // max weight

		try {
			// Get as many priority items as available or as fit
			while (tube.getSize() < MAX_TAKE && getPriorityPoolSize(max) > 0) {
//				MailItem mail=getHighestPriorityMail(max);
//				if(mail!=null)
//					tube.addItem(mail);
				tube.addItem(getHighestPriorityMail(max));

			}
			// Get as many nonpriority items as available or as fit
			while (tube.getSize() < MAX_TAKE && getNonPriorityPoolSize(max) > 0) {
//				MailItem mail=getNonPriorityMail(max);
//				if(mail!=null)
//					tube.addItem(mail);
				tube.addItem(getNonPriorityMail(max));
			}

			if (tube.getSize() > 0)
				robot.dispatch();
		} catch (TubeFullException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void registerWaiting(Robot robot) {
		
		for(int i=0;i<ROBOT_NUM;i++) {
			if(robotList[i]==null) {
				robotList[i]=robot;
				break;
			}
		}
	}

	@Override
	public void deregisterWaiting(Robot robot) {
		
		for(int i=0;i<ROBOT_NUM;i++) {
			if(robotList[i]==robot) {
				robotList[i]=null;
				break;
			}
		}

	}

}

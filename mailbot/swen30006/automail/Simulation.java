package automail;

import exceptions.ExcessiveDeliveryException;
import exceptions.ItemTooHeavyException;
import exceptions.MailAlreadyDeliveredException;
import strategies.Automail;
import strategies.IMailPool;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;

/**
 * This class simulates the behaviour of AutoMail
 */
public class Simulation {

    /** Constant for the mail generator */
    private static final int MAIL_TO_CREATE = 180;
    

    private static ArrayList<MailItem> MAIL_DELIVERED;
    private static double total_score = 0;

    public static void main(String[] args) throws IOException, InstantiationException, IllegalAccessException, ClassNotFoundException {
    	Properties automailProperties = new Properties();
		// Default properties
    	automailProperties.setProperty("Number_of_Robots", "3");
    	automailProperties.setProperty("MailPool", "strategies.SimpleMailPool");
    	// Read properties
		FileReader inStream = null;
		try {
			inStream = new FileReader("automail.properties");
			automailProperties.load(inStream);
		} finally {
			 if (inStream != null) {
	                inStream.close();
	            }
		}
		int number_of_Robots = Integer.parseInt(automailProperties.getProperty("Number_of_Robots"));
		String mailPoolName = automailProperties.getProperty("MailPool");
		IMailPool mailPool = (IMailPool) Class.forName(mailPoolName).newInstance();
		String seedProp = automailProperties.getProperty("Seed");
		// End properties
		
        MAIL_DELIVERED = new ArrayList<MailItem>();
                
        /** Used to see whether a seed is initialized or not */
        HashMap<Boolean, Integer> seedMap = new HashMap<>();
        
        /** Read the first argument and save it as a seed if it exists */
        if (args.length == 0 ) { // No arg
        	if (seedProp == null) { // and no property
        		seedMap.put(false, 0); // so randomise
        	} else { // Use property seed
        		seedMap.put(true, Integer.parseInt(seedProp));
        	}
        } else { // Use arg seed - overrides property
        	seedMap.put(true, Integer.parseInt(args[0]));
        }
        Integer seed = seedMap.get(true);
        System.out.printf("Seed: %s%n", seed == null ? "null" : seed.toString());
        Automail automail = new Automail(mailPool, new ReportDelivery());
        MailGenerator mailGenerator = new MailGenerator(MAIL_TO_CREATE, automail.mailPool, seedMap);
        
        /** Initiate all the mail */
        mailGenerator.generateAllMail();
        // PriorityMailItem priority;  // Not used in this version
//        System.out.println("test");
        while(MAIL_DELIVERED.size() != mailGenerator.MAIL_TO_CREATE) {
        	
//        	System.out.println(MAIL_DELIVERED.size());
//        	System.out.println(mailGenerator.MAIL_TO_CREATE);
//        	System.out.println("-- Step: "+Clock.Time());
        	
            /* priority = */ mailGenerator.step();
            automail.mailPool.step();
            try {
				for (int i=0; i<3; i++) automail.robot[i].step();
			} catch (ExcessiveDeliveryException|ItemTooHeavyException e) {
				e.printStackTrace();
				System.out.println("Simulation unable to complete.");
				System.exit(0);
			}
            Clock.Tick();
        }
        printResults();
    }
    
    static class ReportDelivery implements IMailDelivery {
    	
    	/** Confirm the delivery and calculate the total score */
    	public void deliver(MailItem deliveryItem){
    		if(!MAIL_DELIVERED.contains(deliveryItem)){
                System.out.printf("T: %3d > Delivered [%s]%n", Clock.Time(), deliveryItem.toString());
    			MAIL_DELIVERED.add(deliveryItem);
    			// Calculate delivery score
    			total_score += calculateDeliveryScore(deliveryItem);
    		}
    		else{
    			try {
    				throw new MailAlreadyDeliveredException();
    			} catch (MailAlreadyDeliveredException e) {
    				e.printStackTrace();
    			}
    		}
    	}

    }
    
    private static double calculateDeliveryScore(MailItem deliveryItem) {
    	// Penalty for longer delivery times
    	final double penalty = 1.2;
    	double priority_weight = 0;
        // Take (delivery time - arrivalTime)**penalty * (1+sqrt(priority_weight))
    	if(deliveryItem instanceof PriorityMailItem){
    		priority_weight = ((PriorityMailItem) deliveryItem).getPriorityLevel();
    	}
        return Math.pow(Clock.Time() - deliveryItem.getArrivalTime(),penalty)*(1+Math.sqrt(priority_weight));
    }

    public static void printResults(){
        System.out.println("T: "+Clock.Time()+" | Simulation complete!");
        System.out.println("Final Delivery time: "+Clock.Time());
        System.out.printf("Final Score: %.2f%n", total_score);
    }
}

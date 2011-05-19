package uk.ac.dmu.iesd.cascade.context;


import java.util.*;
import java.util.Vector;
import repast.simphony.engine.schedule.*;
import repast.simphony.essentials.RepastEssentials;
import repast.simphony.space.graph.*;
import repast.simphony.ui.probe.*;
import uk.ac.dmu.iesd.cascade.Consts;


/**
 *  An <em>AggregatorAgent</em> is an object that represents a commercial/business 
 *  entity providing energy services to prosumer agents (<code>ProsumerAgent</code>) such
 *  as household prosumers (<code>HouseholdProsumer</code>) [i.e. it is involved in retail trade], 
 *  while at the same time is securing its gross energy need by trading in the wholesale market.
 *  The <code>AggregatorAgent</code> class is the abstract superclass of aggregator agents.
 *  Examples of aggregator agents include ESCO, RECO and GENCO. <p>
 *  
 * @author J. Richard Snape
 * @author Babak Mahdavi
 * @version $Revision: 1.2 $ $Date: 2011/05/19 13:00:00 $
 * 
 * Version history (for intermediate steps see Git repository history)
 * 
 * 1.1 - Implements ICognitiveAgent (Babak)
 * 1.2 - Made the class abstract; modified the constructor, added/modified/removed fields/methods
 *       made some methods abstract (Babak)
 */
public class AggregatorAgent implements ICognitiveAgent {

	/*
	 * Agent properties
	 */

	/**
	 * an aggregator agent's ID
	 * This field is automatically assigned by constructor 
	 * when the agent is created for the first time
	 * */
	protected long agentID = -1; 

	/**
	 * This field is used for counting number of agents 
	 * instantiated by descendants of this class  
	 **/	
	private static long agentIDCounter = 0; 

	/**
	 * An aggregator agent's name
	 * This field can be <code>null</code>.
	 * */
	protected String agentName;

	/**
	 * An aggregator agent's base name  
	 * it can be reassigned (renamed) properly by descendants of this class  
	 **/	
	protected static String agentBaseName = "aggregator";

	/**
	 * A boolen to determine whether the name has
	 * been set explicitly. <code>nameExplicitlySet</code> will
	 * be false if the name has not been set and true if it has.
	 * @see #getName
	 * @see #setName(String)
	 */
	protected boolean nameExplicitlySet = false;
	
	protected CascadeContext mainContext;
	
	boolean autoControl;
	//String contextName;
	/*
	 * This is net demand, may be +ve (consumption), 0, or 
	 * -ve (generation)
	 */
	protected float netDemand;
	float[] predictedCustomerDemand;
	int predictedCustomerDemandLength;
	float[] overallSystemDemand;
	int overallSystemDemandLength;
	// priceSignal units are �/MWh which translates to p/kWh if divided by 10
	float[] priceSignal;
	int priceSignalLength;
	boolean priceSignalChanged = true;  //set true when we wish to send a new and different price signal.  
	//True by default as it will always be new until the first broadcast
	int ticksPerDay;


	/**
	 * @param Parameters for this setup
	 */
	
	/*public AggregatorAgent(String myContext, float[] baseDemand, Parameters parm) {
		super();
		
		this.ticksPerDay = (Integer) parm.getValue("ticksPerDay");
		this.contextName = myContext;
		this.overallSystemDemandLength = baseDemand.length;
		this.priceSignalLength = baseDemand.length;
		
		if (overallSystemDemandLength % ticksPerDay != 0)
		{
			System.err.println("baseDemand array imported to aggregator not a whole number of days");
			System.err.println("May cause unexpected behaviour - unless you intend to repeat the signal within a day");
		}
		this.priceSignal = new float [priceSignalLength];
		this.overallSystemDemand = new float [overallSystemDemandLength];
		System.arraycopy(baseDemand, 0, this.overallSystemDemand, 0, overallSystemDemandLength);
		//Start initially with a flat price signal of 12.5p per kWh
		Arrays.fill(priceSignal,125f);
		
		//Very basic configuration of predicted customer demand as 
		// a Conssant.  We could be more sophisticated than this or 
		// possibly this gives us an aspirational target...
		this.predictedCustomerDemand = new float[ticksPerDay];
		//Put in a constant predicted demand
		//Arrays.fill(this.predictedCustomerDemand, 5);
		//Or - put in a variable one
		for (int j = 0; j < ticksPerDay; j++)
		{
			this.predictedCustomerDemand[j] = baseDemand[j] / 7000;
		}
		this.predictedCustomerDemandLength = ticksPerDay;
	}
	*/
	
	public AggregatorAgent(CascadeContext context, float[] baseDemand) {
	
		this.ticksPerDay = context.getTickPerDay();
		//this.contextName = myContext;
		this.overallSystemDemandLength = baseDemand.length;
		this.priceSignalLength = baseDemand.length;
		
		if (overallSystemDemandLength % ticksPerDay != 0)
		{
			System.err.println("baseDemand array imported to aggregator not a whole number of days");
			System.err.println("May cause unexpected behaviour - unless you intend to repeat the signal within a day");
		}
		this.priceSignal = new float [priceSignalLength];
		this.overallSystemDemand = new float [overallSystemDemandLength];
		System.arraycopy(baseDemand, 0, this.overallSystemDemand, 0, overallSystemDemandLength);
		//Start initially with a flat price signal of 12.5p per kWh
		Arrays.fill(priceSignal,125f);
		
		//Very basic configuration of predicted customer demand as 
		// a Conssant.  We could be more sophisticated than this or 
		// possibly this gives us an aspirational target...
		this.predictedCustomerDemand = new float[ticksPerDay];
		//Put in a constant predicted demand
		//Arrays.fill(this.predictedCustomerDemand, 5);
		//Or - put in a variable one
		for (int j = 0; j < ticksPerDay; j++)
		{
			this.predictedCustomerDemand[j] = baseDemand[j] / 7000;
		}
		this.predictedCustomerDemandLength = ticksPerDay;
	}
	
	

	/**
	 * Returns a string representation of this agent and its key values 
	 * Currently is used by Repast as the method which produces and returns the probe ID.  
	 * @return  a string representation of this agent
	 **/
	@ProbeID()
	public String toString() {	
		return getClass().getName()+" "+getAgentID();
	}

	/**
	 * Returns a string representing the state of this agent. This 
	 * method is intended to be used for debugging purposes, and the 
	 * content and format of the returned string are left to the implementing 
	 * concrete subclasses. The returned string may be empty but may not be 
	 * <code>null</code>.
	 * 
	 * @return a string representation of this agent's state parameters
	 */
	//protected abstract String paramStringReport();


	/**
	 * Returns the agent's ID. 
	 * @return  unique ID number of this agent
	 */
	public long getAgentID(){
		return this.agentID;
	}

	/**
	 * Returns the agent's name. If the name has not been explicitly set, 
	 * the default base name will be used.  
	 * @return  agent's name as string
	 */
	public String getAgentName() {
		if (this.agentName == null && !nameExplicitlySet) {
			this.agentName = this.agentBaseName;
		}
		return this.agentName;
	}

	/**
	 * Sets name of this agent  
	 * @param name the string that is to be this agent's name
	 * @see #getName
	 */
	public void setAgentName(String name) {
		this.agentName = name;
		nameExplicitlySet = true;
	}



	/**
	 * Returns the net demand <code>netDemand</code> for this agent 
	 * @return  the <code>netDemand</code> 
	 **/
	public float getNetDemand() {
		return this.netDemand;
	}
	/**
	 * Sets the <code>netDemand</code> of this agent  
	 * @param nd the new net demand for the agent
	 * @see #getNetDemand
	 */
	public void setNetDemand(float nd) {
		this.netDemand = nd;
	}
	
	
	public float getCurrentPriceSignal()
	{
		double time = RepastEssentials.GetTickCount();
		return priceSignal[(int) time % priceSignalLength];
	} 

	/*
	 * Step behaviour
	 */

	/******************
	 * This method defines the step behaviour of an aggregator agent
	 * 
	 * Input variables: 	none
	 * 
	 * Return variables: 	boolean returnValue - returns true if the 
	 * 						method executes succesfully
	 ******************/
	@ScheduledMethod(start = 0, interval = 1, shuffle = true, priority = ScheduleParameters.LAST_PRIORITY)
	public boolean step() {

		// Define the return value variable.
		boolean returnValue = true;

		// Note the simulation time if needed.
		double time = RepastEssentials.GetTickCount();
		int timeOfDay = (int) (time % ticksPerDay);

		List<ProsumerAgent> customers = new Vector<ProsumerAgent>();
		List<RepastEdge> linkages = RepastEssentials.GetOutEdges("CascadeContextMain/economicNetwork", this);
		if(Consts.DEBUG) {
			System.out.println("Agent " + agentID + " has " + linkages.size() + "links in economic network");
		}
		for (RepastEdge edge : linkages) {
			Object linkSource = edge.getTarget();
			if (linkSource instanceof ProsumerAgent){
				customers.add((ProsumerAgent) linkSource);    		
			}
			else
			{
				throw (new WrongCustomerTypeException(linkSource));
			}
		}

		float sumDemand = 0;
		for (ProsumerAgent a : customers)
		{
			sumDemand = sumDemand + a.getNetDemand();
		}

		setNetDemand(sumDemand);
		//Set the predicted demand for next day to the sum of the demand at this time today.
		//TODO: This is naive
		
		System.out.println("Setting predicted demand at " + timeOfDay + " to " + sumDemand);
		predictedCustomerDemand[timeOfDay] = sumDemand;

		//TODO I've started too complicated here - first put out flat prices (as per today), then E7, then stepped ToU, then a real dynamic one like this...
		
		//setPriceSignalFlatRate(125f);
		//setPriceSignalEconomySeven(125f, 48f);
		
		// Co-efficients estimated from Figure 4 in Roscoe and Ault
		setPriceSignalRoscoeAndAult(0.0006f, 12f, 40f);
		
		//Here, we simply broadcast the electricity value signal each midnight
		if (timeOfDay == 0) {

			int broadcastLength; // we may choose to broadcast a subset of the price signal, or a repeated pattern
			broadcastLength = priceSignal.length; // but in this case we choose not to

			broadcastDemandSignal(customers, time, broadcastLength);
		}    	

		// Return the results.
		return returnValue;

	}
	
	// ------------------------------
	
	void setPriceSignalFlatRate(float price)
	{
		float[] oldPrice = priceSignal;
		Arrays.fill(priceSignal, price);
		priceSignalChanged = Arrays.equals(priceSignal, oldPrice);
	}
	
	void setPriceSignalEconomySeven(float highprice, float lowprice)
	{
		int morningChangeTimeIndex = (int) (ticksPerDay / (24 / 7.5));
		int eveningChangeTimeIndex = (int) (ticksPerDay / (24 / 23.5));
		float[] oldPrice = priceSignal;
		Arrays.fill(priceSignal, 0, morningChangeTimeIndex, lowprice);
		Arrays.fill(priceSignal, morningChangeTimeIndex + 1, eveningChangeTimeIndex, highprice);
		Arrays.fill(priceSignal, eveningChangeTimeIndex + 1, priceSignal.length - 1, lowprice);
		priceSignalChanged = Arrays.equals(priceSignal, oldPrice);
	}
	
	void setPriceSignalRoscoeAndAult(float A, float B, float C)
	{
		float price;
		float x;
		
		for (int i = 0; i < priceSignalLength; i++)
		{	
			//Note that the division by 10 is to convert the units of predicted customer demand
			//to those compatible with capacities expressed in GW.
			//TODO: unify units throughout the model
			x = (predictedCustomerDemand[i % ticksPerDay] / 10 ) / (Consts.MAX_SUPPLY_CAPACITY_GWATTS - Consts.MAX_GENERATOR_CAPACITY_GWATTS);
			price = (float) (A * Math.exp(B * x) + C);
			System.out.println("Price at tick" + i + " is " + price);
			if (price > Consts.MAX_SYSTEM_BUY_PRICE_PNDSPERMWH) 
			{
				price = Consts.MAX_SYSTEM_BUY_PRICE_PNDSPERMWH;
			}
			priceSignal[i] = price;
		}
		priceSignalChanged = true;
	}
	
	void setPriceSignalExpIncreaseOnOverCapacity(int time)
	{
		//This is where we may alter the signal based on the demand
		// In this simple implementation, we simply scale the signal based on deviation of 
		// actual demand from projected demand for use next time round.
		
		//Define a variable to hold the aggregator's predicted demand at this instant.
		float predictedInstantaneousDemand;
		// There are various things we may want the aggregator to do - e.g. learn predicted instantaneous
		// demand, have a less dynamic but still non-zero predicted demand 
		// or predict zero net demand (i.e. aggregators customer base is predicted self-sufficient
		
		//predictedInstantaneousDemand = predictedCustomerDemand[(int) time % predictedCustomerDemandLength];
		predictedInstantaneousDemand = 0;
		
		if (netDemand > predictedInstantaneousDemand) {
			priceSignal[(int) time % priceSignalLength] = (float) (priceSignal[(int) time % priceSignalLength] * ( 1.25 - Math.exp(-(netDemand - predictedInstantaneousDemand))));
			// Now introduce some prediction - it was high today, so moderate tomorrow...
			if (priceSignalLength > ((int) time % priceSignalLength + ticksPerDay))
			{
				priceSignal[(int) time % priceSignalLength + ticksPerDay] = (float) (priceSignal[(int) time % priceSignalLength + ticksPerDay] * ( 1.25 - Math.exp(-(netDemand - predictedInstantaneousDemand))));
			}
			priceSignalChanged = true; }
	}
	
	/*
	 * helper methods
	 */
	private void broadcastDemandSignal(List<ProsumerAgent> broadcastCusts, double time, int broadcastLength) {


		// To avoid computational load (and realistically model a reasonable broadcast strategy)
		// only prepare and transmit the price signal if it has changed.
		if(priceSignalChanged)
		{
			//populate the broadcast signal with the price signal starting from now and continuing for
			//broadcastLength samples - repeating copies of the price signal if necessary to pad the
			//broadcast signal out.
			float[] broadcastSignal= new float[broadcastLength];
			int numCopies = (int) Math.floor((broadcastLength - 1) / priceSignalLength);
			int startIndex = (int) time % priceSignalLength;
			System.arraycopy(priceSignal,startIndex,broadcastSignal,0,priceSignalLength - startIndex);
			for (int i = 1; i <= numCopies; i++)
			{
				int addIndex = (priceSignalLength - startIndex) * i;
				System.arraycopy(priceSignal, 0, broadcastSignal, addIndex, priceSignalLength);
			}

			if (broadcastLength > (((numCopies + 1) * priceSignalLength) - startIndex))
			{
				System.arraycopy(priceSignal, 0, broadcastSignal, ((numCopies + 1) * priceSignalLength) - startIndex, broadcastLength - (((numCopies + 1) * priceSignalLength) - startIndex));
			}

			for (ProsumerAgent a : broadcastCusts){
				// Broadcast signal to all customers - note we simply say that the signal is valid
				// from now currently, in future implementations we may want to be able to insert
				// signals valid at an offset from now.
				if (Consts.DEBUG)
				{
					//System.out.println("Broadcasting to " + a.sAgentID);
				}
				a.receiveValueSignal(broadcastSignal, broadcastLength);
			}
		}

		priceSignalChanged = false;
	}
	
	

}
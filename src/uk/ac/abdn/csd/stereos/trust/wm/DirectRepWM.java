package uk.ac.abdn.csd.stereos.trust.wm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.exceptions.InvalidParametersException;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

/**
 * This class extends the basic direct weighted mean method to incorporate
 * reputational information sources as well. It uses a simple model where the
 * agent asks a number of other agents in its list of a acquaintences for
 * opinions about a particular target agent. The maximum number of agents to ask
 * is given by max_queries. If this value is negative, all agents will be asked.
 * 
 * @author cburnett
 * 
 */
public class DirectRepWM extends DirectWM
{

	/**
	 * Create a new trust model with the given number of maximum queries
	 * 
	 * @param maxQueries
	 */
	public DirectRepWM()
	{
		super();
	}

	/**
	 * Create a new trust model, specifying the maximum queries, and the
	 * halflife and temperature values for the recency weighting and exploration
	 * functions respectively.
	 * 
	 * @param maxQueries
	 *            (DISABLED for now - ALL agents in the acquaintence set will be
	 *            queried)
	 * @param temp
	 * @param halfLife
	 * @throws InvalidParametersException
	 *             if the given parameters are invalid
	 */
	public DirectRepWM(double temp, int halfLife) throws InvalidParametersException
	{
		super(temp, halfLife);
	}

	/**
	 * Evaluate a set of agents at a given point in time. For reputation, an
	 * agent must not only consider its own experience base, but has the option
	 * to query other agents for opinions
	 * 
	 * TODO - go by the fire model. We are not using transitive discounting
	 * (like FIRE) and we are not using reputation chains (like josang) (we
	 * could actually include them easily) Evaluate ALL agents in the set
	 * reputationally - this may be wasteful but it allows us to capture the
	 * difference between the adhoc and global cases
	 */
	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, int time)
	{

		Map<Agent, Double> results = new HashMap<Agent, Double>();
		// for all the agents we can communicate with...
		for (Agent a : agents) {
			// calculate a reputation value for this agent by quering available
			// recommendation providers
			double repRating = calculateReputation(recommenders.get(a), a, time);

			// if we have a direct rating for this agent
			if (ratings.containsKey(a))
				// get the rating, taking into account reputation
				results.put(a, ratings.get(a) + repRating);
			else
				results.put(a, repRating);
		}

		return results;
		//		
		// if(boltzmann.exploit(time,meanRating,bestRating)) {
		// if(bestAgent != null) return bestAgent;
		// }

		// If the best agent is still unnassigned (i.e. we don't have ANY
		// experiences), just pick a random one
	}

	/**
	 * Calculate a reputation value for this agent
	 * 
	 * @param recommenders
	 *            a list of agents who will act as recommenders
	 * @param a
	 *            the agent to be evaluated
	 * @param time
	 *            time point at which evaluation will be made
	 * @return a mean rating for agent <i>a</i> accross the surveyed population
	 *         (<i>recommenders</i>)
	 */
	private double calculateReputation(List<Agent> recommenders, Agent a, int time)
	{
		// sum of ratings gathered so far
		double sum = 0;
		// double recommenderCount = 0;

		// reputation rating is a function of the mean x variance

		List<Double> ratings = new ArrayList<Double>();

		// Query all the recommenders
		for (Agent r : recommenders) {
			// We won't be asking the agent for opinions about itself :)
			if (!r.equals(a)) {
				double thisRating = r.query(a);
				// if the agent actually has a value to provide
				if (thisRating >= 0) {
					// add it to our sum
					sum += thisRating;
					// and ratings list
					ratings.add(thisRating);
				}
			}
		}
		// now we have a list of data...
		// we want to calculate a measure of the variance
		// for the moment we will use the standard deviation for this
		StandardDeviation sd = new StandardDeviation();
		double[] data = new double[ratings.size()];
		for (int i = 0; i < ratings.size(); i++)
			data[i] = ratings.get(i).doubleValue();
		double variance = sd.evaluate(data);

		double wmean = sum / ratings.size();

		// return the arithmentic mean; weighting will have already been
		// performed by individual agents
		return wmean * (1 - variance);
	}
}

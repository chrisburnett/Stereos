package uk.ac.abdn.csd.stereos.trust.sl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.exceptions.InvalidParametersException;

import uk.ac.abdn.csd.stereos.util.Pair;
import uk.ac.abdn.csd.stereos.util.Utilities;

/**
 * A trust model using the beta reputaion model, and considering both direct
 * trust and reputation.
 * 
 * @author Chris Burnett
 * 
 */
public class DirectRepSL extends DirectSL
{

	public DirectRepSL(double temperature, int halfLife) throws InvalidParametersException
	{
		super(temperature, halfLife);
	}

	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, int time)
	{
		Map<Agent, Double> results = new HashMap<Agent, Double>();
		for (Agent a : agents) {
			results.put(a, evaluate(a, recommenders.get(a), time));
		}
		return results;
	}

	/**
	 * evaluate a single agent
	 */
	private double evaluate(Agent a, List<Agent> recommenders, int time)
	{
		// calculate a reputation value for this agent by quering available
		// recommendation providers for their combined evidence tuples
		Pair<Double, Double> repEvidence = Utilities.aggregateReputation(recommenders, a, time);
		double totalPositives = repEvidence.a;
		double totalNegatives = repEvidence.b;

		// if we have a direct evidence about this agent include it
		if (evidence.containsKey(a)) {
			// get the rating, taking into account reputation
			Pair<Double, Double> dirEvidence = evidence.get(a);
			// add it to our total evidence tuple
			totalPositives += dirEvidence.a;
			totalNegatives += dirEvidence.b;
		}

		Opinion op;
		// if the agent already has an opinion, retrieve and update it
		if (opinions.containsKey(a)) {
			op = opinions.get(a);
			op.setPositives(totalPositives);
			op.setNegatives(totalNegatives);
		} else {
			op = new Opinion(totalPositives, totalNegatives);
			opinions.put(a, op);
		}

		// calculate a reputation rating from probability expectation
		return op.getExpectationValue();
	}

	/**
	 * Evaluate an agent with respect to a particular effort level (for
	 * monitoring). TODO: at the moment, we only allow direct evidence to be
	 * used in the evaluation of effort-level specific trust behaviours. So,
	 * this method simply calls the direct effort-level specific evaluation
	 * method in the superclass.
	 */
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, EffortLevel effort,
			int time)
	{
		return super.evaluate(agents, recommenders, effort, time);
	}
}

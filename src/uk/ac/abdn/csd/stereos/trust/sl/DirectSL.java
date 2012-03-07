package uk.ac.abdn.csd.stereos.trust.sl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.agents.Experience;
import uk.ac.abdn.csd.stereos.exceptions.InvalidParametersException;
import uk.ac.abdn.csd.stereos.util.Boltzmann;
import uk.ac.abdn.csd.stereos.util.Pair;
import uk.ac.abdn.csd.stereos.util.Utilities;
import uk.ac.abdn.csd.stereos.trust.TrustModel;

/**
 * A trust model considering direct evidence only, and using BRS (beta
 * reputation) for belief formation.
 * 
 * @author Chris Burnett
 * 
 */
public class DirectSL extends TrustModel
{

	/**
	 * The Boltzmann exploration function for this trust model
	 */
	protected Boltzmann boltzmann;

	/**
	 * Population mean rating
	 */
	protected double meanRating;

	/**
	 * Lambda weight for recency function
	 */
	// private double lambda;

	/**
	 * Evidence storage. Store the positive and negative experiences with each
	 * agent
	 */
	protected Map<Agent, Pair<Double, Double>> evidence;
	// bit of redundancy here...
	// protected Map<Agent,Map<EffortLevel,Pair<Double,Double>>> effortEvidence;
	protected Map<Agent, Opinion> opinions;

	/**
	 * Create a new DirectSL instance with the given recency weight half-life
	 * and boltzmann temperature
	 * 
	 * @param halfLife
	 * @param temperature
	 * @throws InvalidParametersException
	 */
	public DirectSL(double temperature, int halfLife) throws InvalidParametersException
	{
		super();

		// setup directly observed evidence store
		evidence = new HashMap<Agent, Pair<Double, Double>>();
		// effortEvidence = new
		// HashMap<Agent,Map<EffortLevel,Pair<Double,Double>>>();
		// setup opinions store
		opinions = new HashMap<Agent, Opinion>();
		// create a chache of unchanged opinions
		ratings = new HashMap<Agent, Double>();
		meanRating = 0.0;
		// must be between 0 and 1
		if (temperature > 1 || temperature < 0)
			throw new InvalidParametersException();

		// set up the boltzmann class
		boltzmann = new Boltzmann(temperature);

	}

	/**
	 * Add a new experience, updating the total evidence/rating/opinion caches,
	 * and the effort evidence cache
	 */
	public void addExperience(Experience e)
	{
		// need to update the positives or negatives for the particular agent
		// the experience is with
		// then create a cached opinion value using the beta function
		// as usual, we only need to update our opinions when a new experience
		// is added.
		// evaluation then requires a lookup, and not calculation
		experienceBase.add(e);

		double evaluation = e.getEvaluation();
		Agent trustee = e.getTrustee();

		double positives = 0, negatives = 0;

		// get the existing (total) direct evidence, if there is
		if (evidence.containsKey(trustee)) {
			// otherwise get the (total) direct evidence
			Pair<Double, Double> directEvidence = evidence.get(trustee);
			positives = directEvidence.a;
			negatives = directEvidence.b;
		}

		// if eval was positive,increment positives, otherwise negatives
		if (evaluation >= 0) {
			positives++;
		} else {
			negatives++;
		}

		Opinion op;
		// if we have an opinion stored, update it - protect the base rate
		if (opinions.containsKey(trustee)) {
			op = opinions.get(trustee);
			op.setPositives(positives);
			op.setNegatives(negatives);
		} else {
			// otherwise add a new one
			op = new Opinion(positives, negatives);
			op.setBaseRate(defaultPrior);
			opinions.put(trustee, op);
		}
		// update total direct evidence store
		evidence.put(trustee, new Pair<Double, Double>(positives, negatives));

		// calculate cached personal rating
		double rating = op.getExpectationValue();
		ratings.put(trustee, rating);
		// update the mean rating
		meanRating = Utilities.calculatePopulationMeanPerformance(ratings);
	}

	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, int time)
	{
		Map<Agent, Double> results = new HashMap<Agent, Double>();
		for (Agent a : agents) {
			// If we have a rating for this agent
			if (ratings.containsKey(a))
				// get the rating
				results.put(a, ratings.get(a));
			else
				results.put(a, defaultPrior);
		}
		return results;
	}

	/**
	 * Just returns the unconditional trust, if this method is called.
	 */
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, EffortLevel effort,
			int time)
	{
		// just bypass
		return evaluate(agents, recommenders, time);
	}


	/**
	 * Return the trust value of an agent
	 */
	public double evaluate(Agent a, Map<Agent, List<Agent>> recommenders, int time)
	{
		if (ratings.containsKey(a))
			return ratings.get(a);
		return defaultPrior; // return total ignorance equivalent
	}

	@Override
	public Opinion opinionQuery(Agent a)
	{
		if (opinions.containsKey(a))
			return new Opinion(opinions.get(a));
		else
			return new Opinion(0.0, 0.0);
	}

	@Override
	public Pair<Double, Double> evidenceQuery(Agent a)
	{
		if (evidence.containsKey(a))
			return evidence.get(a);
		else
			return new Pair<Double, Double>(0.0, 0.0);
	}

	@Override
	public Map<Agent, Opinion> getOpinions()
	{
		Map<Agent, Opinion> opinionsCopy = new HashMap<Agent, Opinion>();
		for (Entry<Agent, Opinion> e : opinions.entrySet())
			opinionsCopy.put(e.getKey(), new Opinion(e.getValue()));
		return opinionsCopy;
	}

	@Override
	public void forget()
	{
		evidence.clear();
		opinions.clear();
	}

	// this model does not pay attention to effort levels alone
	@Override
	public Map<String, Map<Agent, Double>> conditionallyEvaluate(List<Agent> candidates,
			Map<Agent, List<Agent>> filteredRecommenders, int timeStep)
	{
		return null;
	}

}

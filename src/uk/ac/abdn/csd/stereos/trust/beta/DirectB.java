package uk.ac.abdn.csd.stereos.trust.beta;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.agents.Experience;
import uk.ac.abdn.csd.stereos.exceptions.InvalidParametersException;
import uk.ac.abdn.csd.stereos.trust.TrustModel;
import uk.ac.abdn.csd.stereos.util.Boltzmann;
import uk.ac.abdn.csd.stereos.util.Pair;
import uk.ac.abdn.csd.stereos.util.Utilities;

//import org.apache.commons.math.distribution.BetaDistributionImpl;

/**
 * A trust model considering direct evidence only, and using BRS (beta
 * reputation) for belief formation.
 * 
 * @author Chris Burnett
 * 
 */
public class DirectB extends TrustModel
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
	 * agent (TODO - we are not using any kind of evidence 'strength' at this
	 * point)
	 */
	protected Map<Agent, Pair<Double, Double>> evidence;

	/**
	 * The beta distribution object that will be at the core of producing SL
	 * opinions and integrating new evidence
	 */
	// private BetaDistributionImpl beta;

	/**
	 * Create a new DirectSL instance with the given recency weight half-life
	 * and boltzmann temperature
	 * 
	 * @param halfLife
	 * @param temperature
	 * @throws InvalidParametersException
	 */
	public DirectB(double temperature, int halfLife) throws InvalidParametersException
	{
		super();

		// setup evidence store
		evidence = new HashMap<Agent, Pair<Double, Double>>();
		// create a chache of unchanged opinions
		ratings = new HashMap<Agent, Double>();
		meanRating = 0.0;
		// must be between 0 and 1
		if (temperature > 1 || temperature < 0)
			throw new InvalidParametersException();

		// set up our recency weighting values
		// TODO - might not use this for the SL system, but should
		// use it anyway
		// lambda = -(halfLife/(Math.log(0.5)));

		// set up the boltzmann class
		boltzmann = new Boltzmann(temperature);

		// create the all important beta distribution object
		// initialise it with no evidence
		// we initialise with 1,1 because of alpha = r+1 and beta = s+1 (from
		// Josang's
		// BRS paper)
		// beta = new BetaDistributionImpl(1,1);
	}

	/**
	 * Add a new experience
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

		double positives = 0;
		double negatives = 0;

		// if this is the first experienc we have with this agent, init a new
		// pair
		if (!evidence.containsKey(trustee))
			evidence.put(trustee, new Pair<Double, Double>(0.0, 0.0));
		else {
			// otherwise get the evidence counts
			Pair<Double, Double> evidenceTotals = evidence.get(trustee);
			positives = evidenceTotals.a;
			negatives = evidenceTotals.b;
		}

		// if eval was positive,increment positives, otherwise negatives
		if (evaluation >= 0)
			positives += evaluation;
		else
			negatives -= evaluation;

		// update the evidence
		evidence.put(trustee, new Pair<Double, Double>(positives, negatives));

		// we only really need to consider the beta function if we want
		// to get the probability that we will observe a value in a particular
		// range. This is useful when we want to compare it with our expectation
		// set up beta
		// beta.setAlpha(positives+1);
		// beta.setBeta(negatives+1);

		// the 'rating' in BRS seems to be the probability expectation value
		// that comes from the beta function. In the paper they normalise it, so
		// lets take the same approach here, and cache the value we get

		// However, since we have an expectation, we can try calculating the
		// probability
		// that the expectation will be in that range

		double rating = calculateProbabilityExpectation(positives, negatives);
		ratings.put(trustee, rating);
		// update the mean rating
		meanRating = Utilities.calculatePopulationMeanPerformance(ratings);
	}

	/**
	 * Calculate the rating as the probability expectation value
	 * 
	 * @param positives
	 * @param negatives
	 * @return the PE value
	 */
	protected double calculateProbabilityExpectation(double positives, double negatives)
	{
		// NOTE: BRS doesn't seem to consider confidence or variance of an
		// opinion
		// although it probably does once the uncertainty dimension is included.
		// for this model of BETA/TRAVOS like systems, I'm just going to model
		// variance/confidence
		// as a the variance of the expected value. To be fair, TRAVOS uses a
		// more sophisticated
		// metric based on confidence intervals of the expected value, but I
		// don't really have time
		// at the moment to implement it.
		double variance = (positives * negatives)
				/ (((positives + negatives) * (positives + negatives)) * (positives + negatives + 1));
		double expectation = (positives / (positives + negatives));
		double rating = expectation * (1 - variance);
		// return (positives-negatives)/(positives + negatives + 2);
		return rating;
	}


	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, int time)
	{

		// Very simple evaluation model - everything we need is already cached,
		// so just return it.

		Map<Agent, Double> results = new HashMap<Agent, Double>();
		for (Agent a : agents) {
			// If we have a rating for this agent
			if (ratings.containsKey(a))
				// get the rating
				results.put(a, ratings.get(a));
			else
				results.put(a, 0.5);
		}
		return results;
	}

	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, EffortLevel effort,
			int time)
	{
		return evaluate(agents, recommenders, time);
	}

	@Override
	public Map<String, Map<Agent, Double>> conditionallyEvaluate(List<Agent> candidates,
			Map<Agent, List<Agent>> filteredRecommenders, int timeStep)
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void forget()
	{
		// TODO Auto-generated method stub
		
	}
}

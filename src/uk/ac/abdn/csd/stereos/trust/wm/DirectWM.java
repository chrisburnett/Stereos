package uk.ac.abdn.csd.stereos.trust.wm;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.agents.Experience;
import uk.ac.abdn.csd.stereos.exceptions.InvalidParametersException;
import uk.ac.abdn.csd.stereos.trust.TrustModel;
import uk.ac.abdn.csd.stereos.util.Boltzmann;
import uk.ac.abdn.csd.stereos.util.Utilities;

import org.apache.commons.math.stat.descriptive.moment.StandardDeviation;

/**
 * A trust model which considers direct experiences only, using weighted mean as
 * its underlying computational model. When evaluating an agent, the model
 * calculates the mean of all experiences, weighted by recency.
 * 
 * Essentially this is using the direct trust approach employed in both Regret
 * and FIRE. We incorporate the Botlmann (spelling) Exploration Strategy to
 * prevent agents just using the same partners all the time.
 * 
 * NOTE: This model emulates traditional direct trust mechanisms. It is not
 * probabilistic, and therefore does not use a performance evaluation function
 * to arrive at success/fail judgements. The raw (real) outcome is used to
 * calculate trust. Therefore, some care should be taken when comparing the
 * performance of this type of model with probabilistic ones.
 * 
 * @author Chris Burnett
 * 
 */
public class DirectWM extends TrustModel
{

	/**
	 * The number of timesteps it takes to half an opinion's weight
	 */
	private int halfLife;

	/**
	 * Lambda weight for recency function
	 */
	protected double lambda;

	/**
	 * Cached mean performance value
	 */
	protected double meanRating;

	/**
	 * The Boltzmann exploration function for this trust model
	 */
	protected Boltzmann boltzmann;

	/**
	 * Set up the trust model with a default temperature (exploration) value of
	 * 0.8 and an experience half-life of 5.
	 */
	public DirectWM()
	{
		super();
		meanRating = 0;
		halfLife = 5;

		try {
			boltzmann = new Boltzmann(0.8);
		} catch (InvalidParametersException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		lambda = -(this.halfLife / (Math.log(0.5)));
	}

	/**
	 * Set up the trust model with the given temp and halflife values.
	 * 
	 * @param temp
	 *            - must be between 0 and 1.
	 * @param halfLife
	 *            number of time steps to half the weight give to an experience
	 * @throws InvalidParametersException
	 *             if the temperature value is not between 0 and 1
	 */
	public DirectWM(double temp, int halfLife) throws InvalidParametersException
	{
		super();
		meanRating = 0;

		// Just set the default of 0.8 if an invalid value is given
		// TODO - this should really throw an exception instead of silently
		// setting something else - but that's not so important right now

		double temperature;
		if (temp > 1 || temp < 0)
			temperature = 0.8;
		else
			temperature = temp;

		this.halfLife = halfLife;
		lambda = -(this.halfLife / (Math.log(0.5)));
		boltzmann = new Boltzmann(temperature);
	}

	/**
	 * Evaluate a set of agents at a given point in time. In this model, since
	 * updating is done when new experiences are added, the <i>evaluation</i> is
	 * simply a lookup of the ratings we currently have for the given agents,
	 * and the selection of the most trusted.
	 * 
	 * This class recieves recommenders but does not use them. It is purely for
	 * contrast with non-reputational systems
	 */
	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, int time)
	{
		Map<Agent, Double> results = new HashMap<Agent, Double>();
		for (Agent a : agents) {
			if (ratings.containsKey(a))
				results.put(a, ratings.get(a));
			else
				results.put(a, 0.0);
		}
		return results;

	}

	/**
	 * Add a new experience and update the trust model. Here we use the recency
	 * scaling method of FIRE
	 * 
	 * @param experience
	 *            the experience to add.
	 */
	public void addExperience(Experience experience)
	{
		// get the current time step
		int currentTimeStep = experience.getTimeStep();
		// First, add the experience
		experienceBase.add(experience);
		// ...then update the rating
		Agent trustee = experience.getTrustee();
		// recalculate trust for this trustee on the basis of the new
		// information
		// maintain how many experiences we have with this trustee
		int ecount = 0;
		// it seems that according to FIRE, variance should be calculated
		// after recency weighting ect has been applied
		// data array for calculating the mean variance
		double data[] = new double[experienceBase.size()];
		// Recalculate weighted mean every time a new experience is added
		for (Experience e : experienceBase) {
			// filter out only experiences to do with the agent in question
			if (e.getTrustee().equals(trustee)) {
				// TIME WEIGHTING
				// re-weight according to time difference
				// recent ratings receive more weight
				// double tweight = Math.exp(-(((currentTimeStep -
				// e.getTimeStep())+1) + 1) / lambda);

				double tweight = Math.exp(-((currentTimeStep - e.getTimeStep()) / lambda));

				// perform all the time weightings
				data[ecount++] = tweight * e.getObservation();
			}
		}
		// calculate the weighted mean rating
		double sum = 0;
		for (double d : data)
			sum += d;
		// divide the data we have by the total number of experiences with this
		// agent
		double wmean = sum / ecount;

		// calculate st.dev (variance approximation)
		StandardDeviation sd = new StandardDeviation();
		double variance = sd.evaluate(data);
		// the rating is the weighted mean * variance
		double rating = wmean * (1 - variance);
		// add the new rating for this agent
		ratings.put(trustee, rating);
		// recalculate the cached mean for the total population
		meanRating = Utilities.calculatePopulationMeanPerformance(ratings);
	}

	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, EffortLevel effort,
			int time)
	{
		// TODO Auto-generated method stub
		return null;
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

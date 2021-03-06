package uk.ac.abdn.csd.stereos.trust;

import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Random;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.agents.Experience;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;
import uk.ac.abdn.csd.stereos.util.Pair;

/**
 * This interface specifies the common methods a trust model should implement
 * 
 * @author Chris Burnett
 * 
 */
public abstract class TrustModel
{

	/**
	 * The structure that will store the agent's experiences
	 */
	protected List<Experience> experienceBase;

	/**
	 * Structure storing the cached (reduced) rating values
	 */
	protected Map<Agent, Double> ratings;

	/**
	 * Shared random instance
	 */
	protected Random random;

	/**
	 * The default prior that all opinions will be created with. This is
	 * essentially \bar{\alpha} from the thesis - the initial degree of
	 * ambiguity aversion, when no stereotype has been formed
	 */
	protected double defaultPrior;

	// Default constructor
	public TrustModel()
	{
		// initialise the experience base and lookup
		experienceBase = new ArrayList<Experience>();
		ratings = new HashMap<Agent, Double>();
		random = new Random();
		defaultPrior = 0.5;
	}

	/**
	 * Produce trust evaluations for the given agents. Note - this is the new
	 * method - the rest are deprecated.
	 * 
	 * @param agents
	 * @param filteredRecommenders
	 * @param time
	 * @return a mapping of agents to trust evaluations
	 */
	public abstract Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> filteredRecommenders,
			int time);

	/**
	 * Record an experienced outcome of a delegation relationship
	 * 
	 * @param experience
	 *            the experience object representing this experience.
	 */
	public void addExperience(Experience experience)
	{
		experienceBase.add(experience);
	}

	/**
	 * Pick a random agent from the list
	 * 
	 * @param agents
	 * @return a randomly selected agent from the given list
	 */
	protected Agent selectRandomAgent(List<Agent> agents)
	{
		// If the best agent is still unnassigned (i.e. we don't have ANY
		// experiences), just pick a random one
		// and if the exploration strategy is in play
		int randomAgent = random.nextInt(agents.size());
		return agents.get(randomAgent);
	}

	/**
	 * Return this model's experience base
	 * 
	 * @return the experience base
	 */
	public List<Experience> getExperienceBase()
	{
		return experienceBase;
	}

	/**
	 * Return a calculated rating for this agent when asked by another agent.
	 * 
	 * @param a
	 *            the agent being evaluated
	 * @return the rating
	 */
	public double query(Agent a)
	{
		if (ratings.containsKey(a))
			return ratings.get(a);
		else
			return -1;
	}

	/**
	 * Return an evidence tuple for the target agent.
	 * 
	 * @param a
	 *            the target agent
	 * @return an evidence tuple, or null if this is not appropriate for the
	 *         implementing trust model.
	 */
	public Pair<Double, Double> evidenceQuery(Agent a)
	{
		// if this method is not overridden, null will be returned
		return null;
	}

	/**
	 * Return an opinion triple (b,d,u) for the target agent.
	 * 
	 * @param a
	 *            the target agent
	 * @return an evidence tuple, or null if this is not appropriate for the
	 *         implementing trust model.
	 */
	public Opinion opinionQuery(Agent a)
	{
		// by default, unless overridden by models which use opinions
		return null;
	}

	/**
	 * Return the set of opinions held by this agent, in entirety
	 * 
	 * @return
	 */
	public Map<Agent, Opinion> getOpinions()
	{
		// null by default, unless overidden
		return null;
	}

	/**
	 * Return a stereotypical base rate bias value for subjective logice
	 * stereotyping agents.
	 * 
	 * @param trustee
	 * @return a base rate produced by a stereotype
	 */
	public double stereotypeQuery(Agent trustee)
	{
		// by default, unless overridden. 0.5 represents no information
		return 0.5;
	}

	/**
	 * Return a value between 0 and 1 representing the confidence of this model.
	 * If it is not overridden, the default will be 1 (totally confident)
	 * 
	 * @return
	 */
	public double confidenceQuery()
	{
		return 1.0;
	}

	/**
	 * Get the confidence of the model at a particular time point.
	 * 
	 * @param index
	 * @return
	 */
	public double confidenceQuery(int index)
	{
		return 1.0;
	}

	public abstract void forget();

	public abstract Map<String, Map<Agent, Double>> conditionallyEvaluate(List<Agent> candidates,
			Map<Agent, List<Agent>> filteredRecommenders, int timeStep);

	// returns null by default - for models that do not provide this function,
	// should just return nothing.

	/*
	 * Placeholder
	 */
	public abstract Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders,
			EffortLevel effort, int time);

	public double getDefaultPrior()
	{
		return defaultPrior;
	}

	public void setDefaultPrior(double defaultPrior)
	{
		this.defaultPrior = defaultPrior;
	}

	/**
	 * Unless overridden, just returns the same as the usual opinion query
	 * 
	 * @param candidate
	 * @param effort
	 * @return
	 */
	public Opinion opinionQuery(Agent candidate, String effort)
	{
		return null;
	}
}

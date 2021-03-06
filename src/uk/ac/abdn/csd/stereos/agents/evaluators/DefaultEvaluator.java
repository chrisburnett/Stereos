package uk.ac.abdn.csd.stereos.agents.evaluators;

import uk.ac.abdn.csd.stereos.agents.Agent;

/**
 * A simple performance evaluator implementation which uses a hard threshold to
 * determine satisfaction or disappointment.
 * 
 * @author Chris Burnett
 * 
 */
public class DefaultEvaluator implements PerformanceEvaluator
{

	/**
	 * The threshold value for thie evaluator
	 */
	private double threshold;

	public DefaultEvaluator(double threshold)
	{
		super();
		this.threshold = threshold;
	}

	/**
	 * Get an evaluation from the evaluator.
	 */
	public double evaluate(Agent trustee, double observation)
	{
		if (observation >= this.threshold)
			return OUTCOME_SUCCESS;
		else
			// return -1;
			return OUTCOME_FAILURE;
	}

	public double getThreshold()
	{
		return threshold;
	}

	public void setThreshold(double threshold)
	{
		this.threshold = threshold;
	}

}

package uk.ac.abdn.csd.stereos.agents.evaluators;

import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.PerceptualBias;

/**
 * This class models a subjective evaluation function which is biases against
 * agent features. This is modelled in a very simple way - a different threshold
 * is used depending on some of the features of the evaluated agent. Very
 * similar to the biased selection of effort level (BiasedDM)
 * 
 * @see TrusteeBiasedDM
 * @author cburnett
 * 
 */
public class BiasedEvaluator implements PerformanceEvaluator
{

	private List<PerceptualBias> biases;

	public BiasedEvaluator(List<PerceptualBias> biases)
	{
		this.biases = biases;
	}

	/**
	 * Select a different threshold based on the features of the trustee
	 */
	public double evaluate(Agent trustee, double observation)
	{
		// if multiple biases match, choose the most specific one
		// basically, the bias with the most matching features 'wins'.
		int bestScore = 0;
		double threshold = 0.5; // default

		for (PerceptualBias p : biases) {
			int score = 0;
			Map<String, Integer> trustorFeatures = trustee.getFeatures();
			for (String f : p.getFeatures()) {
				if (trustorFeatures.get(f) != null && trustorFeatures.get(f) == 1)
					score++;
			}
			if (score > bestScore) {
				bestScore = score;
				threshold = p.getThreshold();
			}
		}

		// return observation - threshold;
		if (observation >= threshold)
			return OUTCOME_SUCCESS;
		else
			return OUTCOME_FAILURE;

	}

}

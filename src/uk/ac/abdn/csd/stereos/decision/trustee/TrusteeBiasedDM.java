package uk.ac.abdn.csd.stereos.decision.trustee;

import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.BehaviouralBias;
import uk.ac.abdn.csd.stereos.agents.Delegation;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.agents.Profile;

/**
 * This decision model implements a simple trustee who provides different levels
 * of service to different agents, depending on their features. It therefore
 * represents a kind of 'predjudiced' or biased decision maker. However, it does
 * not take account any contract.
 * 
 * @author cburnett
 * 
 */
public class TrusteeBiasedDM extends TrusteeDecisionModel
{

	private List<BehaviouralBias> biases;

	public TrusteeBiasedDM(List<BehaviouralBias> biasList)
	{
		biases = biasList;
	}

	/**
	 * Return a particular effort level, dictated by the bias and the features
	 * of the trustor
	 * 
	 * @param p
	 *            the profile of the trustor
	 */
	public EffortLevel selectEffort(Delegation del, double reserve, Profile p)
	{
		// if multiple biases match, choose the most specific one
		// basically, the bias with the most matching features 'wins'.
		BehaviouralBias bestBias = null;
		int bestScore = 0;

		for (BehaviouralBias b : biases) {
			int score = 0;
			Map<String, Integer> trustorFeatures = del.getTrustor().getFeatures();
			for (String f : b.getFeatures()) {
				// NOTE: this means we currently only allow conjunctions of
				// features at the moment
				// if the feature is not hidden, and it is observed as present
				if (trustorFeatures.get(f) != null && trustorFeatures.get(f) == 1)
					score++;
			}
			if (score > bestScore) {
				bestBias = b;
				bestScore = score;
			}
		}

		if (bestScore > 0) {
			return bestBias.getEffort();
		} else {
			return p.getEffortLevel(p.getDefaultEffortLevelId());
		}
	}

	public String toString()
	{
		StringBuffer out = new StringBuffer();
		for (BehaviouralBias b : biases) {
			out.append(b.toString());
		}
		return out.toString();
	}

	@Override
	public EffortLevel selectEffort(Delegation del, double reserve, Profile p,
			Map<Agent, Double> unconditionalOpinions, Map<String, Map<Agent, Double>> conditionalOpinions)
	{
		return selectEffort(del, reserve, p);
	}
}

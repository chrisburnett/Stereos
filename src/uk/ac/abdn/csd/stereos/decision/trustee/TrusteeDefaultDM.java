package uk.ac.abdn.csd.stereos.decision.trustee;

import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.Delegation;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.agents.Profile;

/**
 * This decision model simply selects the default effort level.
 * 
 * @author cburnett
 * 
 */
public class TrusteeDefaultDM extends TrusteeDecisionModel
{

	/**
	 * Simply return the default effort level, regardless of the contract etc.
	 */
	@Override
	public EffortLevel selectEffort(Delegation del, double reserve, Profile p,
			Map<Agent, Double> unconditionalOpinions, Map<String, Map<Agent, Double>> conditionalOpinions)
	{
		return p.getEffortLevel(p.getDefaultEffortLevelId());

	}
}

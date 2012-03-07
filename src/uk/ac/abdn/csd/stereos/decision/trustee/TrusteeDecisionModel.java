package uk.ac.abdn.csd.stereos.decision.trustee;

import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.Delegation;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.agents.Profile;

/**
 * This class gives the abstract functionality of the trustee's decision model.
 * This essentially involves making an effort level selection.
 * 
 * @author cburnett
 * 
 */
public abstract class TrusteeDecisionModel
{

	private Agent owner;

	/**
	 * Select an effort level given a simple delegation proposal
	 * 
	 * @param del
	 * @param p
	 * @param conditionalOpinions
	 * @param unconditionalOpinions
	 * @return an effort level
	 */
	public abstract EffortLevel selectEffort(Delegation del, double reserve, Profile p,
			Map<Agent, Double> unconditionalOpinions, Map<String, Map<Agent, Double>> conditionalOpinions);

	public void setOwner(Agent agent)
	{
		this.owner = agent;
	}

	public Agent getOwner()
	{
		return owner;
	}

}

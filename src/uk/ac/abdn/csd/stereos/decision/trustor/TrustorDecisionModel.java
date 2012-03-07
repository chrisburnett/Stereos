package uk.ac.abdn.csd.stereos.decision.trustor;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.Delegation;
import uk.ac.abdn.csd.stereos.trust.TrustModel;

import java.util.Map;

/**
 * This abstract class defines some shared functionality for decision models.
 * 
 * @author cburnett
 * 
 */
public abstract class TrustorDecisionModel
{

	/**
	 * Reference to a trust model, for monitoring and RI calculations
	 */
	protected TrustModel tm;

	public TrustModel getTrustModel()
	{
		return tm;
	}

	public void setTrustModel(TrustModel tm)
	{
		this.tm = tm;
	}

	/**
	 * We maintain a reference to the owner of the trust model so that we can
	 * 'sign' contracts. TODO - REMOVE IF UNNEEDED
	 */
	protected Agent owner;

	/**
	 * The basic partner selection method. Should evaluate a bunch of partners
	 * and select one, specified in a 'contract' which details the incentive
	 * structure for the delegation. For the simple (non-decision-theoretic)
	 * cases, the contract can just specify anything.
	 * 
	 * @param unconditionalOpinions
	 * @param conditionalOpinions
	 * @param uSucc
	 * @param uFail
	 * @param uAbs
	 * @param monitoringCost
	 * @return
	 */
	public abstract Delegation selectAgent(Map<Agent, Double> unconditionalOpinions,
			Map<String, Map<Agent, Double>> conditionalOpinions, Delegation contract);

	/**
	 * Get the 'owner' of this decision model
	 * 
	 * @return
	 */
	public Agent getOwner()
	{
		return owner;
	}

	/**
	 * Set the 'owner' of this decision model
	 * 
	 * @param owner
	 */
	public void setOwner(Agent owner)
	{
		this.owner = owner;
	}

	public abstract Delegation selectAgent(Map<Agent, Double> agents, Delegation contract);

}

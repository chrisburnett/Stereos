package uk.ac.abdn.csd.stereos.decision.trustor;

import java.util.Map;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.Delegation;

/**
 * A decision model which returns the most trusted agent, regardless of the
 * risks and controls.
 * 
 * @author cburnett
 * 
 */
public class TrustorMostTrustedDM extends TrustorDecisionModel
{

	/**
	 * Select the most trusted agent
	 */
	@Override
	public Delegation selectAgent(Map<Agent, Double> agents, Delegation contract)
	{

                double uAbstain = contract.getAbstainPayoff();
                double monitoringCost = contract.getMonitoringCost();


		Agent bestAgent = null;
		double bestRating = 0.0;
		// Work which of the known agents is the best
		// i.e. choose the agent with the highest probability expectation value
		for (Entry<Agent, Double> a : agents.entrySet()) {
			double thisRating = a.getValue();

			// If this agent is the best we have seen so far, store it
			if (thisRating > bestRating) {
				bestRating = thisRating;
				// note this agent as the best agent
				bestAgent = a.getKey();
			}
		}

		// create a simple contract with no meaning, just containing the
		// selected agent
		// pay him whatever he wants
		if (bestAgent == null)
			return null;

		Delegation newContract = new Delegation(owner, bestAgent, contract.getTrustorSuccessPayoff(), contract.getTrustorFailurePayoff(), bestAgent.getAskingPrice(), bestAgent.getAskingPrice(), uAbstain, monitoringCost, uAbstain,
				Delegation.DELEGATION_MOST_TRUSTED);
                newContract.setDelegationPath(contract.getDelegationPath());  // update delegation path to new contract
                newContract.setVisibility(contract.getVisibility());
                return newContract;
	}

	@Override
	public Delegation selectAgent(Map<Agent, Double> unconditionalOpinions,
			Map<String, Map<Agent, Double>> conditionalOpinions, Delegation contract)
	{
		return selectAgent(unconditionalOpinions, contract);
	}
}

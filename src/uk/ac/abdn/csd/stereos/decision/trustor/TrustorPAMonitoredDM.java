package uk.ac.abdn.csd.stereos.decision.trustor;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.Experiment;
import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.Delegation;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.trust.TrustModel;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;

/**
 * This class represents a decision model which produces contracts which will be
 * monitored.
 * 
 * @author cburnett
 * 
 */
public class TrustorPAMonitoredDM extends TrustorDecisionModel
{

	private TrustModel tm = null;

	/**
	 * Sets the link to the trust model. Without this, the decision model cannot
	 * calculate the expected value of monitoring. In this case, the default EV
	 * will be 0.
	 * 
	 * @param tm
	 */
	public void setTrustModel(TrustModel tm)
	{
		this.tm = tm;
	}

	@Override
	/**
	 * Produce a monitored delegation contract
	 */
	public Delegation selectAgent(Map<Agent, Double> unconditionalOpinions,
			Map<String, Map<Agent, Double>> conditionalOpinions, Delegation contract)
	{
		double uAbs = contract.getAbstainPayoff();
                double uSucc = contract.getTrustorSuccessPayoff();
                double uFail = contract.getTrustorFailurePayoff();
                double monitoringCost = contract.getMonitoringCost();
                // best trustee
		Agent bestTrustee = null;
		// best EU of the trustor!
		double myBestEU = uAbs;
		// best effort overall
		EffortLevel theBestEffort = null;
		String theBestEffortID = null;

		double bestTrusteeSuccPayoff = 0;

		Map<Agent, Map<String, Double>> input = Agent.transposeInput(conditionalOpinions);

		// for each candidate
		for (Entry<Agent, Map<String, Double>> a : input.entrySet()) {
			Agent candidate = a.getKey();
			double reserve = candidate.getAskingPrice();

			// best effort level and associated EU for this agent
			EffortLevel bestEffort = null;
			String bestEffortID = null;
			double bestEffortEU = 0.0;
			double trusteeSuccPayoff = 0;

			// for each effort level calculate the minimum contract and store
			for (Entry<String, Double> rating : a.getValue().entrySet()) {
				// this effort ID
				String effortID = rating.getKey();
				// this effort details
				EffortLevel effort = candidate.getProfile().getEffortLevel(effortID);

				// this effort success prob
				double p = rating.getValue();

				// create two 'imaginary' opinions for EVSI calculation (in this
				// effort level)
				// i.e. should I monitor in this effort level?
				Opinion candidateOp = tm.opinionQuery(candidate, effortID);
				Opinion whatIfGood = new Opinion(candidateOp);
				Opinion whatIfBad = new Opinion(candidateOp);
				whatIfGood.setPositives(candidateOp.getPositives() + 1);
				whatIfBad.setNegatives(candidateOp.getNegatives() + 1);

				// get the probability expectations
				double goodRating = whatIfGood.getExpectationValue();
				double badRating = whatIfBad.getExpectationValue();
				// get the maximum
				double futureP = Math.max(goodRating, badRating);

				// auxiliary costs for this effort level
				double invokeCosts = effort.getCost() + reserve;

				// System.err.println(invokeCosts);
				// expected utilities (real)
				double euSucc = (uSucc - invokeCosts) * p;
				double euFail = (uFail - invokeCosts) * (1 - p);
				// monitoring is now expected - no more thinking about it
				double eu = euSucc + euFail - monitoringCost;

				// expected utilities (expected, from monitoring)
				double euSuccMon = (uSucc - invokeCosts) * (futureP);
				double euFailMon = (uFail - invokeCosts) * (1 - futureP);
				double euMon = euSuccMon + euFailMon;

				// expected utility difference
				double euDiff = Math.abs(euMon - eu);
				// add the absolute difference - even a reduction in EU after
				// monitoring is helping us
				eu += euDiff;

				// debug
				// System.out.println(euDiff);
				// if this effort is the best, note it down
				// do the comparison with big decimals
				if (new BigDecimal(eu, Experiment.mathContext).compareTo(new BigDecimal(bestEffortEU,
						Experiment.mathContext)) >= 0) {
					bestEffortEU = eu;
					bestEffortID = effortID;
					bestEffort = effort;
					// monitoring money doesn't go to trustee
					trusteeSuccPayoff = invokeCosts;
				}
			}
			// now mark if the effort level maximises our EU (for this
			// candidate)
			if (new BigDecimal(bestEffortEU, Experiment.mathContext).compareTo(new BigDecimal(myBestEU,
					Experiment.mathContext)) >= 0) {
				myBestEU = bestEffortEU;
				bestTrustee = candidate;
				theBestEffort = bestEffort;
				theBestEffortID = bestEffortID;
				bestTrusteeSuccPayoff = trusteeSuccPayoff;
			}

		}
		// if no it's better to abstain, delegate to ourselves
		if (theBestEffort == null) {
			Delegation newContract = new Delegation(owner, owner, uSucc, uFail, uAbs, uAbs, uAbs, monitoringCost, uAbs, Delegation.DELEGATION_ABSTAIN);
		        newContract.setDelegationPath(contract.getDelegationPath());
                        newContract.setVisibility(contract.getVisibility());
                        return newContract;
                }

		// need to build a structure here - keep the delegation object general
		Map<String, Double> successPayoffs = new HashMap<String, Double>();
		Map<String, Double> failurePayoffs = new HashMap<String, Double>();

		// payoff only obtained if correct effort is observed - forcing contract
		for (String e : bestTrustee.getEfforts().keySet()) {
			if (e.equals(theBestEffortID)) {
				successPayoffs.put(e, bestTrusteeSuccPayoff);
				failurePayoffs.put(e, bestTrusteeSuccPayoff);
			} else {
				successPayoffs.put(e, 0.0);
				failurePayoffs.put(e, 0.0);
			}

		}

		// return the contract, which is just a forcing one on the best effort -
		// fail payoff will always be 0
		Delegation newContract = new Delegation(owner, bestTrustee, uSucc, uFail, successPayoffs, failurePayoffs, uAbs, monitoringCost, myBestEU,
				Delegation.DELEGATION_MONITORED);
                newContract.setDelegationPath(contract.getDelegationPath());
                newContract.setVisibility(contract.getVisibility());

                return newContract;
	}

	@Override
	public Delegation selectAgent(Map<Agent, Double> agents, Delegation contract)
	{
		// if the PA model is called in this way, just pass it to the DT simple
		// model
		return new TrustorDTBasicDM().selectAgent(agents, contract);
	}
}

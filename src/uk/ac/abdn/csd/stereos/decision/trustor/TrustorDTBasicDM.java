package uk.ac.abdn.csd.stereos.decision.trustor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.Experiment;
import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.Delegation;

/**
 * This class represents a basic implementation of a trustor's decision model,
 * not taking into account monitoring or reputational incentive.
 * 
 * @author cburnett
 * 
 */
public class TrustorDTBasicDM extends TrustorDecisionModel {

    @Override
    /**
     * The result of this method will be a delegation, or 'proposal' object to
     * be sent to the selected agent. If the agent decides not to delegate, null will
     * be returned instead
     *
     * At the moment, risk neutral
     *
     * @returns a delegation object, or null if agent decides not to delegate
     */
    public Delegation selectAgent(Map<Agent, Double> agents, Delegation contract) {
        // needs to generate a 'contract'
        // fundamental principle is that the trustor acts in a way to maximise
        // EU
        // so for each agent we calculate this

        Agent bestAgent = null;
        double bestEU = 0.0;
        // Work which of the known agents is the best
        // i.e. choose the agent with the highest probability expectation value

        // the abstainance EU is the same for all agents
        // we assume here that, in the case of abstainance, the agent gets
        // a definite payoff. However, this could easily be probabilistic; if
        // abstain means
        // to do the task by onesself, this could be subject to self-trust.
        // double euAbstain = uAbs;

        double uSucc = contract.getTrustorSuccessPayoff();
        double uFail = contract.getTrustorFailurePayoff();
        double uAbs = contract.getAbstainPayoff();
        double monitoringCost = contract.getMonitoringCost();

        for (Entry<Agent, Double> a : agents.entrySet()) {
            Agent agent = a.getKey();
            double rating = a.getValue();

            // we are assuming this is an unavoidable cost to invoke the agent
            double askingPrice = agent.getAskingPrice();
            double euSuccess = rating * (uSucc - askingPrice);
            double euFailure = 1 - rating * (uFail - askingPrice);
            double agentEU = euSuccess + euFailure;

            // if this agent offers the best EU so far, set it to be the best
            if (new BigDecimal(agentEU, Experiment.mathContext).compareTo(new BigDecimal(bestEU, Experiment.mathContext)) >= 0) {
                bestAgent = agent;
                bestEU = agentEU;
            }
        }

        // if the best available agent is a worse bet than doing it ourselves,
        // return a contract for ourselves
        // by using the <= relation, we're making the trustor a bit risk averse
        // - if the EUs are the same,
        // the trustor will still prefer to do it alone.

        // if no it's better to abstain, delegate to ourselves
        if (new BigDecimal(bestEU, Experiment.mathContext).compareTo(new BigDecimal(uAbs, Experiment.mathContext)) < 0) {
            Delegation newContract = new Delegation(owner, owner, uSucc, uFail, uAbs, uAbs, uAbs, monitoringCost, uAbs, Delegation.DELEGATION_SIMPLE);
            newContract.setDelegationPath(contract.getDelegationPath());
            newContract.setVisibility(contract.getVisibility());
            return newContract;

        }
        // create a simple contract, just containing the selected agent
        Delegation newContract = new Delegation(owner, bestAgent, uSucc, uFail, bestAgent.getAskingPrice(), bestAgent.getAskingPrice(), uAbs, monitoringCost, bestEU,
                Delegation.DELEGATION_SIMPLE);
        newContract.setDelegationPath(contract.getDelegationPath());
        newContract.setVisibility(contract.getVisibility());
        return newContract;
    }

    @Override
    public Delegation selectAgent(Map<Agent, Double> unconditionalOpinions,
            Map<String, Map<Agent, Double>> conditionalOpinions, Delegation contract) {
        // Just pass on to the simple method - DT trust model doesn't use the
        // conditional ratings
        return selectAgent(unconditionalOpinions, contract);
    }
}

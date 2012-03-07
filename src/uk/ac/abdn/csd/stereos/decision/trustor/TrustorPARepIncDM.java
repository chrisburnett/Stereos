package uk.ac.abdn.csd.stereos.decision.trustor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.math.optimization.GoalType;
import org.apache.commons.math.optimization.OptimizationException;
import org.apache.commons.math.optimization.RealPointValuePair;
import org.apache.commons.math.optimization.linear.LinearConstraint;
import org.apache.commons.math.optimization.linear.LinearObjectiveFunction;
import org.apache.commons.math.optimization.linear.Relationship;
import org.apache.commons.math.optimization.linear.SimplexSolver;

import uk.ac.abdn.csd.stereos.Experiment;
import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.Delegation;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;

/**
 * Reputational incentive!!! Woo!
 * 
 * @author cburnett
 * 
 */
public class TrustorPARepIncDM extends TrustorDecisionModel {

    @Override
    /**
     * The result of this method will be a delegation, or 'proposal' object to
     * be sent to the selected agent. If the agent decides not to delegate, null will
     * be returned instead
     *
     * At the moment, risk neutral
     *
     *
     * @returns a delegation object, or null if agent decides not to delegate
     */
    public Delegation selectAgent(Map<Agent, Double> unconditionalOpinions,
            Map<String, Map<Agent, Double>> conditionalOpinions, Delegation del) {

        double uAbs = del.getAbstainPayoff();
        double uSucc = del.getTrustorSuccessPayoff();
        double uFail = del.getTrustorFailurePayoff();
        double monitoringCost = del.getMonitoringCost();

        // best EU of the trustor!
        Agent bestTrustee = null;
        double myBestEU = round(uAbs);
        Map<Integer, Double> bestContract = null;
        double[] bestRi = new double[]{0, 0};

        // just twisting round the hashmap, nothing to worry about here...
        Map<Agent, Map<String, Double>> input = Agent.transposeInput(conditionalOpinions);

        // for each candidate
        for (Entry<Agent, Map<String, Double>> a : input.entrySet()) {
            Agent candidate = a.getKey();
            Map<String, Double> conditionalRatings = a.getValue();
            // set of contracts for the trustee, one for each effort
            Map<String, Map<Integer, Double>> contracts = new HashMap<String, Map<Integer, Double>>();
            // reputational incentives for each effort level
            Map<String, double[]> repincs = new HashMap<String, double[]>();
            double reserve = round(candidate.getAskingPrice());

            // for each effort level calculate the minimum contract and store
            for (Entry<String, Double> rating : a.getValue().entrySet()) {
                String effortId = rating.getKey();
                EffortLevel effort = candidate.getProfile().getEffortLevel(rating.getKey());
                double p = round(rating.getValue());
                double[] eprobs = {p, 1 - p};
                double costy = effort.getCost();

                // need to be concrete on where ri goes =- lets calc it first
                double[] ri = computeRI(candidate, effortId, input);
                // store the reputational incentive for this effort
                repincs.put(effortId, ri);
                // ok, ri calculated - now just need to use it in a thing!
                // it essentially acts as a regular incentive (that's the
                // argument)
                // and is intended to save the trustor money.
                // so positive ri should be subtracted from the positive
                // solution, neg from neg,
                // and explicitly added to the delegation object so the agent
                // knows what's happening

                // set up (linear i.e. risk neutral) optimiser
                LinearObjectiveFunction obj = new LinearObjectiveFunction(eprobs, -costy);
                Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

                for (String eid : candidate.getProfile().getEfforts().keySet()) {
                    if (!eid.equals(effortId)) {
                        double ep = round(conditionalRatings.get(eid));
                        // ignore if equal
                        if (ep != p) {
                            double coste = candidate.getProfile().getEffortLevel(eid).getCost();
                            constraints.add(new LinearConstraint(eprobs, -costy, Relationship.GEQ, new double[]{ep,
                                        1 - ep}, -coste));
                        }
                    }
                }

                // Participation constraint - with reputation incentive
                constraints.add(new LinearConstraint(eprobs, Relationship.GEQ, reserve + costy));
                // create and run the solver
                SimplexSolver solver = new SimplexSolver();
                RealPointValuePair contract;

                try {
                    contract = solver.optimize(obj, constraints, GoalType.MINIMIZE, false);

                    // get the solution
                    // not paying for RI :) - we will explicitly state it in the
                    // contract,
                    // and the trustee will verify for itself
                    double us = round(contract.getPoint()[0] - ri[Delegation.OUTCOME_SUCCESS]);
                    double uf = round(contract.getPoint()[1] - ri[Delegation.OUTCOME_FAILURE]);

                    if (!contracts.containsKey(effortId)) {
                        contracts.put(effortId, new HashMap<Integer, Double>());
                    }
                    contracts.get(effortId).put(Delegation.OUTCOME_FAILURE, uf);
                    contracts.get(effortId).put(Delegation.OUTCOME_SUCCESS, us);

                } catch (OptimizationException e1) {
                    System.err.println("Optimiser failed.");
                    System.err.println(effortId);
                    System.err.println(effort.getCost());
                    System.err.println(e1);
                }
            }

            // now pick the effort level which maximises our EU (for this
            // candidate)
            double myAgentEU = 0;
            String bestEffort = null;
            for (Entry<String, Map<Integer, Double>> contract : contracts.entrySet()) {
                String eid = contract.getKey();
                double p = 0.5; // default prior
                if (conditionalRatings.containsKey(eid)) {
                    p = round(conditionalRatings.get(eid));
                }

                // we have reputational incentive - we are not paying the agent
                // fully, but rather
                // factoring in the reputational effect of the agent's actions
                double eu = round((uSucc - contract.getValue().get(Delegation.OUTCOME_SUCCESS)) * p
                        + (uFail - contract.getValue().get(Delegation.OUTCOME_FAILURE)) * (1 - p));

                if (eu > myAgentEU) {
                    myAgentEU = eu;
                    bestEffort = eid;
                }
            }
            // if this agent offers the best EU so far, set it to be the best
            if (round(myAgentEU) >= myBestEU) {
                bestTrustee = candidate;
                myBestEU = myAgentEU;
                // get contract and reputational incentives associated with the
                // best effort
                bestRi = repincs.get(bestEffort);
                bestContract = contracts.get(bestEffort);
            }
        }
        // if no it's better to abstain, delegate to ourselves
        if (bestContract == null) {

            Delegation newContract = new Delegation(owner, owner, uSucc, uFail, uAbs, uAbs, uAbs, monitoringCost, uAbs, Delegation.DELEGATION_ABSTAIN);
            newContract.setDelegationPath(del.getDelegationPath());
            newContract.setVisibility(del.getVisibility());
            return newContract;
        }

        // return the contract, with reputational incentives explicitly stated
        Delegation newContract = new Delegation(owner, bestTrustee, uSucc, uFail, bestContract.get(Delegation.OUTCOME_SUCCESS), bestContract.get(Delegation.OUTCOME_FAILURE), uAbs, monitoringCost, myBestEU, bestRi[Delegation.OUTCOME_SUCCESS],
                bestRi[Delegation.OUTCOME_FAILURE], Delegation.DELEGATION_REPINC);
        newContract.setDelegationPath(del.getDelegationPath());
        newContract.setVisibility(del.getVisibility());
        return newContract;
    }

    /**
     * THE RI FUNCTION - effort conditional
     *
     * @param unconditionalOpinions
     * @param conditionalOpinions
     * @return
     */
    private double[] computeRI(Agent candidate, String effortId, Map<Agent, Map<String, Double>> conditionalOpinions) {
        Opinion op = tm.opinionQuery(candidate, effortId);
        // need to compute change in reputation
        Opinion drPlusOp = new Opinion(op);
        Opinion drMinusOp = new Opinion(op);
        drPlusOp.setPositives(op.getPositives() + 1);
        drMinusOp.setNegatives(op.getNegatives() + 1);

        // get the probability expectations for delta reputation
        double dr_inc = drPlusOp.getExpectationValue();
        double dr_dec = drMinusOp.getExpectationValue();

        // now need to calculate the change in expected loss
        // expected loss is just the expectation in case of failure, given what
        // the trustor is asking for,
        // expected loss *within a group*

        // leader expected loss
        // NOTE - CHANGED FROM NON-STATIC REFERENCE
        double targetEL = Agent.calculateAverageEL(conditionalOpinions, effortId);
        // find the market level of expected loss -
        // average EL, for competition
        // averageEL = sum/count;

        // now compute inc and dec ri
        double ri_inc = (targetEL / (1 - dr_inc)) - candidate.getAskingPrice();
        double ri_dec = (targetEL / (1 - dr_dec)) - candidate.getAskingPrice();

        double result[] = new double[2];
        result[Delegation.OUTCOME_SUCCESS] = ri_inc;
        result[Delegation.OUTCOME_FAILURE] = ri_dec;
        // System.err.println(averageEL);
        return result;
    }

    @Override
    public Delegation selectAgent(Map<Agent, Double> agents, Delegation contract) {
        // if the PA model is called in this way, just pass it to the DT simple
        // model
        return new TrustorDTBasicDM().selectAgent(agents, contract);
    }

    /**
     * round using bigdecimal
     *
     * @param r
     * @return
     */
    private double round(double r) {
        return new BigDecimal(r, Experiment.mathContext).doubleValue();
    }
}

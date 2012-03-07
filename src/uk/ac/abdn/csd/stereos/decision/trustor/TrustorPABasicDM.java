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

/**
 * This class represents a basic Principal Agent implementation of a trustor's
 * decision model, not taking into account monitoring or reputational incentive.
 * 
 * Could have a 'meta' model which just runs the other models and picks the best
 * contract.
 * 
 * @author cburnett
 * 
 */
public class TrustorPABasicDM extends TrustorDecisionModel
{

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
			Map<String, Map<Agent, Double>> conditionalOpinions, Delegation del)
	{
		// needs to generate a 'contract'
		// fundamental principle is that the trustor acts in a way to maximise
		// EU
		// so for each agent we calculate this

                double uAbs = del.getAbstainPayoff();
                double uSucc = del.getTrustorSuccessPayoff();
                double uFail = del.getTrustorFailurePayoff();
                double monitoringCost = del.getMonitoringCost();

		Agent bestTrustee = null;
		// best EU of the trustor!
		double myBestEU = new BigDecimal(uAbs, Experiment.mathContext).doubleValue();
		Map<Integer, Double> bestContract = null;
		// Work which of the known agents is the best
		// i.e. choose the agent with the highest probability expectation value

		// the abstainance EU is the same for all agents
		// we assume here that, in the case of abstainance, the agent gets
		// a definite payoff. However, this could easily be probabilistic; if
		// abstain means
		// to do the task by onesself, this could be subject to self-trust.
		// double euAbstain = uAbs;

		// if we have a uniform distribution over effort levels, it makes no
		// sense to compare them
		// this is the best assumption we can make under the MLRC, because it
		// allows efforts to be equal.
		// in this case, when all efforts are equal, there's no point in adding
		// the incentive compatibility constraints
		// 

		// just twisting round the hashmap, nothing to worry about here...
		Map<Agent, Map<String, Double>> input = Agent.transposeInput(conditionalOpinions);

		// for each candidate
		for (Entry<Agent, Map<String, Double>> a : input.entrySet()) {
			Agent candidate = a.getKey();
			Map<String, Double> conditionalRatings = a.getValue();
			// System.out.println(conditionalRatings);

			// no peeking - ideally (even as the programmer) we shouldn't be
			// able to see
			// the whole thing
			// Map<String,EffortLevel> availableEfforts =
			// candidate.getEfforts();

			// three step procedure to calculate best EU
			// contract for each effort level
			Map<String, Map<Integer, Double>> contracts = new HashMap<String, Map<Integer, Double>>();

			BigDecimal reserve = new BigDecimal(candidate.getAskingPrice(), Experiment.mathContext);

			// for each effort level calculate the minimum contract and store
			for (Entry<String, Double> rating : a.getValue().entrySet()) {
				String effortId = rating.getKey();
				EffortLevel effort = candidate.getProfile().getEffortLevel(rating.getKey());
				// if(conditionalRatings.get(effortId) != null)
				// p = conditionalRatings.get(effortId);
				double p = new BigDecimal(rating.getValue(), Experiment.mathContext).doubleValue();

				double[] eprobs = { p, 1 - p };

				double costy = effort.getCost();

				// set up (linear i.e. risk neutral) optimiser
				LinearObjectiveFunction obj = new LinearObjectiveFunction(eprobs, -costy);
				Collection<LinearConstraint> constraints = new ArrayList<LinearConstraint>();

				// constraints.add(new LinearConstraint(new double[] { 5,4 }, 4,
				// Relationship.GEQ, new double[] {3,3}, 3));

				// if we have a uniform distribution over effort levels, it
				// makes no sense to compare them
				// this is the best assumption we can make under the MLRC,
				// because it allows efforts to be equal.
				// in this case, when all efforts are equal, there's no point in
				// adding the incentive compatibility constraints
				// incentive compat is actually multiple constraints
				for (String eid : candidate.getProfile().getEfforts().keySet())
					if (!eid.equals(effortId)) {
						double ep = new BigDecimal(conditionalRatings.get(eid), Experiment.mathContext).doubleValue();
						// ignore if equal
						if (ep != p) {
							double coste = candidate.getProfile().getEffortLevel(eid).getCost();
							constraints.add(new LinearConstraint(eprobs, -costy, Relationship.GEQ, new double[] { ep,
									1 - ep }, -coste));
						}
					}

				// Participation constraint
				constraints.add(new LinearConstraint(eprobs, Relationship.GEQ, reserve.doubleValue() + costy));

				// create and run the solver
				SimplexSolver solver = new SimplexSolver();
				RealPointValuePair contract;
				try {
					contract = solver.optimize(obj, constraints, GoalType.MINIMIZE, true);

					// get the solution
					double us = contract.getPoint()[0];
					double uf = contract.getPoint()[1];

					if (!contracts.containsKey(effortId))
						contracts.put(effortId, new HashMap<Integer, Double>());
					contracts.get(effortId).put(Delegation.OUTCOME_FAILURE, uf);
					contracts.get(effortId).put(Delegation.OUTCOME_SUCCESS, us);

					// System.out.println("Contract - us: " + us + "  uf: " + uf
					// + "  umin: " + contract.getValue());
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
				double p = 0.5;
				if (conditionalRatings.containsKey(eid))
					p = conditionalRatings.get(eid);
				double eu = (uSucc - contract.getValue().get(Delegation.OUTCOME_SUCCESS)) * p
						+ (uFail - contract.getValue().get(Delegation.OUTCOME_FAILURE)) * (1 - p);
				// System.out.println(contract + " _"+ eu + " " + uAbs);

				if (new BigDecimal(eu, Experiment.mathContext).compareTo(new BigDecimal(myAgentEU,
						Experiment.mathContext)) > 0) {
					myAgentEU = eu;
					bestEffort = eid;
				}
			}

			// if this agent offers the best EU so far, set it to be the best
			if (new BigDecimal(myAgentEU, Experiment.mathContext).compareTo(new BigDecimal(myBestEU,
					Experiment.mathContext)) >= 0) {
				bestTrustee = candidate;
				myBestEU = myAgentEU;
				bestContract = contracts.get(bestEffort);
			}
		}

		// NOTE: Up till now we have made it possible to have as many effort
		// levels as desired. However due to running out of time,
		// from this point on, (i.e. the delegation object) the assumption is 2
		// effort levels and 2 outcomes

		// if the best available agent is a worse bet than doing it ourselves,
		// return a contract for ourselves
		// by using the <= relation, we're making the trustor a bit risk averse
		// - if the EUs are the same,
		// the trustor will still prefer to do it alone.
		// System.out.println("Delegating..." + myBestEU + "  " + bestContract);

		// if no it's better to abstain, delegate to ourselves
		if (bestContract == null) {
                    Delegation newContract = new Delegation(owner, owner, uSucc, uFail, uAbs, uAbs, uAbs, monitoringCost, uAbs, Delegation.DELEGATION_ABSTAIN);
                    newContract.setVisibility(del.getVisibility());
                    newContract.setDelegationPath(del.getDelegationPath());
                    return newContract;
		}

		// return the contract
		Delegation newContract = new Delegation(owner, bestTrustee, uSucc, uFail, bestContract.get(Delegation.OUTCOME_SUCCESS), bestContract
				.get(Delegation.OUTCOME_FAILURE), uAbs, monitoringCost, myBestEU, Delegation.DELEGATION_UNMONITORED);
                newContract.setDelegationPath(del.getDelegationPath());
                newContract.setVisibility(del.getVisibility());
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

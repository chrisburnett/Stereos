package uk.ac.abdn.csd.stereos.decision.trustee;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.Experiment;
import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.Delegation;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.agents.Profile;

/**
 * This class represents a simple decision model not taking account of RIs
 * 
 * @author cburnett
 * 
 */
public class TrusteeDTBasicDM extends TrusteeDecisionModel
{

	public EffortLevel selectEffort(Delegation del, double reserve, Profile p,
			Map<Agent, Double> unconditionalOpinions, Map<String, Map<Agent, Double>> conditionalOpinions)
	{
		// if we're in the most trusted mode, don't care about incentives
		if(del.getType() == Delegation.DELEGATION_MOST_TRUSTED)
			return p.getEffortLevel(p.getDefaultEffortLevelId());
		
		// without reputational incentives, the agent will either accept the
		// contract or not,
		// depending on the asking price. This is the simplest decision model
		// which doesn't
		// consider monitoring or effort level choice etc.

		// an agent's asking price is at least the cost of choosing its lowest
		// effort level,

		// so the delegation object contains the payoffs the trustor is
		// proposing
		// the trustee also has some idea of its own competence

		// so the trustee just makes a decision on the basis of its own utility
		// best EU is by default that presented by the agent's asking price
		// will refuse if asking price not met

		// NOTE - here's the thing. Since agents will undoubtedly disagree on
		// the probabilty
		// of success, trustees will base their decisions on the trust the
		// society has in them.
		// That means that, even if an agent knows it's brilliant, it can't go
		// refusing contracts
		// from trustors who don't know about it, when it's unknown in the
		// society. An agent can
		// only expect to be compensated in relation with its reputation. So, at
		// the beginning, when a
		// trustee is unknown, he will have to behave as a complete unknown.
		// This still presents problems
		// because the trustee will not know the degrees of ambiguity aversion -
		// but this is fine - as long
		// as some trustors in the society are willing to take risks. If all the
		// agents are ambituity averse,
		// they will initially produce contracts that no trustee will accept,
		// and the society will freeze
		// (as it's doing)

		// get the unconditional trust rating for ourselves in the society
		// double selfImage = unconditionalOpinions.get(getOwner());

		EffortLevel bestEffort = null;
		String bestEffortId = null;

		// the absolute minimum an agent can expect is their asking price minus
		// the cost of doing minimum effort
		// double bestEU = reserve - p.getEffortLevel("e1").getCost();

		BigDecimal bestEU = new BigDecimal(reserve, Experiment.mathContext);

		// We, as trustee, need to check to see if monitored/unmonitored/RI
		
		if (del.getType() == Delegation.DELEGATION_UNMONITORED
				|| del.getType() == Delegation.DELEGATION_SIMPLE || del.getType() == Delegation.DELEGATION_REPINC
				|| del.getType() == Delegation.DELEGATION_MOST_TRUSTED) {
			// note we can safely handle reputational incentive here because, by
			// default, it's set to 0
			// (unmonitored, hence unconditional) payoffs in case of success or
			// failure
			double uSucc = del.getSuccessPayoff();
			double uFail = del.getFailurePayoff();

			// not verified at this point
			double riSucc = del.getRiPos();
			double riFail = del.getRiNeg();

			// we need to find the probabilities of success/failure in each
			// effort level
			// in order to calculate an EU for each effort level
			for (Entry<String, EffortLevel> entry : p.getEfforts().entrySet()) {
				String eid = entry.getKey();
				EffortLevel e = entry.getValue();
				// eu_succ will be p_succ * (u_succ - cost_effort)
				// these are the 'real' probabilities
				// update - not using the real probabilities - we're using our
				// self image
//				double pSucc = e.successProbAtThreshold(del.getPerformanceThreshold());
//				double pFail = e.failureProbAtThreshold(del.getPerformanceThreshold());

				 double pSucc = conditionalOpinions.get(eid).get(getOwner());
				 double pFail = 1 - pSucc;

				// factor in RI - will be 0 if RI not in use
				double euSucc = pSucc * (uSucc + riSucc);
				double euFail = pFail * (uFail + riFail); // should be
															// negative...

				BigDecimal euEffort = new BigDecimal((euSucc + euFail) - e.getCost());


				if (euEffort.compareTo(bestEU) >= 0) {
					bestEU = euEffort;
					bestEffort = e;
				}
			}
		} else if (del.getType() == Delegation.DELEGATION_MONITORED) {
			// what to do if the delegation is monitored
			// we now read the effort conditional contract

			// calculate EU of each effort level and make a choice
			for (Entry<String, EffortLevel> entry : p.getEfforts().entrySet()) {
				String effortId = entry.getKey();
				EffortLevel thisEffort = entry.getValue();

				double eSuccPayoff = del.getFailurePayoff(effortId);
				double eFailPayoff = del.getSuccessPayoff(effortId);

				// use self-trust to decide
				double eSuccProb = conditionalOpinions.get(effortId).get(getOwner());
				double eFailProb = 1 - eSuccProb;

				BigDecimal eEU = new BigDecimal((eFailProb * (eFailPayoff) + eSuccProb * (eSuccPayoff))
						- thisEffort.getCost(), Experiment.mathContext);
				// System.err.println(eEU);

				// need to factor in asking price (cost of agent survival :))
				if (eEU.compareTo(bestEU) >= 0) {
					bestEU = eEU;
					bestEffort = thisEffort;
					bestEffortId = effortId;

				}
			}
		}
		// record the effort level selected in the delegation object
		// bit of a side effect, but at the moment time is short and best
		// practice is taking
		// a back seat for a little bit

		if (del.getType() == Delegation.DELEGATION_MONITORED)
			del.setTrusteeEffort(bestEffortId);
		else
			del.setTrusteeEffort(null);
		return bestEffort;
	}
}

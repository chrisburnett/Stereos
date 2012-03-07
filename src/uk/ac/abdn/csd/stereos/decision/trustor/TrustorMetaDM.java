package uk.ac.abdn.csd.stereos.decision.trustor;

import java.math.BigDecimal;
import java.util.Map;

import uk.ac.abdn.csd.stereos.Experiment;
import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.Delegation;

/**
 * A 'meta' model which runs the other models
 * 
 * @author cburnett
 * 
 */
public class TrustorMetaDM extends TrustorDecisionModel {

    /**
     * Needs to call the other models in turn, and pick the best contract
     */
    @Override
    public Delegation selectAgent(Map<Agent, Double> unconditionalOpinions,
            Map<String, Map<Agent, Double>> conditionalOpinions, Delegation contract) {

//        double uSucc = contract.getTrustorSuccessPayoff();
//        double uFail = contract.getTrustorFailurePayoff();
//        double uAbs = contract.getAbstainPayoff();
//        double monitoringCost = contract.getMonitoringCost();


        Delegation[] alternatives = new Delegation[3];

        // Only PA type mdels here
        // // simple model
//		 TrustorDecisionModel simpleDT = new TrustorDTBasicDM();
//		 simpleDT.setOwner(this.getOwner());
//		 alternatives[4] = simpleDT.selectAgent(unconditionalOpinions, uSucc,
//				 uFail, uAbs, monitoringCost);
        //System.out.println("DT:" + alternatives[0].getTrustorEU());

        // unmonitored-conditional
        TrustorDecisionModel simplePA = new TrustorPABasicDM();
        simplePA.setOwner(this.getOwner());
        simplePA.setTrustModel(tm);
        alternatives[0] = simplePA.selectAgent(unconditionalOpinions, conditionalOpinions, contract);
        // System.out.println("PA:" + alternatives[0].getTrustorEU());

        // monitored-conditional
        TrustorDecisionModel monitoredPA = new TrustorPAMonitoredDM();
        monitoredPA.setOwner(this.getOwner());
        monitoredPA.setTrustModel(tm);
        alternatives[1] = monitoredPA.selectAgent(unconditionalOpinions, conditionalOpinions, contract);
        // if(alternatives[1].getType() == Delegation.DELEGATION_MONITORED)
        // System.out.println("PAM:" + alternatives[1].getTrustorEU());

        // unmonitored-RI
        TrustorDecisionModel riPA = new TrustorPARepIncDM();
        riPA.setTrustModel(tm);
        riPA.setOwner(this.getOwner());
        alternatives[2] = riPA.selectAgent(unconditionalOpinions, conditionalOpinions, contract);
        // calculate all in turn, get best delegations

        // abstainance!! - this prevents agents from getting shafted :)
        //alternatives[3] = new Delegation(owner, owner, uAbs, uAbs, uAbs, Delegation.DELEGATION_ABSTAIN);

        // return the best
        double best = 0;
        Delegation bestDel = null;
        for (Delegation d : alternatives) {
            if (new BigDecimal(d.getTrustorEU(), Experiment.mathContext).compareTo(new BigDecimal(best,
                    Experiment.mathContext)) >= 0) {
                best = d.getTrustorEU();
                bestDel = d;
            }
        }
        // System.err.println(bestDel.getType());
        bestDel.setDelegationPath(contract.getDelegationPath());
        bestDel.setVisibility(contract.getVisibility());
        bestDel.setOutcome(contract.getOutcome());
        return bestDel;

    }

    @Override
    public Delegation selectAgent(Map<Agent, Double> agents, Delegation contract) {
        // TODO Auto-generated method stub
        return null;
    }
}

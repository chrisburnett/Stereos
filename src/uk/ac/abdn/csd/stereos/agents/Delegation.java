package uk.ac.abdn.csd.stereos.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.Experiment;

/**
 * This class represents an incentive structure, and all the necessary flags to
 * represent different outcomes. Note that this assumes (as does the whole
 * system) only one possible task.
 * 
 * @author cburnett
 * 
 */
public class Delegation {

    // the trustor who has proposed the contract
    private Agent trustor;
    // the trustee to whome the contract is proposed
    private Agent trustee;
    // flags for success and failure
    public static final int OUTCOME_SUCCESS = 1;
    public static final int OUTCOME_FAILURE = 0;
    // flags for delegation type stamps
    public static final int DELEGATION_MOST_TRUSTED = 100;
    public static final int DELEGATION_SIMPLE = 101;
    public static final int DELEGATION_UNMONITORED = 102;
    public static final int DELEGATION_ABSTAIN = 110;
    public static final int DELEGATION_MONITORED = 103;
    public static final int DELEGATION_REPINC = 104;
   
    // payoff structures for the TRUSTEE in case of success or failure
    private Map<String, Double> successPayoffs;
    private Map<String, Double> failurePayoffs;
    // payoffs for the TRUSTOR in case of success/failure
    private double trustorSuccessPayoff;


    private double trustorFailurePayoff;
    private double abstainPayoff;

    // reputational incentives
    private double riNeg;
    private double riPos;
    private int type;
    // is delegation monitored?
    private boolean isMonitored;
    private double monitoringCost;


    // is RI proposed?
    private boolean isRi;
    // the expected performance level
    // i.e. trustor considers performance above this as success, and below as
    // fail
    // defaults to 0.5
    private double performanceThreshold = 0.5;
    private String trusteeEffort;
    // The delegation history or path - the intermediary agents, beginning with the
    private List<Agent> delegationPath;
    // the final result of the delegation
    private double outcome;

    // a flag recording the visibility condition of this delegation object
    private int visibility;


    /**
     * The EU the trustor expects to get from the contract This allows
     * alternative delegations to be compared by the trustor. It's not intended
     * to be viewed by the trustee...
     */
    private double trustorEU;

    public double getTrustorEU() {
        return trustorEU;
    }

    public void setTrustorEU(double trustorEU) {
        this.trustorEU = trustorEU;
    }

    /**
     * Initial delegation constructor
     * 
     * Use this constructor when creating the initial delegation, i.e. when none of the details have been decided yet.
     * It is only required to provide the details which are specific and fixed to the trustor, like the trustor's payoffs and costs
     * 
     * @param trustor
     * @param trustorSuccessPayoff
     * @param trustorFailurePayoff
     * @param abstainPayoff
     * @param monitoringCost
     */
    public Delegation(Agent trustor, double trustorSuccessPayoff, double trustorFailurePayoff, double abstainPayoff, double monitoringCost, int visibility)
    {
        this.trustorSuccessPayoff = trustorSuccessPayoff;
        this.trustorFailurePayoff = trustorFailurePayoff;
        this.abstainPayoff = abstainPayoff;
        this.monitoringCost = monitoringCost;
        this.outcome = 0.0;
        // by default, we just record a delegation as having a default path including the trustor.
        this.delegationPath = new ArrayList<Agent>();
        this.visibility = visibility;
    }

    /**
     * Unmonitored delegation constructor
     *
     * @param successPayoff
     * @param failurePayoff
     */
    public Delegation(Agent trustor, Agent trustee, double trustorSuccessPayoff, double trustorFailurePayoff, double successPayoff, double failurePayoff, double abstainPayoff, double monitoringCost, double trustorEU,
            int type) {

        this.trustorSuccessPayoff = trustorSuccessPayoff;
        this.trustorFailurePayoff = trustorFailurePayoff;

        successPayoffs = new HashMap<String, Double>();
        failurePayoffs = new HashMap<String, Double>();

        // * represents all effort levels, since we aren't being monititored
        successPayoffs.put("*", successPayoff);
        failurePayoffs.put("*", failurePayoff);
        this.abstainPayoff = abstainPayoff;
        this.monitoringCost = monitoringCost;
        isRi = false;
        isMonitored = false;
        riNeg = 0;
        riPos = 0;

        this.trustorEU = trustorEU;

        this.trustee = trustee;
        this.trustor = trustor;

        this.trusteeEffort = null;
        this.type = type;

        // by default, we just record a delegation as having a default path including the trustor.
        this.delegationPath = new ArrayList<Agent>();
        this.delegationPath.add(trustor);

        // outcome is by default 0 - if anything happens such that this isn't set, or the delegation
        // fails to complete, the worst (completely unsatisfied) outcome should remain
        this.outcome = 0.0;


    }

    /**
     * Monitored delegation constructor - payoff to trustee is conditional on
     * EFFORT
     *
     * @param sps
     *            success payoff function
     * @param fps
     *            failure payoff function
     */
    public Delegation(Agent trustor, Agent trustee, double trustorSuccessPayoff, double trustorFailurePayoff, Map<String, Double> sps, Map<String, Double> fps, double abstainPayoff, double monitoringCost, double trustorEU,
            int type) {

        this.trustorSuccessPayoff = trustorSuccessPayoff;
        this.trustorFailurePayoff = trustorFailurePayoff;

        successPayoffs = sps;
        failurePayoffs = fps;
        this.abstainPayoff = abstainPayoff;
        isMonitored = true;
        this.monitoringCost = monitoringCost;

        isRi = false;
        riNeg = 0;
        riPos = 0;
        this.type = type;
        this.trusteeEffort = null;
        this.trustorEU = trustorEU;
        this.trustee = trustee;
        this.trustor = trustor;
        // by default, we just record a delegation as having a default path including the trustor.
        this.delegationPath = new ArrayList<Agent>();
        // outcome is by default 0 - if anything happens such that this isn't set, or the delegation
        // fails to complete, the worst (completely unsatisfied) outcome should remain
        this.outcome = 0.0;
    }

    /**
     * Reputational incentive constructor
     *
     * @param successPayoff
     * @param failurePayoff
     * @param riP
     *            positive reputational incentive
     * @param riN
     *            negative reputational incentive
     */
    public Delegation(Agent trustor, Agent trustee, double trustorSuccessPayoff, double trustorFailurePayoff, double successPayoff, double failurePayoff, double abstainPayoff, double monitoringCost, double trustorEU,
            double riP, double riN, int type)
    {

        this.trustorSuccessPayoff = trustorSuccessPayoff;
        this.trustorFailurePayoff = trustorFailurePayoff;

        successPayoffs = new HashMap<String, Double>();
        failurePayoffs = new HashMap<String, Double>();
        successPayoffs.put("*", successPayoff);
        failurePayoffs.put("*", failurePayoff);
        this.abstainPayoff = abstainPayoff;
        riPos = riP;
        riNeg = riN;
        isRi = true;
        isMonitored = false;
        this.monitoringCost = monitoringCost;


        this.trustee = trustee;
        this.trustor = trustor;
        this.trusteeEffort = null;
        this.type = type;
        this.trustorEU = trustorEU;

        // by default, we just record a delegation as having a default path including the trustor.
        this.delegationPath = new ArrayList<Agent>();

        // the final result of the delegation
        this.outcome = 0.0;
    }

    /**
     * Returns the delegation path as constrained by the visibility condition applied to the delegation
     * as visible to the root trustor.
     * @return a list of visible agents in the delegation chain
//     */
//    public List<Agent> getVisibleDelegationPath()
//    {
//        if(visibility == Experiment.VISIBILITY_FULL)
//            return getDelegationPath();
//        if(visibility == Experiment.VISIBILITY_NEXT)
//    }

    public Agent getTrustor() {
        return trustor;
    }

    public void setTrustor(Agent trustor) {
        this.trustor = trustor;
    }

    public Agent getTrustee() {
        return trustee;
    }

    public void setTrustee(Agent trustee) {
        this.trustee = trustee;
    }

    /**
     * Return unmonitored success payoff for this delegation
     */
    public double getSuccessPayoff() {
        return riPos + successPayoffs.get("*");
    }

    /**
     * Return unmonitored failure payoff for this delegation
     */
    public double getFailurePayoff() {
        return riNeg + failurePayoffs.get("*");
    }

    /**
     * Return the conditional payoff in success case
     *
     * @param effort
     * @return
     */
    public double getSuccessPayoff(String effort) {
        return riPos + successPayoffs.get(effort);
    }

    /**
     * Return monitored (conditional) payoff
     *
     * @param effort
     *            id of effort level
     * @return
     */
    public double getFailurePayoff(String effort) {
        return riNeg + failurePayoffs.get(effort);
    }

    public double getRiNeg() {
        return riNeg;
    }

    public void setRiNeg(double riNeg) {
        this.riNeg = riNeg;
    }

    public double getRiPos() {
        return riPos;
    }

    public void setRiPos(double riPos) {
        this.riPos = riPos;
    }

    public boolean isMonitored() {
        return isMonitored;
    }

    public void setMonitored(boolean isMonitored) {
        this.isMonitored = isMonitored;
    }

    public boolean isRi() {
        return isRi;
    }

    public void setRi(boolean ri) {
        this.isRi = ri;
    }

    public double getPerformanceThreshold() {
        return performanceThreshold;
    }

    public Map<String, Double> getSuccessPayoffs() {
        return successPayoffs;
    }

    public void setSuccessPayoffs(Map<String, Double> successPayoffs) {
        this.successPayoffs = successPayoffs;
    }

    public Map<String, Double> getFailurePayoffs() {
        return failurePayoffs;
    }

    public void setFailurePayoffs(Map<String, Double> failurePayoffs) {
        this.failurePayoffs = failurePayoffs;
    }

    /**
     * Returns the effort level selected by the trustee. Is null by default, so
     * this returns null if the trustee has not completed the task yet, or if
     * the effort level was not monitored.
     *
     * @return
     */
    public String getTrusteeEffort() {
        return trusteeEffort;
    }

    public void setTrusteeEffort(String trusteeEffort) {
        this.trusteeEffort = trusteeEffort;
    }

    public void setPerformanceThreshold(double performanceThreshold) {
        this.performanceThreshold = performanceThreshold;
    }

    public int getType() {
        return this.type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public List<Agent> getDelegationPath() {
        return delegationPath;
    }

    public void setDelegationPath(List<Agent> delegationPath) {
        this.delegationPath = delegationPath;
    }


    public double getAbstainPayoff() {
        return abstainPayoff;
    }

    public void setAbstainPayoff(double abstainPayoff) {
        this.abstainPayoff = abstainPayoff;
    }

    public double getMonitoringCost() {
        return monitoringCost;
    }

    public void setMonitoringCost(double monitoringCost) {
        this.monitoringCost = monitoringCost;
    }


    public double getOutcome() {
        return outcome;
    }

    public void setOutcome(double outcome) {
        this.outcome = outcome;
    }

  public double getTrustorFailurePayoff() {
        return trustorFailurePayoff;
    }

    public void setTrustorFailurePayoff(double trustorFailurePayoff) {
        this.trustorFailurePayoff = trustorFailurePayoff;
    }

    public double getTrustorSuccessPayoff() {
        return trustorSuccessPayoff;
    }

    public void setTrustorSuccessPayoff(double trustorSuccessPayoff) {
        this.trustorSuccessPayoff = trustorSuccessPayoff;
    }


    public int getVisibility() {
        return visibility;
    }

    public void setVisibility(int visibility) {
        this.visibility = visibility;
    }


}

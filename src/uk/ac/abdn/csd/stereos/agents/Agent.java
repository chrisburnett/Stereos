package uk.ac.abdn.csd.stereos.agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.Experiment;
import uk.ac.abdn.csd.stereos.agents.evaluators.DefaultEvaluator;
import uk.ac.abdn.csd.stereos.agents.evaluators.PerformanceEvaluator;
import uk.ac.abdn.csd.stereos.decision.trustee.TrusteeDecisionModel;
import uk.ac.abdn.csd.stereos.decision.trustor.TrustorDecisionModel;
import uk.ac.abdn.csd.stereos.reputation.DefaultFilter;
import uk.ac.abdn.csd.stereos.reputation.ReputationFilter;
import uk.ac.abdn.csd.stereos.trust.TrustModel;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;
import uk.ac.abdn.csd.stereos.util.Pair;

/**
 * This class represents an agent participating in the experiment. At any
 * particular time step, an agent can be either a trustee or a trustor, but
 * never both.
 * 
 * @author Chris Burnett
 * 
 */
public class Agent {

    // Constants for the mode of an agent
    public static final int TRUSTEE = 0;
    public static final int TRUSTOR = 1;
    // flag to record whether this agent has never been a trustor/trustee before
    private boolean trustorNewbie;
    private boolean trusteeNewbie;
    /**
     * This variable determines the role this agent will play at this time step
     */
    private int role;
    /**
     * This variable stores a short identifier for the agent
     */
    private String id;
    /**
     * The trust model this agent will use
     */
    protected TrustModel trustModel;
    /**
     * The decision model the agent will use as a trustor
     */
    protected TrustorDecisionModel trustorDecisionModel;
    /**
     * The decision model to be used when acting as a trustee
     */
    protected TrusteeDecisionModel trusteeDecisionModel;
    /**
     * The performance profile of this agent
     */
    private Profile profile;
    /**
     * The profile the agent 'believes' it has
     */
    private Profile believedProfile;
    /**
     * Feature vector of this agent. Features are represented as strings, or
     * symbols. Currently, presence or absence of a feature is indicated by a 1
     * or 0, respectively
     */
    private Map<String, Integer> featureVector;
    /**
     * The performance evaluation function this agent will use to judge the
     * outcomes of delegation decisions as either positive or negative
     */
    private PerformanceEvaluator performanceEvaluator;
    /**
     * This filter this agent will use to filter out biased or deceptive
     * recommenders
     */
    private ReputationFilter filter;
    /**
     * Reference to the experiment this agent is participating in, so the agent
     * can request sets of candidates.
     */
    protected Experiment experiment;
    /**
     * Store the average outcomes of the agent at each time point and maintain
     * the total for the purposes of calculating the average
     */
    private List<Double> avgResult;
    private double totalResult;
    /**
     * Utility of the agent at each time step This simple variable records the
     * agent's utility throughout the experiment whether they be trustee or
     * trustor
     */
    private double utility;
    private List<Double> utilityHistory;
    /**
     * Asking price of the agent for the (default) task
     */
    private double askingPrice;
    /**
     * The last contract type - for analysis purposes
     */
    private int lastContractType;
    private double lastInteractionOutcome;


    /**
     * Create a new agent with given profile and trust model. This constructor
     * will create an agent with a default (threshold) performance evaluation
     * function with a threshold of 0.5.
     *
     * @param id
     *            short id for this agent
     * @param trustModel
     *            trust model used by this agent
     * @param todm
     *            the decision model used
     * @param profile
     *            the performance profile of this agent
     * @param featureVector
     *            the complete feature vector of this agent (including noise
     *            features)
     * @param a
     *            reference to the experiment this agent is part of
     */
    public Agent(String id, TrustModel tm, TrustorDecisionModel todm, TrusteeDecisionModel tedm, Profile profile,
            Map<String, Integer> featureVector, Experiment experiment) {
        super();
        this.id = id;
        this.trustModel = tm;
        this.trustorDecisionModel = todm;
        this.trusteeDecisionModel = tedm;
        this.experiment = experiment;
        this.profile = this.believedProfile = profile;
        this.featureVector = featureVector;
        // If no PE function is specified, create the default evaluator with a
        // threshold of 0.5
        this.performanceEvaluator = new DefaultEvaluator(0.5);
        this.filter = new DefaultFilter();
        this.avgResult = new ArrayList<Double>();
        this.utilityHistory = new ArrayList<Double>();
        this.totalResult = 0;
        this.lastContractType = 0;
        // init utility history to start at 0
        utilityHistory.add(0.0);
        utility = 0;

        // set our self as owner
        todm.setOwner(this);
        tedm.setOwner(this);
        // give the decision model a link back to the trust model
        todm.setTrustModel(tm);

        // tell trust model about the ambiguity aversion of this agent
        tm.setDefaultPrior(this.profile.getDefaultAmbiguityAversion());

        // set our asking price to that specified as default by the profile
        askingPrice = this.profile.getDefaultAskingPrice();
        trustorNewbie = true;
        trusteeNewbie = true;
    }

    public void setTrustModel(TrustModel trustModel) {
        this.trustModel = trustModel;
    }

    /**
     * Create a new agent with the specified parameters, and given performance
     * evaluation function
     *
     * @param id
     *            short id for this agent
     * @param tm
     *            trust model used by this agent
     * @param dm
     *            the decision model used
     * @param pe
     *            performance evaluation function for this agent
     * @param p
     *            profile the performance profile of this agent
     * @param featureVector
     *            the complete feature vector of this agent (including noise
     *            features)
     * @param e
     *            a reference to the experiment this agent is part of
     */
    public Agent(String id, TrustModel tm, TrustorDecisionModel todm, TrusteeDecisionModel tedm,
            PerformanceEvaluator pe, Profile p, Map<String, Integer> featureVector, Experiment e) {
        super();
        this.id = id;
        this.trustModel = tm;
        this.trustorDecisionModel = todm;
        this.trusteeDecisionModel = tedm;
        this.experiment = e;
        this.profile = this.believedProfile = p;
        this.featureVector = featureVector;
        this.performanceEvaluator = pe;
        this.avgResult = new ArrayList<Double>();
        this.utilityHistory = new ArrayList<Double>();
        this.totalResult = 0;
        this.filter = new DefaultFilter();

        // set the default ambiguity aversion
        this.trustModel.setDefaultPrior(p.getDefaultAmbiguityAversion());

        // init utility history to start at 0
        utilityHistory.add(0.0);
        utility = 0;

        // set our self as owner
        todm.setOwner(this);
        tedm.setOwner(this);
        this.lastContractType = 0;

        // give the decision model a link back to the trust model
        todm.setTrustModel(tm);

        // tell trust model about the ambiguity aversion of this agent
        tm.setDefaultPrior(this.profile.getDefaultAmbiguityAversion());

        // set our asking price to that specified as default by the profile
        askingPrice = this.profile.getDefaultAskingPrice();
        trustorNewbie = true;
        trusteeNewbie = true;
    }

    /**
     * The profile this agent 'believes' it belongs to.
     * This is the profile the agent will use when making its own decisions-
     * it can be considered to represent an agents complete confidence in its
     * own degree of self-trustworthiness.
     * 
     * @return the profile the agent believes it has
     */
    public Profile getBelievedProfile() {
        return believedProfile;
    }

    public void setBelievedProfile(Profile believedProfile) {
        this.believedProfile = believedProfile;
    }

    public boolean isTrusteeNewbie() {
        return trusteeNewbie;
    }

    public void setTrusteeNewbie(boolean trusteeNewbie) {
        this.trusteeNewbie = trusteeNewbie;
    }

    public boolean isTrustorNewbie() {
        return trustorNewbie;
    }

    public void setTrustorNewbie(boolean trustorNewbie) {
        this.trustorNewbie = trustorNewbie;
    }

    public int getLastContractType() {
        return lastContractType;
    }

    public void setLastContractType(int lastContractType) {
        this.lastContractType = lastContractType;
    }

    /**
     * Get the role this agent will be enacting
     *
     * @return the role as an integer
     */
    public int getRole() {
        return role;
    }

    /**
     * Set the role this agent will be enacting
     *
     * @param role
     */
    public void setRole(int role) {
        this.role = role;
    }

    /**
     * Get the ID of this agent
     *
     * @return the ID string
     */
    public String getId() {
        return id;
    }

    /**
     * Set the ID of this agent
     *
     * @param id
     *            the ID string to set
     */
    public void setId(String id) {
        this.id = id;
    }

    public Profile getProfile() {
        return profile;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public Map<String, Integer> getFeatures() {
        return featureVector;
    }

    public void setFeatures(Map<String, Integer> features) {
        this.featureVector = features;
    }

    /**
     * Cause this agent to perform its task delegation sequence
     *
     * @param uAbstain
     * @param monitoringCost
     *
     * @param timeStep
     *            the current time step the simulation is at. This is so that
     *            the agent can know for sure what the current time step is.
     *            Although we are currently invoking all delegating agents at
     *            every time step, it may be the case in the future that there
     *            is a probability of delegation at each time step, and so the
     *            agent cannot rely on a simple counter here to know what the
     *            time step is.
     *
     *            Returns a reference to the chosen agents.
     */
    public Delegation delegate(Delegation del, int timeStep) {
        // uSucc and uFail are *global* (trustor) task success/fail utilities
        // set by experiment class at invocation, or passed by another agent in case of sub-delegation
        double myUSucc = del.getTrustorSuccessPayoff();
        double myUFail = del.getTrustorFailurePayoff();
        double uAbstain = del.getAbstainPayoff();
        // other trustor parameters from the delegation object
        // Ask the experiment control for the list of candidates
        List<Agent> candidates = experiment.getDelegationCandidates(this);
        // Ask the experiment control for a list of recommenders
        List<Agent> recommenders = experiment.getReputationCandidates(this);
        // need to remove candidates for consideration who are in the sub-delegation path
        // this prevents loops or backtracking in delegation chains
        candidates.removeAll(del.getDelegationPath());
        // filter recommenders for stereotypical bias - we may contact different
        // recommenders for different agents
        Map<Agent, List<Agent>> filteredRecommenders = filter.filterRecommenders(this, candidates, recommenders);
        // placeholder for chosen partner (self?)
        Agent trustee = null;
        // update asking prices of all trustees - prepare them for delegation
        // (potentially)
//		for (Agent a : candidates)
//			a.compete(timeStep);

        // --- MAIN AGENT TRUST PROCESS ---
        double observation = -1;
        // Use trust model to evaluate them - unconditionally
        Map<Agent, Double> unconditionalOpinions = trustModel.evaluate(candidates, filteredRecommenders, timeStep);
        // then conditionally
        Map<String, Map<Agent, Double>> conditionalOpinions = trustModel.conditionallyEvaluate(candidates,
                filteredRecommenders, timeStep);
        // until we find an agent who will accept
        while (observation == -1) {
            // Use decision model to decide - decision mode produces a
            // 'contract' (or incentive structure, whatever)
            Delegation contract = trustorDecisionModel.selectAgent(unconditionalOpinions, conditionalOpinions, del);
            // add ourselves to the delegationpath
            contract.getDelegationPath().add(this);
            //contract.setDelegationPath(delegationPath);
            // --------------------------------
            if (contract != null) {
                // if agent opted to delegate at all
                double evaluation;
                // get the chosen trustee from the contract
                trustee = contract.getTrustee();
                if (trustee != null) {
                    // invoke - the trustee is made aware of its competitors and
                    // recommender peers
                    // this enables it to calculate its 'self-image' and enables
                    // reputational incentive
                    // each agent in the chain (if there is a chain) has to look at the outcome and realise what it means for it
                    observation = trustee.perform(contract, candidates, filteredRecommenders, timeStep).getOutcome();
                    lastInteractionOutcome = observation;
                    if (observation < 0) {
                        // if no delegation happened, because the trustee
                        // refused, then we abstain
                        incUtility(uAbstain);
                        return null; // return null to the experiment class - no
                        // interaction happened
                    }

                    // -1 means trustee refused
                    evaluation = this.performanceEvaluator.evaluate(trustee, observation);
                    // transfer payment from trustor to trustee
                    // unless self delegation!

                    updateUtilities(this, trustee, evaluation, contract);

                    // get effort, if we monitored
                    String observedEffort = null;
                    if (contract.getType() == Delegation.DELEGATION_MONITORED) {
                        observedEffort = contract.getTrusteeEffort();
                    }
                    Experience thisExperience = new Experience(this, trustee, observedEffort, observation, evaluation,
                            this.performanceEvaluator, timeStep);
                    trustModel.addExperience(thisExperience);
                    updateAvgResult(evaluation, trustModel.getExperienceBase().size());

                    // submit the new rating to the centralised reputation store
                    // record the type of delegation
                    this.lastContractType = contract.getType();
                    trustorNewbie = false;
                    // get the new delegation path, after delegation is complete
                    return contract;
                } else {
                    this.lastContractType = Delegation.DELEGATION_ABSTAIN;
                    this.incUtility(uAbstain);
                    return contract;
                }
            } else {
                // in the case agent opts to abstain, do not delegate but deduct
                // the abstain cost
                // from the agent's utility
                this.incUtility(uAbstain);
                this.lastContractType = Delegation.DELEGATION_ABSTAIN;
                // add the performing agent to the delegation path
                return contract;
            }
        }
        this.lastContractType = Delegation.DELEGATION_ABSTAIN;
        // if we haven't got an agent at this stage, it means they all refused,
        // which means we are abstaining
        // just return the null - CAREFUL
        return null;
        // -----------------------------------------------------
    }

    public double getLastInteractionOutcome() {
        return lastInteractionOutcome;
    }

    public void setLastInteractionOutcome(double lastInteractionOutcome) {
        this.lastInteractionOutcome = lastInteractionOutcome;
    }

    /**
     * Update the utilities of the agents after delegaiton, based on the outcome
     * of the contract
     *
     * @param agent
     * @param trustee2
     * @param contract
     * @param monitoringCost
     * @param monitoringCost2
     * @param uFail
     */
    private void updateUtilities(Agent trustor, Agent trustee, double evaluation, Delegation contract) {

        // final amounts to be incremented/deducted
        double trustorSum = 0;
        double trusteeSum = 0;

        if (contract.getType() == Delegation.DELEGATION_MONITORED) {
            // get the observed effort
            String observedEffort = contract.getTrusteeEffort();
            // have to pay in any case - payment conditional on effort
            // trustee only gets paid for choosing the correct effort - nothing
            // else
            // deduct monitoring cost from trustor
            trustorSum -= contract.getMonitoringCost();
            // and pay the sum to the trustee
            trustorSum -= contract.getSuccessPayoff(observedEffort);
            trusteeSum += contract.getSuccessPayoff(observedEffort);
        } else {
            // not monitored - conditional on outcome
            if (evaluation == 1) {
                // if success, pay the trustee
                trustorSum -= contract.getSuccessPayoff();
                trusteeSum += contract.getSuccessPayoff();
            } else {
                trustorSum -= contract.getFailurePayoff();
                trusteeSum += contract.getFailurePayoff();
            }
        }

        // finally, the trustor gets his bonus from the task (or not)
        // regardless of the contract type
        if (evaluation == 1) {
            // success
            trustorSum += contract.getTrustorSuccessPayoff();
        } else // failure
        {
            trustorSum += contract.getTrustorFailurePayoff();
            // remember in monitoring contracts, have to pay the agent
            // even in case of failure
        }

        // do the increments!!!!
        trustor.incUtility(trustorSum);
        trustee.incUtility(trusteeSum);

    }

    /**
     * Update the agent's average interaction outcome
     *
     * @param outcome
     * @param timeStep
     */
    private void updateAvgResult(double outcome, int expCount) {
        totalResult += outcome;
        avgResult.add(totalResult / expCount);
    }

    /**
     * Return the agent's average utility after a given number of experiences
     * (time)
     *
     * @param interaction
     * @return the average utility by this interaction, -1 if agent does not the
     *         requested interaction
     */
    public double getAvgResult(int interaction) {
        if (interaction >= avgResult.size()) {
            return -1;
        }
        return avgResult.get(interaction);
    }

    public List<Double> getAvgResults() {
        return this.avgResult;
    }

    /**
     * Perform a virtual task on behalf of a delegating agent and return the
     * result
     *
     * @return a double representing the outcome (task performance)
     */
    public double perform(int timeStep) {
        // compete(timeStep);

        // when asked to perform without an incentive structure, we select the
        // default level of effort from the profile
        return profile.getDefaultPerformanceValue();
    }

    /**
     * Perform a virtual task, given a particular incentive structure (contract)
     *
     * @param del
     * @return a double representing the outcome (task performance) or -1
     *         representing refusal. Again, this is bad practice, but time is
     *         short. ideally, I would restore the propose() method which allows
     *         an agent to propose a contract before delegating, but this would
     *         really go hand in hand with more sophisticated negotiation over
     *         delegation, and that's not wah
     */
    public Delegation perform(Delegation del, List<Agent> competitors, Map<Agent, List<Agent>> recommenders, int timeStep) {
        // in deciding whether to accept the delegation, the agent needs to
        // consider its standing in the society
        // it does this by consulting its 'self-trust' model.
        Map<Agent, Double> unconditionalOpinions = trustModel.evaluate(competitors, recommenders, timeStep);
        // then conditionally
        Map<String, Map<Agent, Double>> conditionalOpinions = trustModel.conditionallyEvaluate(competitors,
                recommenders, timeStep);

        // check to see if sub-delegation is allowed
        // NOTE - currently, sub-delegation is not fully implemented.
        if (experiment.isSubDelegationAllowed()) {

            Delegation contract = trustorDecisionModel.selectAgent(unconditionalOpinions, conditionalOpinions, del);
            // assuming we are not included in the opinions
            if (contract.getTrustee() != this) // if we are not the best agent for the job, sub-delegate
            {
                return delegate(del, timeStep);
            }
        }

        // sub-delegating is not allowed, or if it is, we are the 'best' agent
        // in this case, we are actually performing the task, and not subdelegating
        EffortLevel selectedEffort = trusteeDecisionModel.selectEffort(del, askingPrice, profile,
                unconditionalOpinions, conditionalOpinions);
        if (selectedEffort == null) {
            return del;
        }

        // indicate that we have interacted
        if(trusteeNewbie) trusteeNewbie = false;
        // add ourselves as performers to the contract
        del.getDelegationPath().add(this);
        // do the performance
        double rawOutcome = profile.getPerformanceValue(selectedEffort);
        double outcome = 0;

        // normalise outcome to be between 0 and 1
        if(rawOutcome > 1) outcome = 1;
        else if(rawOutcome < 0) outcome = 0;
        else outcome = rawOutcome;

        // update and return the contract
        del.setOutcome(outcome);
        return del;
    }

    /**
     * Get this agent's trust model
     *
     * @return the trust model
     */
    public TrustModel getTrustModel() {
        return trustModel;
    }

    /**
     * Return a rating when asked, for use in reputational models.
     *
     * @param a
     *            an agent
     * @return a calculated rating for that agent
     */
    public double query(Agent a) {
        // get the cached calculated rating from the trust model
        return trustModel.query(a);
    }

    /**
     * Return a tuple of positive and negative evidence when asked.
     *
     * @param a
     *            the target agent being evaluated
     * @return an evidence tuple, or null if this feature is not supported by
     *         the agent's trust model
     */
    public Pair<Double, Double> evidenceQuery(Agent a) {
        return trustModel.evidenceQuery(a);
    }

    /**
     * Return an SL opinion about the specified agent
     *
     * @param a
     *            agent to query for
     * @return an SL opinion, or null if the agent's model is not SL based
     */
    public Opinion opinionQuery(Agent a) {
        return trustModel.opinionQuery(a);
    }

    /**
     * Return a short string describing this agent
     */
    public String toString() {
        // We use A to signify the trustor, and B the trustee.
        // This is consistent with the usual "Alice and Bob" convention for
        // delegation relationships :)
        String roleName = this.role == Agent.TRUSTOR ? "A" : "B";
        return id + ":" + roleName + ":" + profile.getId();
    }

    /**
     * Equals method, which compares agents on the basis of their ID strings
     *
     * @param agent
     *            to compare
     * @return true, if the ID strings match
     */
    public boolean equals(Agent a) {
        if (this.id.equals(a.id)) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * Increment utility at time step by specified amount
     */
    public void incUtility(double amount) {
        // get the last item in the utility history
        utility += amount;
        utilityHistory.add(amount);
    }

    /**
     * Increment utility at time step by specified amount
     */
    public void decUtility(double amount) {
        // get the last item in the utility history
        utility -= amount;
        utilityHistory.add(amount);
    }

    /**
     * Get the utility at a given time step
     *
     * @param timeStep
     * @return
     */
    public List<Double> getUtilityHistory() {
        return utilityHistory;
    }

    /**
     * Return a nice string representing the feature vector of this agent
     *
     * @return
     */
    public String getFeatureVectorString() {
        StringBuffer out = new StringBuffer();
        out.append(getId() + ": [");

        // we need a sorted vector
        Map<String, Integer> sv = new TreeMap<String, Integer>(featureVector);

        for (Entry<String, Integer> e : sv.entrySet()) {
            out.append(e.getKey() + "=");
            if (e.getValue() == null) {
                out.append("_");
            } else {
                out.append(e.getValue());
            }
            out.append(",");
        }
        return out.toString();
    }

    public PerformanceEvaluator getPerformanceEvaluator() {
        return performanceEvaluator;
    }

    public void setPerformanceEvaluator(PerformanceEvaluator performanceEvaluator) {
        this.performanceEvaluator = performanceEvaluator;
    }

    public ReputationFilter getFilter() {
        return filter;
    }

    public void setFilter(ReputationFilter rFilter) {
        this.filter = rFilter;
    }

    /**
     * @param askingPrice
     *            the askingPrice to set
     */
    public void setAskingPrice(double askingPrice) {
        this.askingPrice = askingPrice;
    }

    /**
     * @return the askingPrice
     */
    public double getAskingPrice() {
        return askingPrice;
    }

    public Map<String, EffortLevel> getEfforts() {
        return this.profile.getEfforts();
    }



    /**
     * This method is causes the agent to check its standing in the society and
     * update its asking price so that it exposes the same EL as better agents,
     * giving it a chance of being selected
     */
    public void compete(int timeStep) {
        // best asking price is based on the lowest level of effort

        // our competitors
        // Ask the experiment control for the list of competitors
        List<Agent> competitors = new ArrayList<Agent>(experiment.getDelegationCandidates(this));
        competitors.add(this); // add ourselves, so we get our 'standing'
        // Ask the experiment control for a list of recommenders
        List<Agent> recommenders = experiment.getReputationCandidates(this);
        // evaluate society
        Map<Agent, List<Agent>> filteredRecommenders = new DefaultFilter().filterRecommenders(this, competitors,
                recommenders);

        // we need to get our standing
        // Map<Agent,Map<String,Double>> conditionalOpinions =
        // transposeInput(trustModel.conditionallyEvaluate(competitors,
        // filteredRecommenders, timeStep));
        Map<Agent, Double> unconditionals = trustModel.evaluate(competitors, filteredRecommenders, timeStep);
        // calculate average (target) EL - minimum effort
        double targetEL = calculateAverageEL(unconditionals);

        // calculate our own reputation (for failure)
        double myP = 1 - unconditionals.get(this);
        // double myEL = myP * askingPrice;

        // System.err.print("Oldask: " + askingPrice + " ");

        // System.err.println("EL: " + myP * askingPrice + " ask: "+askingPrice
        // + "  P: " + myP);


        // what should our asking price be to meed the target?
        askingPrice = targetEL / myP;

        // System.err.println("Newask: " + askingPrice);

    }

    /**
     * Calculate the average unconditional expected loss within the society
     * @param unconditionals Unconditional opinions 
     * @return
     */
    private double calculateAverageEL(Map<Agent, Double> unconditionals) {
        // average expected loss
        double sum = 0, count = 0;
        // find the market level of expected loss - calculate the average

        for (Entry<Agent, Double> entry : unconditionals.entrySet()) {
            double thisP = 1 - entry.getValue();
            Agent thisAgent = entry.getKey();
            double thisCost = thisAgent.getAskingPrice();

            double thisEL = thisP * thisCost;

            sum += thisEL;
            count++;
        }
        return sum / count;
    }


    public static Map<Agent, Map<String, Double>> transposeInput(Map<String, Map<Agent, Double>> input) {
        Map<Agent, Map<String, Double>> output = new HashMap<Agent, Map<String, Double>>();
        for (Entry<String, Map<Agent, Double>> a : input.entrySet()) {
            for (Entry<Agent, Double> ad : a.getValue().entrySet()) {
                if (output.get(ad.getKey()) == null) {
                    output.put(ad.getKey(), new HashMap<String, Double>());
                }
                output.get(ad.getKey()).put(a.getKey(), ad.getValue());
            }
        }
        return output;
    }

    /**
     * Calculate the average expected loss in the society, for a given effort level
     * @param conditionalOpinions
     * @param effortId
     * @return
     */
    public static double calculateAverageEL(Map<Agent, Map<String, Double>> conditionalOpinions, String effortId) {
        // average expected loss
        double sum = 0, count = 0;
        // find the market level of expected loss - calculate the average
        for (Entry<Agent, Map<String, Double>> entry : conditionalOpinions.entrySet()) {
            double thisP = 1 - entry.getValue().get(effortId);
            Agent thisAgent = entry.getKey();
            double thisCost = thisAgent.getAskingPrice();

            double thisEL = thisP * thisCost;

            sum += thisEL;
            count++;
        }
        return sum / count;
    }

    /**
     * Calculate the best expected loss this agent can achieve, for a particular effort level, given its neighbourhood
     * @param conditionalOpinions
     * @param effortId
     * @return
     */
    public double calculateBestEL(Map<Agent, Map<String, Double>> conditionalOpinions, String effortId) {
        // start with arbitrary agent
        Agent first = conditionalOpinions.keySet().iterator().next();
        double bestEL = first.askingPrice * (1 - conditionalOpinions.get(first).get(effortId));

        for (Entry<Agent, Map<String, Double>> entry : conditionalOpinions.entrySet()) {

            double thisP = 1 - entry.getValue().get(effortId);
            Agent thisAgent = entry.getKey();
            double thisCost = thisAgent.getAskingPrice();

            double thisEL = thisP * thisCost;

            if (thisEL < bestEL) {
                bestEL = thisEL;
            }

        }

        return bestEL;
    }

    /**
     * Clear this agent's 'memory' and set its newbie flag
     */
    public void forget() {
        this.trustorNewbie = true;
        this.trustModel.forget();
    }
}

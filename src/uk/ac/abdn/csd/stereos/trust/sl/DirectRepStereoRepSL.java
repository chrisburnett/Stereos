package uk.ac.abdn.csd.stereos.trust.sl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.exceptions.InvalidParametersException;
import uk.ac.abdn.csd.stereos.trust.TrustModel;
import uk.ac.abdn.csd.stereos.util.Pair;
import uk.ac.abdn.csd.stereos.util.Utilities;

/**
 * This class implements a trust model using direct, reputational and
 * stereotypical components, and using subjective logic as a computational
 * mechanism.
 * 
 * It additionally implements base-rate queries - if a reputational search
 * returns no direct evidence about an agent, this model will attempt to find
 * out if any other agent can produce stereotypical probability expectations. If
 * they can, it will take the mean and use it as a stereotypical bias.
 * 
 * @author Chris Burnett
 * 
 */
public class DirectRepStereoRepSL extends DirectRepStereoSL {

    /**
     * Default constructor.
     *
     * @param temperature
     * @param halfLife
     * @param learningInterval
     * @param clusters
     * @throws InvalidParametersException
     */
    public DirectRepStereoRepSL(double temperature, int halfLife, int learningInterval, int clusters)
            throws InvalidParametersException {
        super(temperature, halfLife, learningInterval, clusters);
    }

    @Override
    public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, int time) {
        Map<Agent, Double> results = new HashMap<Agent, Double>();
        // update the base rate cache as required
        updateBaseRates(agents);

        // Work which of the known agents is the best
        // i.e. choose the agent with the highest probability expectation value
        for (Agent a : agents) {
            // calculate a reputation value for this agent by quering available
            // recommendation providers for their combined evidence tuples
            Pair<Double, Double> repEvidence = Utilities.aggregateReputation(recommenders.get(a), a, time);
            double totalPositives = repEvidence.a;
            double totalNegatives = repEvidence.b;

            // if we have direct evidence about this agent include it
            if (evidence.containsKey(a)) {
                // get the rating, taking into account reputation
                Pair<Double, Double> dirEvidence = evidence.get(a);
                // add it to our total evidence tuple
                totalPositives += dirEvidence.a;
                totalNegatives += dirEvidence.b;
            }

            // if we have an opinion already, get it
            // bring together direct and reputational evidence
            Opinion op;
            if (opinions.containsKey(a)) {
                op = opinions.get(a);
                op.setPositives(totalPositives);
                op.setNegatives(totalNegatives);
            } else {
                op = new Opinion(totalPositives, totalNegatives);
                opinions.put(a, op);
            }

            // set the base rate from the stereotype, if we have one
            if (stereotypeRatingCache.containsKey(a) && !stereotypeRatingCache.get(a).equals(0.5)) {
                op.setBaseRate(stereotypeRatingCache.get(a));
            } else {

                List<TrustModel> models = new ArrayList<TrustModel>();
                for (Agent r : recommenders.get(a)) {
                    models.add(r.getTrustModel());
                }
                models.add(this);

                double av = Utilities.RMSEBias(models, a);
                op.setBaseRate(av);
            }
            // calculate a reputation rating from probability expectation
            double rating = op.getExpectationValue();
            ratings.put(a, rating);

            // add this rating to the result set
            results.put(a, rating);

            // update the mean rating
            meanRating = Utilities.calculatePopulationMeanPerformance(ratings);
        }
        return results;
    }

    /**
     * Override the opinion query to cause the agent to stereotype the query
     * trustee when asked.
     */
    @Override
    public double stereotypeQuery(Agent a) {
        // if we have a base rate calculated, return it
        if (stereotypeRatingCache.containsKey(a)) {
            return stereotypeRatingCache.get(a);
        }

        // if not, then we should try to produce one
        // but not if the model isn't ready - if it's not, return a 'shrug' ;)
        if (!learner.isReady()) {
            return 0.5;
        }

        // stereotype this agent
        List<Agent> l = new ArrayList<Agent>();
        l.add(a);
        this.updateBaseRates(l);
        // return the produced stereotype value
        return stereotypeRatingCache.get(a);
    }
}

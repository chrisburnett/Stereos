package uk.ac.abdn.csd.stereos.util;

import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.trust.TrustModel;

/**
 * This class houses static methods that are useful accross the project.
 * 
 * @author Chris Burnett
 * 
 */
public class Utilities
{

	/**
	 * Calculate the mean agent performance value
	 * 
	 * @return the mean agent performance value
	 */
	public static double calculatePopulationMeanPerformance(Map<Agent, Double> lookup)
	{
		double count = 0, sum = 0;
		for (Double d : lookup.values()) {
			sum += d.doubleValue();
			count++;
		}
		double mean = sum / count;
		return mean;
	}

	/**
	 * When calculating reputation in a probabilistic model, if we assume all
	 * agents are reliable, then we can just incorporate their experiences as
	 * evidence and re-calculate the probability rather than taking the mean or
	 * average or whatever. Of course, this is a big assumption and needs to be
	 * noted.
	 * 
	 * @param recommenders
	 *            - set of recommenders
	 * @param a
	 *            - target agent
	 * @param time
	 *            - timepoint of recommendation
	 * @return a recommendation
	 */
	public static Pair<Double, Double> aggregateReputation(List<Agent> recommenders, Agent a, int time)
	{

		// totals for this target agent - initialise to zero
		double repPositives = 0, repNegatives = 0;
		// query all our recommenders and accumulate the evidence
		for (Agent r : recommenders) {
			// don't ask the target for its own opinion :)
			if (!r.equals(a)) {
				// ask r for evidence about a- NOT an opinion query
				Pair<Double, Double> rEvidence = r.evidenceQuery(a);
				// add this to our accumulated total
				repPositives += rEvidence.a;
				repNegatives += rEvidence.b;
			}
		}
		return new Pair<Double, Double>(repPositives, repNegatives);
	}

	/**
	 * Calculate the average stereotypical bias held about this trustee by the
	 * given recommenders - NOTE - doesn't take into account model confidence!!
	 * 
	 * @param recommenders
	 * @param trustee
	 * @return the average bias
	 */
	public static double averageBias(List<Agent> recommenders, Agent trustee)
	{
		double total = 0, count = 0;

		for (Agent r : recommenders) {
			// get the recommender's opinion
			double s = r.getTrustModel().stereotypeQuery(trustee);
			// don't consider blank opinions
			if (!(s == 0.5)) {
				total += s;
				count++;
			}
		}

		// average
		if (count > 0)
			return total / count;
		else
			return 0.5; // shrug
	}

	/**
	 * Calculate the RMSE bias for this agent (as given in the paper)
	 * 
	 * @param recommenders
	 * @param trustee
	 * @return
	 */
	public static double RMSEBias(List<TrustModel> models, Agent trustee)
	{
		double total = 0, count = 0;

		for (TrustModel t : models) {
			// get the recommender's stereotypical opinion - weighted by
			// confidence
			double s = t.stereotypeQuery(trustee);
			double c = t.confidenceQuery();
			// don't consider blank opinions - i.e. 0.5 even after weighting
			if (!(s == 0.5)) {
				total += s * c;
				count += c;
			}
		}

		// weighted average
		if (count > 0)
			return total / count;
		else
			return 0.5; // shrug
	}
}

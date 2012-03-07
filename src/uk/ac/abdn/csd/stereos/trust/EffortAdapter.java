package uk.ac.abdn.csd.stereos.trust;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.agents.EffortLevel;
import uk.ac.abdn.csd.stereos.agents.Experience;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;
import uk.ac.abdn.csd.stereos.util.Pair;

/**
 * This class adapts a given type of trust model for use modelling multiple
 * efforts
 * 
 * As such it presents many of the usual methods, but calling them without
 * specifying an effort will result in the unconditional model being called
 * 
 * @author cburnett
 * 
 */
public class EffortAdapter extends TrustModel
{

	Map<String, TrustModel> effortModels;
	TrustModel unconditionalModel;

	public EffortAdapter(Map<String, TrustModel> effortModels, TrustModel unconditionalModel)
	{
		// for each effort, create a new model and add it to the registry
		// in this way, we can represent conditional trust a bit more easily

		// the unconditional model is used any time the usual trust model
		// (unconditional) methods are called
		this.effortModels = effortModels;
		this.unconditionalModel = unconditionalModel;
	}

	/**
	 * Return all experiences
	 */
	@Override
	public List<Experience> getExperienceBase()
	{
		return unconditionalModel.getExperienceBase();
	}

	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> filteredRecommenders, int time)
	{
		// Just pass the call on to the unconditional model
		return unconditionalModel.evaluate(agents, filteredRecommenders, time);
	}

	@Override
	public Map<Agent, Double> evaluate(List<Agent> agents, Map<Agent, List<Agent>> recommenders, EffortLevel effort,
			int time)
	{
		// Select the appropriate model, and pass the call on
		return effortModels.get(effort).evaluate(agents, recommenders, time);
	}

	@Override
	public void addExperience(Experience experience)
	{
		// Pass all experiences to conditional model
		unconditionalModel.addExperience(experience);
		// then pass to specific model if effort specified
		if (experience.getEffort() != null)
			effortModels.get(experience.getEffort()).addExperience(experience);
	}

	@Override
	public Map<String, Map<Agent, Double>> conditionallyEvaluate(List<Agent> candidates,
			Map<Agent, List<Agent>> filteredRecommenders, int timeStep)
	{
		// get the list of effort levels by asking an agent (assumes efforts are
		// all the same but this is fine
		Map<String, EffortLevel> efforts = candidates.listIterator().next().getEfforts();

		Map<String, Map<Agent, Double>> results = new HashMap<String, Map<Agent, Double>>();

		for (Entry<String, EffortLevel> e : efforts.entrySet()) {
			// System.out.println(e.equals(effortModels.keySet().iterator().next()));
			results.put(e.getKey(), effortModels.get(e.getKey()).evaluate(candidates, filteredRecommenders, timeStep));

		}

		return results;
	}

	@Override
	public Pair<Double, Double> evidenceQuery(Agent a)
	{
		return unconditionalModel.evidenceQuery(a);
	}

	@Override
	public Opinion opinionQuery(Agent a)
	{
		return unconditionalModel.opinionQuery(a);
	}

	@Override
	public double query(Agent a)
	{
		return unconditionalModel.query(a);
	}

	@Override
	public double stereotypeQuery(Agent trustee)
	{
		return unconditionalModel.stereotypeQuery(trustee);
	}

	public Pair<Double, Double> evidenceQuery(Agent a, String e)
	{
		return effortModels.get(e).evidenceQuery(a);
	}

	public Opinion opinionQuery(Agent a, String e)
	{
		return effortModels.get(e).opinionQuery(a);
	}

	public double query(Agent a, String e)
	{
		return effortModels.get(e).query(a);
	}

	public double stereotypeQuery(Agent trustee, String e)
	{
		return effortModels.get(e).stereotypeQuery(trustee);
	}

	@Override
	public void setDefaultPrior(double defaultPrior)
	{
		// this method will set the default prior to all effort levels
		// this still obeys the MLRC condition
		for (TrustModel tm : effortModels.values())
			tm.setDefaultPrior(defaultPrior);
		unconditionalModel.setDefaultPrior(defaultPrior);
	}

	@Override
	public void forget()
	{
		for(TrustModel tm : effortModels.values())
			tm.forget();
	}

        public TrustModel getUnconditionalTrustModel()
        {
            return this.unconditionalModel;
        }

}

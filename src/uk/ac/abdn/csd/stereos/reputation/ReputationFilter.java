package uk.ac.abdn.csd.stereos.reputation;

import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.agents.Agent;

/**
 * This interface defines the behaviour a reputation filter should have. A
 * reputation filter is a class that takes a number of recommenders and filters
 * out some of them according to some criteria.
 * 
 * @author cburnett
 * 
 */
public interface ReputationFilter
{

	public Map<Agent, List<Agent>> filterRecommenders(Agent self, List<Agent> candidates, List<Agent> recs);

	public void visualise();

}

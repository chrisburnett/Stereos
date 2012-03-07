package uk.ac.abdn.csd.stereos.learning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;
import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 * This class represents a learning mechanism comprising a clustering and
 * classification phase.
 * 
 * @author Chris Burnett
 * 
 */
public class TwoStageLearner implements Learner
{

	private Clusterer clusterer;
	private StereoClassifier classifier;

	boolean isReady;

	/**
	 * Create a new two stage learner.
	 * 
	 * @param noOfClusters
	 *            number of clusters to create at the clustering phase.
	 * @param km
	 *            use true if KMeans clustering (with centroids) is desired.
	 *            Otherwise EM algorithm is used for the clustering component.
	 */
	public TwoStageLearner(int noOfClusters, boolean km)
	{
		isReady = false;
		if (!km)
			clusterer = new SLClusterer(noOfClusters);
		else
			clusterer = new KMClusterer(noOfClusters);
		classifier = new StereoClassifier(noOfClusters);
	}

	/**
	 * (Re)build the learner.
	 */
	public void train(Map<Agent, Opinion> opinions)
	{
		Map<Agent, Opinion> examples = new HashMap<Agent, Opinion>();
		for (Entry<Agent, Opinion> e : opinions.entrySet())
			if (e.getValue().getUncertainty() != 1.0)
				examples.put(e.getKey(), e.getValue());

		clusterer.addOpinions(examples);
		clusterer.createClusters();
		// rebuild classifier
		// classifier.setNoOfClasses(clusterer.getNumClusters());

		// if clusterer found clusters, classify. if not, mark model as still
		// not ready
		if (clusterer.isReady()) {
			classifier.addLabelledAgents(clusterer.getLabelledAgents(), clusterer.getNumClusters());
			classifier.createClassifier();
			isReady = true;
		}
	}

	/**
	 * Classify the given list of agents, and return their biases.
	 */
	public Map<Agent, Double> getBaseRates(List<Agent> agents)
	{
		// now classify the uncached agents
		Map<Agent, Integer> classifiedTrustees = new HashMap<Agent, Integer>();
		try {
			classifiedTrustees = classifier.classifyAgents(agents);
		} catch (Exception e) {
			System.out.println("TwoStageLearner: Exception occurred while classifying");
			e.printStackTrace();
		}
		return clusterer.getClassPEValues(classifiedTrustees);
	}

	/**
	 * Get the cluster (class) the given agent would be in, based on its
	 * features
	 * 
	 * @param a
	 * @return a cluster id
	 */
	public int getClusterForAgent(Agent a)
	{
		List<Agent> alist = new ArrayList<Agent>();
		alist.add(a);
		int clusterId = 0;
		try {
			Map<Agent, Integer> result = classifier.classifyAgents(alist);
			clusterId = result.get(a);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return clusterId;
	}

	public boolean isReady()
	{
		return isReady;
	}

	public Clusterer getClusterer()
	{
		return this.clusterer;
	}

	public Classifier getClassifier()
	{
		return this.classifier.classifier;
	}

	public double getErrorRate()
	{
		// TODO Auto-generated method stub
		return 0;
	}

	/**
	 * Not supported for missing features.
	 * 
	 * @returns null
	 */
	public Set<String> getModelSignature()
	{
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Not supported for missing features.
	 * 
	 * @returns null
	 */
	public void train(Map<Agent, Opinion> opinions, Set<String> hiddenFeatures)
	{
		// TODO Auto-generated method stub

	}

	public Instances getData()
	{
		// TODO Auto-generated method stub
		return null;
	}

}

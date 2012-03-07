package uk.ac.abdn.csd.stereos.learning;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Enumeration;

import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances; //import weka.clusterers.ClusterEvaluation;
import weka.clusterers.ClusterEvaluation;
import weka.clusterers.SimpleKMeans; //import weka.clusterers.SimpleKMeans;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;

/**
 * A clusterer for grouping together opinions in the SL opinion space (b,d,u)
 * Currently uses k-means algorithm to produce 'bins' (clusters) as a
 * discretization of the SL opinion space.
 * 
 * Update June '10 - now uses EM algorithm.
 * 
 * @author Chris Burnett
 * 
 */
public class KMClusterer implements Clusterer
{

	// create the cluster objects we will maintain and use
	// objects that make up our Weka data structures
	private FastVector attributes;
	protected Instances data;

	private SimpleKMeans clusterer;
	// EM clusterer;
	// private ClusterEvaluation eval = new ClusterEvaluation();

	// this structure maintains the assignment or labelling of agents
	// to clusters, once the algorithm has run.
	protected Map<Agent, Integer> agentLabels;

	// an array maintaining which instance ID an opinion about an agent
	// has been assigned to, so that later on we can ask which cluster
	// an *agent* is in.
	protected Agent[] instancesToAgents;

	// track to see if the clusterer is actually setup
	// this helps clients who are trying to get base rates out of it
	protected boolean isReady;

	// an array storing the calculated base rates for the clusters
	double[] baseRateCache;

	// the instances which have been determined to be centroids
	protected Instances centroids;

	/**
	 * Construct a new clusterer on the given opinion space.
	 * 
	 * @param opinions
	 */
	public KMClusterer(int noOfClusters)
	{
		isReady = false;
		agentLabels = new HashMap<Agent, Integer>();
		// create the cluster objects we will maintain and use
		String[] options = new String[3];
		options[0] = "-N"; // no. of clusters
		options[1] = Integer.toString(noOfClusters);
		// preserve the order of instances so we can map them back to agents
		options[2] = "-O";
		// create the clustereKr
		clusterer = new SimpleKMeans();
		try {
			clusterer.setOptions(options); // set the options
		} catch (Exception e) {
			System.err.println("SLClusterer: Option not supported:");
			e.printStackTrace();
		}
	}

	/**
	 * Convenience method to allow the opinion base of a trust model to be
	 * easily passed in. The clusterer doesn't use any details about the agents
	 * to form the clusters so it's not necessary to maintain agent details.
	 * 
	 * @param opinions
	 *            a structure mapping agents to opinions held about them
	 */
	public void addOpinions(Map<Agent, Opinion> opinions)
	{
		// we're updating - we're not ready
		isReady = false;
		instancesToAgents = new Agent[opinions.size()];
		// start setting up the Weka stuff
		attributes = new FastVector();
		// the subject (trustee) of an opinion
		// NOTE: it may not be necessary to include the ID of the agents.
		// leaving it out means we don't have to use a filter on the dataset
		// attributes.addElement(new Attribute("subject", (FastVector) null));
		// three numerical attributes for b,d and u
		attributes.addElement(new Attribute("b"));
		// attributes.addElement(new Attribute("d"));
		attributes.addElement(new Attribute("u"));

		// setup the data object
		data = new Instances("Opinions", attributes, 0);
		int instance = 0;
		// populate it...
		for (Entry<Agent, Opinion> e : opinions.entrySet()) {
			Opinion op = e.getValue();
			Agent ag = e.getKey();
			// record what instance this agent is to the clusterer
			instancesToAgents[instance++] = ag;
			// get the attributes
			double b = op.getBelief();
			// double d = op.getDisbelief();
			double u = op.getUncertainty();
			// now, create an instance
			double[] vals = new double[data.numAttributes()];
			vals[0] = b;
			// vals[1] = d;
			vals[1] = u;
			// and add it to the dataset
			data.add(new Instance(1.0, vals));
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.abdn.csd.stereos.learning.Clusterer#createClusters()
	 */
	public void createClusters()
	{
		agentLabels = new HashMap<Agent, Integer>();
		// we have the data in a form weka can use, so build the clusters
		try {
			// train the clusterer
			clusterer.buildClusterer(data);
			// System.err.println(clusterer);
			// set up the evaluation object
			ClusterEvaluation eval = new ClusterEvaluation();
			eval.setClusterer(clusterer);
			eval.evaluateClusterer(data);

			if (clusterer.numberOfClusters() > 1)
				// switch the flag to indicate the model is ready for use
				isReady = true;
			else
				isReady = false;

			// now label the data we were given, to pass to the classification
			// algorithm
			int[] assignments = new int[data.numInstances()];
			for (int k = 0; k < instancesToAgents.length; k++)
				assignments[k] = clusterer.clusterInstance(data.instance(k));

			// loop through and map the assignments back to agents
			for (int i = 0; i < assignments.length; i++)
				agentLabels.put(instancesToAgents[i], assignments[i]);

			// System.out.println(agentLabels);
			// store the centroids
			centroids = clusterer.getClusterCentroids();
			// for each one, produce a base rate and cache it
			baseRateCache = new double[centroids.numInstances()];
			for (int i = 0; i < baseRateCache.length; i++) {
				// calculate..
				double bri = baseRateFromCentroid(centroids.instance(i));
				// then cache the value
				this.baseRateCache[i] = bri;
			}

		} catch (Exception e) {
			System.err.println("SLClusterer: Exception ocurred while building clusterer:");
			e.printStackTrace();
		}
	}

	/**
	 * Calculate a base rate value from cluster centroid coordinates
	 * 
	 * @param centroid
	 *            Instance representing the centroid of a cluster
	 * @return a base rate value for this cluster
	 */
	public double baseRateFromCentroid(Instance centroid)
	{
		double b = centroid.value(0);
		// double d = centroid.value(1);
		double u = centroid.value(1);

		double d = 1 - b - u;

		Opinion o = new Opinion(b, d, u);
		// project this onto the probability axis.
		// note - this will take account any existing value of a
		// does this mean stereotypes can be strengthened?
		return o.getExpectationValue();
	}

	/**
	 * Bring up a visualisation GUI for this clusterer. (Imported from Weka
	 * documentation)
	 */
	public void visualise()
	{
		// try {
		// PlotData2D plot = ClustererPanel.setUpVisualizableInstances(data,
		// eval);
		// String name = (new SimpleDateFormat("HH:mm:ss - ")).format(new
		// Date());
		// //String cname = "SimpleKMeans";
		//		    
		// VisualizePanel vp = new VisualizePanel();
		// vp.setName(name + " (" + data.relationName() + ")");
		// plot.setPlotName(name + " (" + data.relationName() + ")");
		// vp.addPlot(plot);
		//		    
		// String plotName = vp.getName();
		// final javax.swing.JFrame jf =
		// new javax.swing.JFrame("Weka Clusterer Visualize: " + plotName);
		// jf.setSize(500,400);
		// jf.getContentPane().setLayout(new BorderLayout());
		// jf.getContentPane().add(vp, BorderLayout.CENTER);
		// jf.addWindowListener(new java.awt.event.WindowAdapter() {
		// public void windowClosing(java.awt.event.WindowEvent e) {
		// jf.dispose();
		// }
		// });
		// jf.setVisible(true);
		//		    
		// } catch (Exception e) {
		// e.printStackTrace();
		// }
	}

	/**
	 * Return a string describing the clusterer status
	 * 
	 * @return
	 */
	public String getClustererStatus()
	{
		return clusterer.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.abdn.csd.stereos.learning.Clusterer#getAgentLabels()
	 */
	public Map<Agent, Integer> getLabelledAgents()
	{
		return agentLabels;
	}

	@SuppressWarnings("unchecked")
	public String printCentroids()
	{
		StringBuffer result = new StringBuffer();
		// Instances centroids = clusterer.getClusterCentroids();
		for (Enumeration<Instance> e = centroids.enumerateInstances(); e.hasMoreElements();)
			result.append(e.nextElement() + "\n");
		return result.toString();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.abdn.csd.stereos.learning.Clusterer#getCentroidForCluster(int)
	 */
	// public Instance getCentroidForCluster(int clusterId)
	// {
	// return centroids.instance(clusterId);
	// }

	/**
	 * Return a mapping of agents to cluster-based PE values, given a list of
	 * agents and the cluster to which they belong (or to which they are
	 * predicted to belong) to.
	 * 
	 * @param agents
	 * @return a map mapping agents to base-rate PE values derived from clusters
	 */
	public Map<Agent, Double> getClassPEValues(Map<Agent, Integer> agents)
	{
		Map<Agent, Double> results = new HashMap<Agent, Double>();
		for (Entry<Agent, Integer> e : agents.entrySet()) {
			// essentially, translate the class label into a double PE value
			results.put(e.getKey(), baseRateCache[e.getValue()]);
		}
		return results;
	}

	public boolean isReady()
	{
		return isReady;
	}

	/**
	 * Get a set of found centroids
	 * 
	 * @return
	 */
	// public Instances getCentroids() {
	// return centroids;
	// }

	public int getNumClusters()
	{
		try {
			return clusterer.numberOfClusters();
		} catch (Exception e) {
			return 0;
		}
	}

}

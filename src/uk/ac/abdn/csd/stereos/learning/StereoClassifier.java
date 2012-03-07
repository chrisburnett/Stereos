package uk.ac.abdn.csd.stereos.learning;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.agents.Agent;
import weka.classifiers.trees.J48;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;

/**
 * This class performs the classification stage, by learning relationships
 * between the input feature vectors and class labels produced by the clusterer.
 * 
 * Was originally intended for the stereotyping stage, but is more suited for
 * finding biases in opinions
 * 
 * @author Chris Burnett
 * 
 */
public class StereoClassifier
{

	// create the cluster objects we will maintain and use
	// objects that make up our Weka data structures
	private FastVector attributes;
	// nominal values that features can take - currently boolean
	private FastVector nominalVals;
	Instances data;

	// our classifier instance
	// REPTree classifier;
	// J48 classifier;
	weka.classifiers.Classifier classifier;
	// number of classes
	private int noOfClasses;

	public int getNoOfClasses()
	{
		return noOfClasses;
	}

	public void setNoOfClasses(int noOfClasses)
	{
		this.noOfClasses = noOfClasses;
	}

	// a helper data structure to make it easier to populate the weka dataset
	Map<String, Integer> attributeMap;
	boolean isReady;

	public StereoClassifier(int noOfClasses)
	{
		isReady = false;
		attributeMap = new HashMap<String, Integer>();
		// instantiate a classifier with options
		setup();
		// this.noOfClasses = noOfClasses;
	}

	public void setup()
	{
		String[] options = {};
		// TODO - automate this as a parameter!
		classifier = new J48();
		try {
			classifier.setOptions(options);
		} catch (Exception e) {
			System.err.println("StereoClassifier: Couldn't instantiate:");
			e.printStackTrace();
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * uk.ac.abdn.csd.stereos.learning.Classifier#addLabelledAgents(java.util
	 * .Map)
	 */
	public void addLabelledAgents(Map<Agent, Integer> labelledAgents, int numClasses)
	{
		setup();
		// noOfClasses = numClasses;
		// get an example agent to build our weka data structure from
		Agent example = labelledAgents.keySet().toArray(new Agent[0])[0];
		Map<String, Integer> exampleFV = example.getFeatures();

		// vector for data attributes
		attributes = new FastVector();

		// All agents will have the entire feature vector, with 0s and 1s
		// indicating
		// whether that feature is present or not. Since this is binary, it
		// would probably
		// be better to have a nominal value specified, but for now we will use
		// a numerical attribute

		// nominal values - only 0 and 1 (true and false, present/not present,
		// etc.)
		nominalVals = new FastVector();
		nominalVals.addElement("0");
		nominalVals.addElement("1");

		// use nominal values for class labels too
		FastVector classVals = new FastVector();
		for (int j = 0; j < numClasses; j++) {
			classVals.addElement(Integer.toString(j));
		}

		// add attributes to correspond to features
		int i = 0;
		for (String fid : exampleFV.keySet()) {
			// nominals
			attributes.addElement(new Attribute(fid, nominalVals));
			// 'remember' the sequence of attributes in the weka dataset
			attributeMap.put(fid, i++);
		}
		// the labels given by the clustering stage - nominal
		attributes.addElement(new Attribute("class", classVals));

		// input the data into our structure. We want each agent, its features
		// and its label as given by the clustering stage
		data = new Instances("Labelled Agents", attributes, 0);
		// indicate the last attribute as the class label
		data.setClassIndex(data.numAttributes() - 1);
		// begin adding data - for each labeled agent instance we have:
		for (Entry<Agent, Integer> labelledAgent : labelledAgents.entrySet()) {
			// convert to the nominal string class label first
			String classLabel = Integer.toString(labelledAgent.getValue());

			// get this agent's features...
			Map<String, Integer> fv = labelledAgent.getKey().getFeatures();
			double instanceVals[] = new double[data.numAttributes()];
			// for each feature in the feature set
			for (Entry<String, Integer> f : fv.entrySet()) {
				String featureId = f.getKey();
				String value = f.getValue().toString();
				int index = attributeMap.get(featureId);
				// put the feature value in the right place in the data vector
				instanceVals[index] = nominalVals.indexOf(value);
			}
			// finally add the class given by the clusterer as the last
			// attribute value
			instanceVals[data.numAttributes() - 1] = classVals.indexOf(classLabel);
			// add the instance to the dataset
			data.add(new Instance(1.0, instanceVals));
		}
	}

	/**
	 * Run the classification algorithm on the data we have, and produce a
	 * classifier from agent features to cluster centroids
	 */
	public void createClassifier()
	{
		// train the classifier
		try {
			classifier.buildClassifier(data);
		} catch (Exception e) {
			System.err.println("StereoClassifier: Error while building classifier:");
			e.printStackTrace();
		}
		// indicate the classifier is ready
		isReady = true;
	}

	/**
	 * Determine which classes a given set of agents should be in, according to
	 * their features.
	 * 
	 * @param trustee
	 * @return the string identifier of the opinion space cluster this agent
	 *         should belong to
	 * @throws Exception
	 *             if something goes wrong during the classification
	 */
	public Map<Agent, Integer> classifyAgents(List<Agent> agents) throws Exception
	{
		// need to convert agents into weka 'instances' first
		// create a new empty dataset for our incoming data
		Instances newData = new Instances(data, agents.size());
		Map<Agent, Integer> results = new HashMap<Agent, Integer>();
		// for each agent...
		for (Agent a : agents) {
			double[] instanceVals = new double[data.numAttributes()];
			// get this agent's features...
			Map<String, Integer> fv = a.getFeatures();
			// for each feature in the feature set
			for (Entry<String, Integer> f : fv.entrySet()) {
				String featureId = f.getKey();
				String value = f.getValue().toString();
				int index = attributeMap.get(featureId);
				// put the feature value in the right place in the data vector
				instanceVals[index] = nominalVals.indexOf(value);
			}
			// add the instance to the dataset
			newData.add(new Instance(1.0, instanceVals));
		}

		// classify the new examples
		for (int i = 0; i < newData.numInstances(); i++) {
			// NOTE - this might not work (int cast)
			int label = (int) classifier.classifyInstance(newData.instance(i));
			// store the results
			results.put(agents.get(i), label);
		}
		return results;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see uk.ac.abdn.csd.stereos.learning.Classifier#getData()
	 */
	public Instances getData()
	{
		return data;
	}

	/**
	 * Initialises the voodoo to visualise the produced tree classifier
	 * 
	 * @throws Exception
	 * @see http://weka.wikispaces.com/Visualizing+a+Tree
	 */
	public void visualize() throws Exception
	{
		// display classifier
		final javax.swing.JFrame jf = new javax.swing.JFrame("Weka Classifier Tree Visualizer");
		jf.setSize(500, 400);
		jf.getContentPane().setLayout(new BorderLayout());
		TreeVisualizer tv = new TreeVisualizer(null, ((J48) classifier).graph(), new PlaceNode2());
		jf.getContentPane().add(tv, BorderLayout.CENTER);
		jf.addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosing(java.awt.event.WindowEvent e)
			{
				jf.dispose();
			}
		});
		jf.setVisible(true);
		tv.fitToScreen();
	}

	public boolean isReady()
	{
		return isReady;
	}

	public String toString()
	{
		return classifier.toString();
	}

}

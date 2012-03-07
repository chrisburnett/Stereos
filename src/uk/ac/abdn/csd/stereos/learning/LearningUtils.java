package uk.ac.abdn.csd.stereos.learning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;
import weka.core.Attribute;
import weka.core.FastVector;
import weka.core.Instance;
import weka.core.Instances;

/**
 * Static utility methods for learning classes.
 * 
 * @author Chris Burnett
 * 
 */
public class LearningUtils
{

	/**
	 * Convert a set of opinions into Weka training instances.
	 * 
	 * @param opinions
	 *            a structure mapping agents to opinions held about them
	 */
	// public static Instances opinionsToInstances(Map<Agent,Opinion> opinions)
	// {
	// Agent[] instancesToAgents = new Agent[opinions.size()];
	// // start setting up the Weka stuff
	// FastVector attributes = new FastVector();
	// // the subject (trustee) of an opinion
	// // NOTE: it may not be necessary to include the ID of the agents.
	// // leaving it out means we don't have to use a filter on the dataset
	// //attributes.addElement(new Attribute("subject", (FastVector) null));
	// // three numerical attributes for b,d and u
	// attributes.addElement(new Attribute("b"));
	// attributes.addElement(new Attribute("d"));
	// attributes.addElement(new Attribute("u"));
	//
	// // setup the data object
	// Instances data = new Instances("Opinions", attributes, 0);
	// int instance = 0;
	// // populate it...
	// for(Entry<Agent,Opinion> e : opinions.entrySet())
	// {
	// Opinion op = e.getValue();
	// Agent ag = e.getKey();
	// // record what instance this agent is to the clusterer
	// instancesToAgents[instance++] = ag;
	// // get the attributes
	// double b = op.getBelief();
	// double d = op.getDisbelief();
	// double u = op.getUncertainty();
	// // now, create an instance
	// double[] vals = new double[data.numAttributes()];
	// vals[0] = b;
	// vals[1] = d;
	// vals[2] = u;
	// // and add it to the dataset
	// data.add(new Instance(1.0, vals));
	// }
	// return data;
	// }

	/**
	 * Convert a set of opinions into Weka training instances.
	 * 
	 * @param opinions
	 *            a structure mapping agents to opinions held about them
	 * @param mfs
	 *            a set of missing feature labels to 'blank out' from the
	 *            training data, to support reduced model learning.
	 */
	public static Instances opinionsToM5PInstances(Map<Agent, Opinion> opinions)
	{
		// get an example agent to build our weka data structure from
		// TODO: problem - this assumes all feature vectors the same length
		// we need to edit the vectors first then use the largest set of
		// features as example
		Agent example = mostFeaturefulAgent(new ArrayList<Agent>(opinions.keySet()));
		Map<String, Integer> exampleFV = example.getFeatures();
		Map<String, Integer> attributeMap = new HashMap<String, Integer>();

		// start setting up the Weka stuff
		FastVector attributes = new FastVector();

		// features
		// nominal values - only 0 and 1 (true and false, present/not present,
		// etc.)
		FastVector nominalVals = new FastVector();
		nominalVals.addElement("0");
		nominalVals.addElement("1");
		// add attributes to correspond to features
		int i = 0;
		for (String fid : exampleFV.keySet()) {
			// nominals
			attributes.addElement(new Attribute(fid, nominalVals));
			// 'remember' the sequence of attributes in the weka dataset
			attributeMap.put(fid, i++);
		}
		// numerical class attribute for probability expectation
		attributes.addElement(new Attribute("exp"));

		// now.....setup the data object
		Instances data = new Instances("Opinions", attributes, 0);
		data.setClassIndex(data.numAttributes() - 1);
		// populate it...for each opinion, get the agent's features and the
		// expectation value
		for (Entry<Agent, Opinion> e : opinions.entrySet()) {
			Opinion op = e.getValue();
			Agent ag = e.getKey();
			// get the numerical class attribute
			double exp = op.getExpectationValue();

			// now, create an instance
			Instance instance = new Instance(data.numAttributes());
			instance.setDataset(data);

			Map<String, Integer> fv = ag.getFeatures();
			// for each feature in the feature set
			for (Entry<String, Integer> f : fv.entrySet()) {
				String featureId = f.getKey();
				int index = attributeMap.get(featureId);

				// check in case it's a missing value
				if (f.getValue() != null) {
					String value = f.getValue().toString();
					// put the feature value in the right place in the data
					// vector
					instance.setValue(index, nominalVals.indexOf(value));
				} else {
					// else set to missing
					instance.setMissing(index);
				}

			}
			// finally add the class given by the clusterer as the last
			// attribute value
			instance.setClassValue(exp);
			// add the instance to the dataset
			data.add(instance);
		}
		return data;
	}

	/**
	 * Convert a set of opinions into Weka training instances with selected
	 * features hidden.
	 * 
	 * @param opinions
	 *            a structure mapping agents to opinions held about them
	 * @param hiddenFeautres
	 *            a set of missing feature labels to 'blank out' from the
	 *            training data, to support reduced model learning.
	 */
	public static Instances opinionsToM5PInstances(Map<Agent, Opinion> opinions, Set<String> hiddenFeatures)
	{
		// get an example agent to build our weka data structure from
		// TODO: problem - this assumes all feature vectors the same length
		// we need to edit the vectors first then use the largest set of
		// features as example
		Agent example = mostFeaturefulAgent(new ArrayList<Agent>(opinions.keySet()));
		Map<String, Integer> exampleFV = example.getFeatures();
		Map<String, Integer> attributeMap = new HashMap<String, Integer>();

		// start setting up the Weka stuff
		FastVector attributes = new FastVector();

		// features
		// nominal values - only 0 and 1 (true and false, present/not present,
		// etc.)
		FastVector nominalVals = new FastVector();
		nominalVals.addElement("0");
		nominalVals.addElement("1");
		// add attributes to correspond to features
		int i = 0;
		for (String fid : exampleFV.keySet()) {
			// if this feature is to be hidden, hide it
			if (!hiddenFeatures.contains(fid)) {
				// nominals
				attributes.addElement(new Attribute(fid, nominalVals));
				// 'remember' the sequence of attributes in the weka dataset
				attributeMap.put(fid, i++);
			}
		}
		// numerical class attribute for probability expectation
		attributes.addElement(new Attribute("exp"));

		// now.....setup the data object
		Instances data = new Instances("Opinions", attributes, 0);
		data.setClassIndex(data.numAttributes() - 1);
		// populate it...for each opinion, get the agent's features and the
		// expectation value
		for (Entry<Agent, Opinion> e : opinions.entrySet()) {
			Opinion op = e.getValue();
			Agent ag = e.getKey();
			// get the numerical class attribute
			double exp = op.getExpectationValue();

			// now, create an instance
			Instance instance = new Instance(data.numAttributes());
			instance.setDataset(data);

			Map<String, Integer> fv = ag.getFeatures();
			// for each feature in the feature set
			for (Entry<String, Integer> f : fv.entrySet()) {
				// hide if marked to be hidden
				if (!hiddenFeatures.contains(f.getKey())) {
					String featureId = f.getKey();
					int index = attributeMap.get(featureId);

					// check in case it's a missing value
					if (f.getValue() != null) {
						String value = f.getValue().toString();
						// put the feature value in the right place in the data
						// vector
						instance.setValue(index, nominalVals.indexOf(value));
					} else {
						// else set to missing
						instance.setMissing(index);
					}
				}
			}
			// finally add the class given by the clusterer as the last
			// attribute value
			instance.setClassValue(exp);
			// add the instance to the dataset
			data.add(instance);
		}
		return data;
	}

	/**
	 * Prepare agents for classification by the MP5 algorithm.
	 * 
	 * @param agents
	 */
	public static Instances agentsToMP5Instances(List<Agent> agents)
	{
		// get an example agent to build our weka data structure from
		Map<String, Integer> exampleFV = mostFeaturefulAgent(agents).getFeatures();
		Map<String, Integer> attributeMap = new HashMap<String, Integer>();

		// start setting up the Weka stuff
		FastVector attributes = new FastVector();

		// features
		// nominal values - only 0 and 1 (true and false, present/not present,
		// etc.)
		FastVector nominalVals = new FastVector();
		nominalVals.addElement("0");
		nominalVals.addElement("1");
		// add attributes to correspond to features
		int i = 0;
		for (String fid : exampleFV.keySet()) {
			// nominals
			attributes.addElement(new Attribute(fid, nominalVals));
			// 'remember' the sequence of attributes in the weka dataset
			attributeMap.put(fid, i++);
		}
		// numerical class attribute for probability expectation
		attributes.addElement(new Attribute("exp"));

		// now.....setup the data object
		Instances data = new Instances("Opinions", attributes, 0);
		data.setClassIndex(data.numAttributes() - 1);
		// populate it...for each opinion, get the agent's features and the
		// expectation value
		for (Agent a : agents) {

			// now, create an instance
			// double[] vals = new double[data.numAttributes()];
			Instance instance = new Instance(data.numAttributes());

			Map<String, Integer> fv = a.getFeatures();
			// for each feature in the feature set
			for (Entry<String, Integer> f : fv.entrySet()) {
				String featureId = f.getKey();
				Attribute att = data.attribute(featureId);

				// check in case it's a missing value
				if (f.getValue() != null) {
					String value = f.getValue().toString();
					// put the feature value in the right place in the data
					// vector
					instance.setValue(att, nominalVals.indexOf(value));
				} else {
					// else set to missing
					instance.setMissing(att);
				}
			}

			// add the instance to the dataset
			data.add(instance);

		}
		return data;
	}

	/**
	 * Return the agent from the set with the largest set of features (for weka
	 * peculiarity)
	 * 
	 * @param agents
	 * @return
	 */
	public static Agent mostFeaturefulAgent(List<Agent> agents)
	{
		Agent bestAgent = null;
		int biggest = 0;
		for (Agent a : agents) {
			if (a.getFeatures().size() > biggest) {
				bestAgent = a;
				biggest = a.getFeatures().size();
			}
		}
		return bestAgent;
	}

}

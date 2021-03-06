package uk.ac.abdn.csd.stereos.learning;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;
import weka.classifiers.Classifier;
import weka.core.Instances;

/**
 * A reduced model learner is really a 'meta' learner - it maintains a set of
 * models of some type, each one representing an observed pattern of missing and
 * present features. In this way, it implements the 'reduced modelling' idea.
 * 
 * Essentially, we want to separate maintain models for missing features. When a
 * model which is less specific than one we've already got a model for is
 * encountered, we can create a new 'reduced' model and train it on the data we
 * already have, only hiding the features that are missing in the classification
 * set.
 * 
 * NOTE: Since we are dealing with a tree, we really don't need to consider all
 * cases of missing features as problematic. A missing feature only causes a
 * problem when it prevents classification, by blocking access to a terminal
 * leaf. Therefore, it's the paths in general which matter. For example, perhaps
 * an example is missing a feature, but this feature would not have been used to
 * classify the instance. How can we detect and deal with this?
 * 
 * Let's ignore this for now. It's an optimisation that would let us reduce the
 * number of models we need to deal with, but for now lets just try to get the
 * basic thing working.
 * 
 * So, get IDs of all salient features and make a set (signiature). I don't
 * think it's necessary to make a hash of strings to integers.
 * 
 * This reduced modeller uses M5P learning instances.
 * 
 * @author cburnett
 * 
 */
public class ReducedModelLearner implements Learner
{

	/**
	 * This model is our base model - the one containing the features currently
	 * considered salient. Once we have constructed the base model, more general
	 * models will be constructed on-line to deal with features not present in
	 * the base model.
	 */
	private Learner baseModel;

	/**
	 * Base signature of the base model
	 */
	private Set<String> baseSignature;

	/**
	 * This structure maps MF (missing feature) signiatures to their appropriate
	 * MF models.
	 */
	private Map<Set<String>, Learner> MFmodels;

	/**
	 * A list of models which should be updated
	 */
	private List<Learner> modelsToUpdate;

	/**
	 * Cache of opinions for lazy training of MF models
	 */
	private Map<Agent, Opinion> opinionCache;

	public ReducedModelLearner()
	{
		baseModel = new M5PLearner();
		MFmodels = new HashMap<Set<String>, Learner>();
		modelsToUpdate = new ArrayList<Learner>();
	}

	/**
	 * This function should behave just like the normal classification function
	 * with one major exception - when an agent to be classified has a feature
	 * missing which is present in the base model, then a 'missing feature'
	 * model should be used. If an appropriate MF model cannot be found, then
	 * one should be created from the experience base. This should be done by
	 * training the new model with examples where the missing feature(s) in the
	 * classification instance are hidden in the examples.
	 * 
	 * So there's two steps to this - matching 'signatures' of missing/present
	 * features to appropriate models, and transforming training experiences
	 * into a form compatible with those models.
	 */
	public Map<Agent, Double> getBaseRates(List<Agent> agents)
	{

		Map<Agent, Double> biases = new HashMap<Agent, Double>();

		// first thing to do is convert the instances
		Instances data = LearningUtils.agentsToMP5Instances(agents);
		// loop through instances and classify
		// we need to make sure instances only go to the right models so that
		// weka
		// doesn't see any missing features
		for (int i = 0; i < data.numInstances(); i++) {
			Set<String> instanceSignature = new HashSet<String>();
			// we can check one by one of the attributes are missing
			for (String f : baseSignature) {
				// check to see if the current salient (base) feature is present
				if (data.instance(i).isMissing(data.attribute(f)))
					// we record that f is not present - empty set means no
					// missing salients
					instanceSignature.add(f);
			}

			// try to classify
			try {
				if (baseModel.isReady()) {

					Set<String> missingSalients = new HashSet<String>(instanceSignature);
					missingSalients.retainAll(baseSignature);

					// if we have a complete set of salient features, and a
					// ready model
					if (missingSalients.size() == 0) {
						List<Agent> target = new ArrayList<Agent>();
						target.add(agents.get(i));
						biases.put(agents.get(i), baseModel.getBaseRates(target).get(agents.get(i)));
					} else {
						// we need to format the incoming data (classification)
						// set to match the model
						// System.out.println("Using MF model :" +
						// instanceSignature + "  BaseSig :" + baseSignature);
						// but if we have a missing feature, deal with it:
						// if we already have this model, and it's ready, get
						// its prediction
						if (MFmodels.containsKey(instanceSignature)) {
							Learner m = MFmodels.get(instanceSignature);

							// if model needs trained, train
							if (modelsToUpdate.contains(m)) {
								train(opinionCache, instanceSignature);
								modelsToUpdate.remove(m);
							}

							if (m.isReady()) {
								// biases.put(agents.get(i),
								// m.getClassifier().classifyInstance(data.instance(i)));
								List<Agent> target = new ArrayList<Agent>();
								target.add(agents.get(i));
								biases.putAll(m.getBaseRates(target));
							} else
								biases.put(agents.get(i), 0.5); // assign
																// default bias
																// for now
						}
						// otherwise create it but wait until the learning
						// interval is called
						else {
							Learner newModel = new M5PLearner();
							newModel.train(opinionCache, instanceSignature);
							MFmodels.put(instanceSignature, newModel);
							List<Agent> target = new ArrayList<Agent>();
							target.add(agents.get(i));
							biases.put(agents.get(i), newModel.getBaseRates(target).get(agents.get(i))); // assign
																											// default
																											// bias
																											// for
																											// now
						}
					}
				}
			} catch (Exception e) {
				System.err.println("MP5Learner: Error while classifying new instances:");
				e.printStackTrace();
			}
		}
		return biases;
	}

	/**
	 * Should be properly extended - how do we calculate RMSE of multiple
	 * models? The base model is trained against all examples so it's not a good
	 * measure...
	 */
	public double getErrorRate()
	{

		// try the average error of the models
		double sum = 0, count = 0;

		for (Learner l : MFmodels.values()) {
			if (l.isReady()) {
				sum += l.getErrorRate();
				count++;
			}
		}

		sum += baseModel.getErrorRate();
		count++;

		double averageError = sum / count;

		return averageError;
	}

	public boolean isReady()
	{
		return baseModel.isReady();
	}

	/**
	 * Train a specific models based on current contents of experience base. We
	 * deal with MF models online, so they only get trained as necessary.
	 */
	public void train(Map<Agent, Opinion> opinions, Set<String> signature)
	{
		Set<String> nonDiagnostics = new HashSet<String>();
		Agent eg = LearningUtils.mostFeaturefulAgent(new ArrayList<Agent>(opinions.keySet()));
		// get rid of all features not indicated by base model as being salient
		// (mark as hidden) - hide all non-diagnostic features in sub models
		for (String f : eg.getFeatures().keySet())
			if (!baseSignature.contains(f))
				nonDiagnostics.add(f);

		// if this is the base signature just go ahead
		Set<String> missingSalients = new HashSet<String>(signature);
		missingSalients.retainAll(baseSignature);
		if (missingSalients.size() == 0) {
			train(opinions);
		} else {
			// given an MF signature get the correct model and train
			nonDiagnostics.addAll(signature);
			MFmodels.get(signature).train(opinions, nonDiagnostics);
			// else {
			// M5PLearner newMFmodel = new M5PLearner();
			//				
			// // add the real missing features to the nondiagnostic ones so
			// they all get hidden
			// nonDiagnostics.addAll(signature);
			// newMFmodel.train(opinions,nonDiagnostics);
			// MFmodels.put(signature, newMFmodel);
			// }
		}
	}

	/**
	 * Return the model signature for a particular model.
	 * 
	 * @param m
	 * @return
	 */
	public Set<String> getModelSignature(Learner m)
	{
		return m.getModelSignature();
	}

	/**
	 * Return the signature of the base model
	 */
	public Set<String> getModelSignature()
	{
		return baseModel.getModelSignature();
	}

	/**
	 * Train the currently held models. Called by trust model on learning
	 * interval.
	 */
	public void train(Map<Agent, Opinion> opinions)
	{
		baseModel.train(opinions);
		baseSignature = getModelSignature();

		// mark all models as requiring training
		// we train models lazily, so we wait until they are needed
		modelsToUpdate = new ArrayList<Learner>(MFmodels.values());

		opinionCache = opinions;
		// retrain all currently stored models
		// for(Entry<Set<String>, Learner> e : MFmodels.entrySet())
		// {
		// train(opinions, e.getKey());
		// }
	}

	/**
	 * Simply return the base classifier
	 */
	public Classifier getClassifier()
	{
		return baseModel.getClassifier();
	}

}

package uk.ac.abdn.csd.stereos.learning;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.lazy.LWL;
import weka.core.Instances;

/**
 * This class represents a stereotype learner using the M5P algorithm for
 * continuous class learner. Therefore, it does not employ a clustering stage.
 * 
 * @author Chris Burnett
 * 
 */
public class LWLLearner implements Learner
{

	private LWL classifier;
	private boolean isReady;
	private Instances data;
	private Evaluation eval;
	private boolean evaluationReady;

	public boolean isEvaluationReady()
	{
		return evaluationReady;
	}

	public LWLLearner()
	{
		classifier = createLearner();
		isReady = false;
	}

	public LWL createLearner()
	{
		String options[] = {};
		LWL myClassifier = new LWL();
		try {
			myClassifier.setOptions(options);
		} catch (Exception e) {
			System.err.println("LWLLLearner: Invalid options:");
			e.printStackTrace();
		}
		return myClassifier;
	}

	public Map<Agent, Double> getBaseRates(List<Agent> agents)
	{
		Map<Agent, Double> biases = new HashMap<Agent, Double>();
		if (isReady) {
			// first thing to do is convert the instances
			Instances data = LearningUtils.agentsToMP5Instances(agents);
			// loop through and classify
			for (int i = 0; i < data.numInstances(); i++) {
				// try to classify
				try {
					double result = classifier.classifyInstance(data.instance(i));
					biases.put(agents.get(i), result);
				} catch (Exception e) {
					System.err.println("LWLLLearner: Error while classifying new instances:");
					e.printStackTrace();
				}
			}
		}

		return biases;

	}

	public boolean isReady()
	{
		return isReady;
	}

	public void train(Map<Agent, Opinion> opinions)
	{

		// filter out uniformative (totally uncertain) opinions
		Map<Agent, Opinion> examples = new HashMap<Agent, Opinion>();
		for (Entry<Agent, Opinion> e : opinions.entrySet())
			if (e.getValue().getUncertainty() != 1.0)
				examples.put(e.getKey(), e.getValue());

		data = LearningUtils.opinionsToM5PInstances(opinions);

		// try to build
		try {
			classifier.buildClassifier(data);
		} catch (Exception e1) {
			System.err.println("M5PLearner: Error during classification:");
			e1.printStackTrace();
		}

		isReady = true;

	}

	/**
	 * Run 10x cross validation
	 */
	public void evaluate()
	{
		if (isReady) {
			// try to evaluate
			try {
				// evaluate the classifier
				eval = new Evaluation(data);
				Classifier tester = createLearner();
				eval.crossValidateModel(tester, data, 10, new Random(1));
			} catch (Exception e1) {
				System.err.println("M5PLearner: Error during evalutaion:");
				e1.printStackTrace();
			}
			this.evaluationReady = true;
		}

	}

	/**
	 * Get the root relative squared error of the predictor.
	 * 
	 * @return -1 if classifier is not setup yet.
	 */
	public double getErrorRate()
	{
		if (evaluationReady)
			return eval.errorRate();
		else
			return -1;
	}

	public String toString()
	{
		return classifier.toString();
	}

	public Set<String> getModelSignature()
	{
		// TODO Auto-generated method stub
		return null;
	}

	public Classifier getClassifier()
	{
		return classifier;
	}

	public void train(Map<Agent, Opinion> opinions, Set<String> hiddenFeatures)
	{
		// filter out uniformative (totally uncertain) opinions
		Map<Agent, Opinion> examples = new HashMap<Agent, Opinion>();
		for (Entry<Agent, Opinion> e : opinions.entrySet())
			if (e.getValue().getUncertainty() != 1.0)
				examples.put(e.getKey(), e.getValue());

		data = LearningUtils.opinionsToM5PInstances(opinions, hiddenFeatures);

		// try to build
		try {
			classifier.buildClassifier(data);
		} catch (Exception e1) {
			System.err.println("M5PLearner: Error during classification:");
			e1.printStackTrace();
		}

		isReady = true;
	}

	public Instances getData()
	{
		// TODO Auto-generated method stub
		return null;
	}

}

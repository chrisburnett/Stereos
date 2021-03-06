package uk.ac.abdn.csd.stereos.learning;

import java.awt.BorderLayout;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Map.Entry;

import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;
import weka.classifiers.Classifier;
import weka.classifiers.Evaluation;
import weka.classifiers.trees.M5P;
import weka.classifiers.trees.m5.RuleNode;
import weka.core.Instances;
import weka.gui.treevisualizer.PlaceNode2;
import weka.gui.treevisualizer.TreeVisualizer;

/**
 * This class represents a stereotype learner using the M5P algorithm for
 * continuous class learner. Therefore, it does not employ a clustering stage.
 * 
 * @author Chris Burnett
 * 
 */
public class M5PLearner implements Learner {

    private M5P classifier;
    private boolean isReady;
    private Instances data;
    private Evaluation eval;
    private boolean evaluationReady;

    public boolean isEvaluationReady() {
        return evaluationReady;
    }

    public M5PLearner() {
        classifier = createLearner();
        isReady = false;
    }

    public M5P createLearner() {
        // String options[] = {"-R"};
        String options[] = {};

        M5P myClassifier = new M5P();
        try {
            myClassifier.setOptions(options);
        } catch (Exception e) {
            System.err.println("M5PLearner: Invalid options:");
            e.printStackTrace();
        }
        return myClassifier;
    }

    public Map<Agent, Double> getBaseRates(List<Agent> agents) {
        Map<Agent, Double> biases = new HashMap<Agent, Double>();
        if (isReady) {
            // first thing to do is convert the instances
            // match the incoming data up to the model
            Instances inData = LearningUtils.agentsToMP5Instances(agents);

            while (inData.numAttributes() > data.numAttributes()) {

                for (int i = 0; i < inData.numAttributes(); i++) {
                    // for each attribute in the new example
                    // if it isn't in the attribute set of this model, delete it
                    if (data.attribute(inData.attribute(i).name()) == null) {
                        inData.deleteAttributeAt(i);
                    }
                }
            }

            // loop through and classify
            for (int i = 0; i < inData.numInstances(); i++) {
                // try to classify
                try {
                    double result = classifier.classifyInstance(inData.instance(i));
                    biases.put(agents.get(i), result);
                } catch (Exception e) {
                    System.err.println("MP5Learner: Error while classifying new instances:");
                    e.printStackTrace();
                    for (int n = 0; n < inData.numAttributes(); n++) {
                        System.out.print(inData.attribute(n).name() + ", ");

                    }
                    System.out.println("");

                    for (int n = 0; n < data.numAttributes(); n++) {
                        System.out.print(data.attribute(n).name() + ", ");

                    }
                    System.out.println("");
                }
            }
        }
        return biases;
    }

    public boolean isReady() {
        return isReady;
    }

    public void train(Map<Agent, Opinion> opinions) {

        // filter out uniformative (totally uncertain) opinions
        Map<Agent, Opinion> examples = new HashMap<Agent, Opinion>();
        for (Entry<Agent, Opinion> e : opinions.entrySet()) {
            if (e.getValue().getUncertainty() != 1.0) {
                examples.put(e.getKey(), e.getValue());
            }
        }

        data = LearningUtils.opinionsToM5PInstances(opinions);

        // try to build
        try {
            classifier.buildClassifier(data);
        } catch (Exception e1) {
            System.err.println("M5PLearner: Error during classification:");
            e1.printStackTrace();
        }
        isReady = true;

        // once built, run the cross validation to get the error
        evaluate();
    }

    public void train(Map<Agent, Opinion> opinions, Set<String> hiddenFeatures) {

        // filter out uniformative (totally uncertain) opinions
        Map<Agent, Opinion> examples = new HashMap<Agent, Opinion>();
        for (Entry<Agent, Opinion> e : opinions.entrySet()) {
            if (e.getValue().getUncertainty() != 1.0) {
                examples.put(e.getKey(), e.getValue());
            }
        }

        data = LearningUtils.opinionsToM5PInstances(opinions, hiddenFeatures);

        // try to build
        try {
            classifier.buildClassifier(data);
        } catch (Exception e1) {
            System.err.println("M5PLearner: Error during classification:");
            e1.printStackTrace();
        }
        isReady = true;

        // once built, run the cross validation to get the error
        evaluate();
    }

    public String toDot() {
        try {
            return classifier.graph();
        } catch (Exception e) {
            System.err.println("M5PLearner: error creating DOT graph:");
            e.printStackTrace();
        }
        return "";
    }

    public String printModelInfo()
    {
        //return this.classifier.getM5RootNode().printLeafModels();
        return this.classifier.getM5RootNode().treeToString(20);


    }

    /**
     * Run 10x cross validation
     */
    public void evaluate() {
        if (isReady) {
            // try to evaluate
            try {
                // evaluate the classifier
                eval = new Evaluation(data);
                // Classifier tester = createLearner();
                eval.crossValidateModel(classifier, data, 10, new Random(1));
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
    public double getErrorRate() {
        if (evaluationReady) {
            return eval.rootMeanSquaredError();
        } else {
            return 0;
        }
    }

    public String toString() {
        return classifier.toString();
    }

    /**
     * Initialises the voodoo to visualise the produced tree classifier
     *
     * @throws Exception
     * @see http://weka.wikispaces.com/Visualizing+a+Tree
     */
    public void visualize() throws Exception {
        // display classifier
        final javax.swing.JFrame jf = new javax.swing.JFrame("Weka Classifier Tree Visualizer");
        jf.setSize(500, 400);
        jf.getContentPane().setLayout(new BorderLayout());
        TreeVisualizer tv = new TreeVisualizer(null, ((M5P) classifier).graph(), new PlaceNode2());
        jf.getContentPane().add(tv, BorderLayout.CENTER);
        jf.addWindowListener(new java.awt.event.WindowAdapter() {

            public void windowClosing(java.awt.event.WindowEvent e) {
                jf.dispose();
            }
        });
        jf.setVisible(true);
        tv.fitToScreen();
    }

    public String getDot() {
        // TODO Auto-generated method stub
        try {
            return classifier.graph();
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }

    public Instances getData() {
        return data;
    }

    /**
     * Return a set of strings representing the features considered salient by
     * this model.
     *
     * @return
     */
    public Set<String> getModelSignature() {
        Set<String> signature = new HashSet<String>();
        traverse(classifier.getM5RootNode(), signature);
        return signature;
    }

    /**
     * Traverse the tree recursively collecting salient attribute IDs
     *
     * @param rn
     * @param sig
     */
    private void traverse(RuleNode rn, Set<String> sig) {
        // System.out.println(classifier.getM5RootNode().treeToString(0));
        // process root
        if (!rn.isLeaf()) {
            String nodeName = data.attribute(rn.splitAtt()).name();
            sig.add(nodeName);
            // now left
            if (rn.leftNode() != null) {
                traverse(rn.leftNode(), sig);
            }
            // now right
            if (rn.rightNode() != null) {
                traverse(rn.rightNode(), sig);
            }
        }

    }

    public Classifier getClassifier() {
        return classifier;
    }
}

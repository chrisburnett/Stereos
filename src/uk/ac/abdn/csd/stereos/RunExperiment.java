package uk.ac.abdn.csd.stereos;

import java.awt.BorderLayout;
import java.awt.Frame;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JTextArea;

import uk.ac.abdn.csd.stereos.exceptions.InvalidParametersException;

import uk.ac.abdn.csd.stereos.util.reporters.ExampleAverageReporter;
import uk.ac.abdn.csd.stereos.util.reporters.ExampleRMSEReporter;
import uk.ac.abdn.csd.stereos.util.reporters.GlobalAverageReporter;
import uk.ac.abdn.csd.stereos.util.reporters.GlobalOpinionsReporter;
import uk.ac.abdn.csd.stereos.util.reporters.GlobalRMSEReporter;
import uk.ac.abdn.csd.stereos.util.reporters.Reporter;
import uk.ac.abdn.csd.stereos.util.reporters.TrustorUtilityReporter;
import uk.ac.abdn.csd.stereos.util.viewer.Viewer;

/**
 * This class executes an experiment profile.
 * 
 * @author Chris Burnett
 * 
 */
public class RunExperiment implements Runnable {

    // reference to a text area in the GUI, for displaying output
    private javax.swing.JTextArea outputArea = null;

    private Properties properties;
    private boolean visualiser;
    private JTextArea statusArea;

    /**
     * @param args
     * @throws InvalidParametersException
     * @throws IOException
     */
    public static void main(String[] args) throws IOException, InvalidParametersException {
        if (args.length > 0) {
            // recording?
            boolean v = false;
            String profile = args[0];
            if (args.length > 1 && args[1].equals("-v"))
                v = true;

            // location of parameter files
            String parametersPath = "experiments/" + profile + ".properties";
            Properties properties = new Properties();

            // read what properties we are using
            FileInputStream in = new FileInputStream(parametersPath);
            properties.load(in);
            in.close();
            properties.put("experimentName", profile);

            RunExperiment experimentRunner = new RunExperiment(properties,v,null);
            (new Thread(experimentRunner)).start();
        }
    }

    public RunExperiment(Properties properties, boolean visualiser, javax.swing.JTextArea statusArea)
    {
        this.properties = properties;
        this.visualiser = visualiser;
        this.statusArea = statusArea;
    }

    public void run() {

         /**
         * This inner class implements the frame that will be used to display
         * the Processing app for viewing the results
         *
         * @author cburnett
         *
         */
        class ExperimentFrame extends Frame {

            public ExperimentFrame(Viewer pa) {
                setLayout(new BorderLayout());
                pa.setVisible(true);
                this.setResizable(true);
                this.setSize(1000, 800);

                pa.init();
                this.add(pa, BorderLayout.CENTER);
            }
        }

        String profile = properties.get("experimentName").toString();

        DateFormat df = new SimpleDateFormat("yyMMddHHmmss");
        String experimentTime = df.format(new Date());

        // location for results
        File expsDir = new File("experiments/" + profile + experimentTime);
        // make the directory if it doesn't already exist
        if (!expsDir.exists()) {
            expsDir.mkdir();
        }


        // check to see if batching was requested
        int runs = 1;
        if (properties.containsKey("runs")) {
            runs = Integer.parseInt(properties.getProperty("runs"));
        }

        String conditionsString = properties.getProperty("conditions");
        String[] conditions = conditionsString.split(",");

        // make a new directory for the results

        // begin batching loop
        for (int k = 1; k <= runs; k++) {
            try {
                System.out.println("Run " + k);
                // array of experiments
                Experiment[] experiments = new Experiment[conditions.length];
                // run each specified condition
                for (int i = 0; i < conditions.length; i++) {
                    String condition = conditions[i];
                    int thisCondition = Integer.parseInt(condition);
                    // if this condition is actually a valid condition
                    if (thisCondition >= Experiment.NT && thisCondition <= Experiment.GGB) {
                        // if the recording flag is set, tell the experiment to
                        // record
                        Viewer viewer = null;
                        if (visualiser) {
                            viewer = new Viewer(1000, 800);
                            ExperimentFrame ef = new ExperimentFrame(viewer);
                            ef.setVisible(true);
                        }
                        String startString = "Running Condition " + thisCondition + "...[";
                        if (statusArea == null) {
                            System.out.print(startString);
                        } else {
                            statusArea.append(startString);
                        }
                        try {
                            experiments[i] = new Experiment("experiments/" + profile + ".properties", visualiser, statusArea);
                            experiments[i].setProfileName(profile + "-" + k);
                            experiments[i].setCondition(thisCondition);
                            experiments[i].run();
                        } catch (InvalidParametersException e) {
                            System.err.println("ERROR: There was a problem with some experimental parameters.");
                            e.printStackTrace();
                        }
                        if (visualiser) {
                            viewer.setData(experiments[i].getHistory());
                        }
                        String endString = "]...complete.\n";
                        if (statusArea == null) {
                            System.out.print(endString);
                        } else {
                            statusArea.append(endString);
                        }
                    }
                    // MP EVAL
                    // if (thisCondition > 3) {
                    // try {
                    // for(int u=0;u<experiments[i].getTrustorCount();u++)
                    // {
                    // Instances data = ((ReducedModelLearner) ((DirectStereoSL)
                    // experiments[i]
                    // .getTrustors().get(u).getTrustModel())
                    // .getLearner()).getData();
                    // ArffSaver saver = new ArffSaver();
                    // saver.setInstances(data);
                    // saver.setFile(new File(expsDir.getPath() +
                    // "/leanerdata/cond"+i+"_t"+u+".arff"));
                    // saver.writeBatch();
                    // }
                    // } catch (Exception e) {
                    // e.printStackTrace();
                    // }
                    // }
                }
                // write the datafiles
                Reporter eaReporter = new ExampleAverageReporter(expsDir);
                Reporter gaReporter = new GlobalAverageReporter(expsDir);
                Reporter opReporter = new GlobalOpinionsReporter(expsDir);
                Reporter eeReporter = new ExampleRMSEReporter(expsDir);
                Reporter geReporter = new GlobalRMSEReporter(expsDir);
                Reporter tuReporter = new TrustorUtilityReporter(expsDir);
                //Reporter puReporter = new ProfileUtilityReporter(expsDir);
                opReporter.writeReport(experiments);
                eaReporter.writeReport(experiments);
                gaReporter.writeReport(experiments);
                eeReporter.writeReport(experiments);
                geReporter.writeReport(experiments);
                //				puReporter.writeReport(experiments);
                tuReporter.writeReport(experiments);
                System.out.println("All experiments completed");
            } catch (IOException ex) {
                Logger.getLogger(RunExperiment.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }
}

package uk.ac.abdn.csd.stereos.util.reporters;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.Experiment;
import uk.ac.abdn.csd.stereos.agents.Agent;
import uk.ac.abdn.csd.stereos.trust.sl.Opinion;

/**
 * A reporter which produces a file containing the final opinion spaces of each
 * agent
 * 
 * @author Chris Burnett
 * 
 */
public class GlobalOpinionsReporter implements Reporter
{

	/**
	 * String that will be appended to the output filename
	 */
	public static final String id = "ops";

	// directory to write to
	private File dir;

	public GlobalOpinionsReporter(File expsDir)
	{
		dir = expsDir;
	}

	public void writeReport(Experiment[] e) throws IOException
	{
		System.out.print("Writing final opinion spaces...");
		String profile = e[0].getProfileName();

		// loop through experiments writing a new file for each
		for (Experiment exp : e) {
			if (!(exp.getCondition() == Experiment.NT)) {
				// a structure mapping agents to an integer matrix index
				Map<Agent, Integer> agentIndices = new HashMap<Agent, Integer>();
				Map<Agent, Integer> trustorIndices = new HashMap<Agent, Integer>();

				List<Agent> trustors = exp.getTrustors();

				List<Agent> agents = exp.getAgents();
				int agentCount = exp.getAgentCount();
				int trustorCount = exp.getTrustorCount();

				String[] trusteeProfileMap = new String[agentCount];
				String[] trustorProfileMap = new String[trustorCount];
				int i = 0;
				int j = 0;
				// setup the agent index map. Agents can now be mapped to an
				// integer array index.
				for (Agent a : agents) {
					agentIndices.put(a, i);
					trusteeProfileMap[i] = a.getProfile().toString();
					i++;
				}
				// same for trustor coordinates
				for (Agent t : trustors) {
					trustorIndices.put(t, j);
					trustorProfileMap[j] = t.getProfile().toString();
					j++;
				}

				// create a file for this condition's results
				PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter(new File(dir, profile + "-" + "c"
						+ exp.getCondition() + "-" + id + ".csv"))));

				// create a new matrix
				Opinion[][] opinionMatrix = new Opinion[agents.size()][trustors.size()];

				// for each trustor in the experiment
				for (Agent a : trustors) {
					// and for each possible partner...
					for (Agent b : agents) {
						// get the opinion, if there is one
						Opinion op = a.getTrustModel().opinionQuery(b);
						// put the opinion in the appropriate cell
						if (op == null)
							op = new Opinion(0.0, 0.0);
						opinionMatrix[agentIndices.get(b)][trustorIndices.get(a)] = op;
					}
				}
				// now, write the matrix to a text file.
				// commas will separate cells, newlines lines
				int k = 0;

				for (Opinion[] row : opinionMatrix) {
					// get the agent this row corresponds to and append its
					// profile as well
					// so we can see if profiles are behaving properly in the
					// viewer
					for (Opinion op : row)
						out.append(op.toStringWithBaseRate() + ",");
					out.append(trusteeProfileMap[k++]);
					out.append("\n");
				}

				for (String s : trustorProfileMap)
					out.append(s + ",");
				out.append("\n");

				out.close();
			}
		}
		System.out.print("...completed.\n");
	}
}

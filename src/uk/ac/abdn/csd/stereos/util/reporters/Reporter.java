package uk.ac.abdn.csd.stereos.util.reporters;

import java.io.IOException;

import uk.ac.abdn.csd.stereos.Experiment;

/**
 * Interface for a class that generates reports for experiments.
 * 
 * @author cburnett
 * 
 */
public interface Reporter
{
	public void writeReport(Experiment[] e) throws IOException;
}

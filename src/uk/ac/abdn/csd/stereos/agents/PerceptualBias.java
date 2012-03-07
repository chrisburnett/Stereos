package uk.ac.abdn.csd.stereos.agents;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.abdn.csd.stereos.exceptions.InvalidParametersException;

/**
 * This class models a perceptual bias, that is, a threshold to use when
 * evaluating the performance of agents with a particular set of features. At
 * the moment, this is quite simple, only storing the threshold and therefore
 * only permitting single-dimension threhsold based subjective evaluation
 * functions. It could be made more complex, but this should suffice for now.
 * 
 * @author cburnett
 */
public class PerceptualBias
{

	private Profile profile;

	/**
	 * the threshold this bias will effect on the SEF (subjective evaluation
	 * function)
	 */
	private double threshold;

	private List<String> features;

	public PerceptualBias(Profile profile, double threshold, List<String> features)
	{
		this.profile = profile;
		this.threshold = threshold;
		this.features = features;
	}

	public static Map<Profile, List<PerceptualBias>> parseBiases(String biasFilePath, List<Profile> profileList)
			throws InvalidParametersException, IOException
	{
		BufferedReader input = new BufferedReader(new FileReader(biasFilePath));
		Map<Profile, List<PerceptualBias>> biases = new HashMap<Profile, List<PerceptualBias>>();

		// we need to be able to match up biases to the profiles they belong to
		Map<String, Profile> profiles = new HashMap<String, Profile>();

		// turn into map
		for (Profile p : profileList)
			profiles.put(p.getId(), p);

		String biasFile;
		while ((biasFile = input.readLine()) != null) {
			List<String> features = new ArrayList<String>();
			String[] biasLines = biasFile.split("\n");
			for (int i = 0; i < biasLines.length; i++) {
				// ignore comments :)
				if (!biasLines[i].startsWith("#")) {
					String[] parts = biasLines[i].split(",");
					String biasType = parts[0];
					// only read behavioural biases
					if (biasType.equalsIgnoreCase("p")) {
						String profileId = parts[1];
						double threshold = Double.parseDouble(parts[2]);

						Profile profile = profiles.get(profileId);

						if (profile == null)
							throw new InvalidParametersException();

						for (int j = 3; j < parts.length; j++)
							features.add(parts[j]);

						PerceptualBias bias = new PerceptualBias(profile, threshold, features);
						if (biases.get(profile) == null)
							biases.put(profile, new ArrayList<PerceptualBias>());
						biases.get(profile).add(bias);
					}

				}
			}
		}
		return biases;
	}

	public double getThreshold()
	{
		return threshold;
	}

	public void setThreshold(double threshold)
	{
		this.threshold = threshold;
	}

	public Profile getProfile()
	{
		return profile;
	}

	public void setProfile(Profile profile)
	{
		this.profile = profile;
	}

	public List<String> getFeatures()
	{
		return features;
	}

	public void setFeatures(List<String> features)
	{
		this.features = features;
	}

}

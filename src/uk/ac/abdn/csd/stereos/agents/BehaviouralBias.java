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
 * This class represents a trustee's behavioural bias. It mainly exists to give
 * somewhere to put the code required to parse the .bias file.
 * 
 * @author cburnett
 * 
 */
public class BehaviouralBias
{

	private EffortLevel effort;
	private Profile profile;
	private List<String> features;

	public BehaviouralBias(Profile p, EffortLevel e, List<String> fs)
	{
		profile = p;
		effort = e;
		features = fs;
	}

	/**
	 * Parse a .bias file into a map, mapping profiles to biases
	 * 
	 * @param biasFilePath
	 * @param profileList
	 * @return
	 * @throws InvalidParametersException
	 * @throws IOException
	 */
	public static Map<Profile, List<BehaviouralBias>> parseBiases(String biasFilePath, List<Profile> profileList)
			throws InvalidParametersException, IOException
	{
		BufferedReader input = new BufferedReader(new FileReader(biasFilePath));
		Map<Profile, List<BehaviouralBias>> biases = new HashMap<Profile, List<BehaviouralBias>>();

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
					if (biasType.equalsIgnoreCase("b")) {
						String profileId = parts[1];
						String effortId = parts[2];

						Profile profile = profiles.get(profileId);
						EffortLevel effort = profile.getEffortLevel(effortId);
						if (profile == null || effort == null)
							throw new InvalidParametersException();

						for (int j = 3; j < parts.length; j++)
							features.add(parts[j]);

						BehaviouralBias bias = new BehaviouralBias(profile, effort, features);
						if (biases.get(profile) == null)
							biases.put(profile, new ArrayList<BehaviouralBias>());
						biases.get(profile).add(bias);
					}

				}
			}
		}
		return biases;
	}

	public EffortLevel getEffort()
	{
		return effort;
	}

	public void setEffort(EffortLevel effort)
	{
		this.effort = effort;
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

	public String toString()
	{
		StringBuffer fs = new StringBuffer();
		for (String f : features)
			fs.append(f + " ");
		return profile.getId() + "," + effort.getMean() + "," + effort.getVariance() + ",[ " + fs.toString() + "]";
	}

}

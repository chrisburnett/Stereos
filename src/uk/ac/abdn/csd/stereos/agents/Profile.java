package uk.ac.abdn.csd.stereos.agents;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.math.random.RandomDataImpl;

/**
 * This class represents a profile for trustee agents. It describes a particular
 * performance distribution and shared features for a particular agent type. The
 * parameters represented by this object allow the experiment to create some
 * agents with particular behavioural characteristics.
 * 
 * This class is used for both simple and decision theoretic agents. If a
 * profile is not instantiated with decision theoretic parameters but is then
 * used for decision theoretic agent experiments, and exception will be thrown.
 * 
 * @author Chris Burnett
 * 
 */
public class Profile
{

	// string to use when only one effort level is used
	private static final String DEFAULT_EFFORT = "e1";

	// We will draw random performance values from the normal distribution
	RandomDataImpl random;

	// Features shared by all agents of this profile
	private Map<String, Double> features;

	// Effort levels and behaviours available to this profile
	private Map<String, EffortLevel> efforts;

	// profile id
	private String id;

	// id of the default effort level this agent will select (non decision
	// theoretic)
	private String defaultEffortLevelId;

	// the default 'asking price' agents of this profile will ask for
	private double defaultAskingPrice;

	// the default ambiguity aversion parameter for this profile
	private double defaultAmbiguityAversion;

	/**
	 * Proportion of total agent population to have this profile
	 */
	private int trusteeCount;
	private int trustorCount;

	public String getId()
	{
		return id;
	}

	// Default constructor
	public Profile()
	{
		this.random = new RandomDataImpl();
	}

	/**
	 * Initialise this profile with a single effort level
	 * 
	 * NOTE - for now, we are representing the features (or shared features, in
	 * this case) of agents as structures mapping a string feature name to a
	 * boolean indicating whether is is present or not. Multi-valued features
	 * can be captured in this way (although it might require more features) and
	 * it might make life a little bit easier when implementing this decision
	 * tree.
	 * 
	 * @param id
	 *            short identifier for this profile
	 * @param meanPerformance
	 *            a mean performance value
	 * @param variance
	 *            statistical deviation in normal distribution
	 * @param proportion
	 *            Proportion of total agent population who will have this
	 *            profile
	 * @param features
	 *            map containing the features shared by agents of this profile
	 */
	public Profile(String id, double meanPerformance, double variance, int trusteeCount, int trustorCount,
			Map<String, Double> features)
	{
		super();
		this.id = id;
		this.trusteeCount = trusteeCount;
		this.trustorCount = trustorCount;
		this.defaultAskingPrice = 0.0;
		this.random = new RandomDataImpl();
		this.features = features;
		efforts = new HashMap<String, EffortLevel>();
		efforts.put(DEFAULT_EFFORT, new EffortLevel(meanPerformance, variance, 0));
		this.defaultEffortLevelId = DEFAULT_EFFORT;
		this.defaultAmbiguityAversion = 0.5;
	}

	/**
	 * Create a profile with a number of effort levels.
	 * 
	 * @param id
	 * @param noAgentsOfProfile
	 * @param features
	 * @param efforts
	 */
	public Profile(String id, int trusteeCount, int trustorCount, double defaultAskingPrice, double defaultAA,
			Map<String, Double> features, Map<String, EffortLevel> efforts)
	{
		super();
		this.id = id;
		this.trusteeCount = trusteeCount;
		this.trustorCount = trustorCount;
		this.defaultAskingPrice = defaultAskingPrice;
		this.random = new RandomDataImpl();
		this.features = features;
		this.efforts = efforts;

		// by default, set it to the first effort level we find
		this.defaultEffortLevelId = efforts.keySet().toArray(new String[0])[0];
		this.defaultAmbiguityAversion = defaultAA;
	}

	public void setId(String id)
	{
		this.id = id;
	}

	public double getDefaultMeanPerformance()
	{
		return efforts.get(defaultEffortLevelId).getMean();
	}

	public void setDefaultMeanPerformance(double meanPerformance)
	{
		efforts.get(defaultEffortLevelId).setMean(meanPerformance);
	}

	public double getDefaultVariance()
	{
		return efforts.get(defaultEffortLevelId).getVariance();
	}

	public void setDefaultVariance(double variance)
	{
		efforts.get(defaultEffortLevelId).setVariance(variance);
	}

	public Map<String, Double> getFeatures()
	{
		return features;
	}

	public void setFeatures(Map<String, Double> features)
	{
		this.features = features;
	}

	public int getTrusteeCount()
	{
		return trusteeCount;
	}

	public void setTrusteeCount(int trusteeCount)
	{
		this.trusteeCount = trusteeCount;
	}

	public int getTrustorCount()
	{
		return trustorCount;
	}

	public void setTrustorCount(int trustorCount)
	{
		this.trustorCount = trustorCount;
	}

	/**
	 * Get a simulated performance value for this profile, given an effort level
	 * 
	 * @return a performance value
	 */
	public double getPerformanceValue(String el)
	{
		EffortLevel e;
		if (efforts.containsKey(el)) {
			e = efforts.get(el);
			// Return a simulated performance value from the normal distribution
			// If mean and sd are both 0, then we assume that uniform (`totally'
			// random) distribution is requested
			return getPerformanceValue(e);
		} else
			return this.getDefaultPerformanceValue();
	}

	public double getPerformanceValue(EffortLevel el)
	{
		if (el.getMean() == 0.0 && el.getVariance() == 0.0)
			return random.nextUniform(0, 1);
		return random.nextGaussian(el.getMean(), el.getVariance());
	}

	/**
	 * Get a simulated -default- performance value for this profile
         *
	 * @return a performance value
	 */
	public double getDefaultPerformanceValue()
	{
		EffortLevel e = efforts.get(defaultEffortLevelId);
		// Return a simulated performance value from the normal distribution
		return random.nextGaussian(e.getMean(), e.getVariance());
	}

	public String getDefaultEffortLevelId()
	{
		return defaultEffortLevelId;
	}

	public void setDefaultEffortLevelId(String defaultEffortLevelId)
	{
		this.defaultEffortLevelId = defaultEffortLevelId;
	}

	/**
	 * When printing out the profile as a string, we only display the default
	 * effort level
	 */
	public String toString()
	{
		StringBuffer out = new StringBuffer();
		// for(Entry<String, EffortLevel> el : efforts.entrySet())
		// out.append("<"+id+":"+el.getKey()+":"+el.getValue().()+":"+el.getValue().getVariance()+">");
		out.append("<" + id + ":" + this.getDefaultMeanPerformance() + ":" + this.getDefaultVariance() + ">");

		return out.toString();
	}

	/**
	 * This method parses a textual specification of a set of agent profiles
	 * into a list of instantiated agent profile classes.
	 * 
	 * Profiles are described according to the following format:
	 * 
	 * id,mean,stdev,count:[features]
	 * 
	 * where:
	 * 
	 * [features] is a comma separated list of pairs (featureId, probability).
	 * <i>probability</i> gives the probability of an agent in this profile
	 * having this feature. Features are boolean at this stage, so probability
	 * of not having the feature is 1-probability.
	 * 
	 * NOTE - This might have to change, if representing the vector differently
	 * makes the decision tree algorithm implementation simpler. For example, it
	 * might be necessary to encode feature vectors as maps mapping a feature to
	 * a boolean value representing presence or not. However, in this case the
	 * underlying feature model (boolean) would remain the same.
	 * 
	 * @param profilePath
	 *            path to a profile descriptor file
	 * @return a list of agent profiles
	 * @throws IOException
	 * @throws NumberFormatException
	 */
	public static List<Profile> parseAgentProfiles(String profilePath) throws NumberFormatException, IOException
	{
		BufferedReader input = new BufferedReader(new FileReader(profilePath));
		String profileString = "";
		List<Profile> profiles = new ArrayList<Profile>();
		while ((profileString = input.readLine()) != null) {
			String[] profileSpecs = profileString.split("\n");
			for (int i = 0; i < profileSpecs.length; i++) {
				// ignore comments :)
				if (!profileSpecs[i].startsWith("#")) {
					String[] parts = profileSpecs[i].split(":");
					String[] params = parts[0].split(",");
					String features = parts[1];
					String efforts = parts[2];
					String id = params[0];
					int trusteeCount = Integer.parseInt(params[1]);
					int trustorCount = Integer.parseInt(params[2]);
					double defaultAskingPrice = Double.parseDouble(params[3]);
					double defaultAA = Double.parseDouble(params[4]);
					Map<String, Double> featureList = new HashMap<String, Double>();
					Map<String, EffortLevel> effortLevels = new HashMap<String, EffortLevel>();

					// regexp pattern for feature list
					Pattern pattern = Pattern.compile("(\\w+),(\\d+\\.\\d+)");
					Matcher m = pattern.matcher(features);
					while (m.find()) {
						String featureName = m.group(1);
						double prob = Double.parseDouble(m.group(2));
						featureList.put(featureName, prob);
					}

					// regexp pattern for effort levels list - mean, sd, cost
					pattern = Pattern.compile("(\\w+),(\\d+\\.\\d+),(\\d+\\.\\d+),(\\d+)");
					m = pattern.matcher(efforts);
					while (m.find()) {
						String effortName = m.group(1);
						double mean = Double.parseDouble(m.group(2));
						double sd = Double.parseDouble(m.group(3));
						double cost = Double.parseDouble(m.group(4));
						// create and add a new effort level
						effortLevels.put(effortName, new EffortLevel(mean, sd, cost));
					}

					// create the new profile with the list of efforts provided
					Profile thisProfile = new Profile(id, trusteeCount, trustorCount, defaultAskingPrice, defaultAA,
							featureList, effortLevels);
					profiles.add(thisProfile);
				}
			}
		}
		return profiles;
	}

	/**
	 * Return an effort level given its ID, or null if no such effort level
	 * exists
	 */
	public EffortLevel getEffortLevel(String effortId)
	{
		return efforts.get(effortId);
	}

	public Map<String, EffortLevel> getEfforts()
	{
		return efforts;
	}

	public void setEfforts(Map<String, EffortLevel> efforts)
	{
		this.efforts = efforts;
	}

	/**
	 * Two profiles are the same if their IDs are the same.
	 * 
	 * @param p
	 * @return
	 */
	public boolean equals(Profile p)
	{
		return p.getId().equals(this.id);
	}

	/**
	 * @param defaultAskingPrice
	 *            the defaultAskingPrice to set
	 */
	public void setDefaultAskingPrice(double defaultAskingPrice)
	{
		this.defaultAskingPrice = defaultAskingPrice;
	}

	/**
	 * @return the defaultAskingPrice
	 */
	public double getDefaultAskingPrice()
	{
		return defaultAskingPrice;
	}

	public double getDefaultAmbiguityAversion()
	{
		return defaultAmbiguityAversion;
	}

	public void setDefaultAmbiguityAversion(double defaultAmbiguityAversion)
	{
		this.defaultAmbiguityAversion = defaultAmbiguityAversion;
	}

}

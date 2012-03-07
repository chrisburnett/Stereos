package uk.ac.abdn.csd.stereos.agents;

import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.NormalDistribution;
import org.apache.commons.math.distribution.NormalDistributionImpl;

/**
 * This class represents a probability distribution from which performance
 * values are drawn.
 * 
 * @author cburnett
 * 
 */
public class EffortLevel
{

	private double mean;
	private double variance;

	// a normal distribution object so we can compute CDF easily
	private NormalDistribution dist;

	/**
	 * Cost to enact this effort level
	 */
	private double cost;

	public EffortLevel(double mean, double variance, double cost)
	{
		super();
		this.mean = mean;
		this.variance = variance;
		this.dist = new NormalDistributionImpl(mean, variance);
		this.cost = cost;

	}

	/**
	 * This method uses the cumulative probability density function of the
	 * effort level's normal distribution to find the probability of the agent's
	 * behaviour being above a certain threshold. In this way, we can find out
	 * what the probability is that we will satisfy a particular threshold
	 * criteria, given that we know the parameters of the associated normal
	 * distribution.
	 * 
	 * @param threshold
	 *            - will usually be 0.5 in non-perceptual-bias conditions
	 * @return
	 */
	public double successProbAtThreshold(double threshold)
	{
		return 1 - failureProbAtThreshold(threshold);
	}

	public double failureProbAtThreshold(double threshold)
	{
		try {
			return dist.cumulativeProbability(threshold);
		} catch (MathException e) {
			System.err.println("EffortLevel: Math Exception while computing CDF");
			e.printStackTrace();
			return 0.0;
		}
	}

	public double getVariance()
	{
		return variance;
	}

	public void setVariance(double variance)
	{
		this.variance = variance;
	}

	public double getCost()
	{
		return cost;
	}

	public void setCost(double cost)
	{
		this.cost = cost;
	}

	public double getMean()
	{
		return mean;
	}

	public void setMean(double mean)
	{
		this.mean = mean;
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(cost);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(mean);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(variance);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj)
	{
		int i = 0;
		EffortLevel other = (EffortLevel) obj;
		if (this == obj)
			return true;
		System.out.println("NO" + i++);
		if (obj == null)
			return false;
		System.out.println("NO" + i++);
		// if (getClass() != obj.getClass())
		// return false;
		System.out.println("NO" + i++);

		if (cost != other.cost)
			return false;
		System.out.println("NO" + i++);

		if (mean != other.mean)
			return false;
		System.out.println("NO" + i++);

		if (variance != other.variance)
			return false;
		System.out.println("NO---" + i++);

		return true;
	}

}

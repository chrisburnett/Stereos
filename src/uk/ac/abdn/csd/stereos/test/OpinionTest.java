package uk.ac.abdn.csd.stereos.test;

import uk.ac.abdn.csd.stereos.trust.sl.*;

public class OpinionTest
{

	/**
	 * @param args
	 */
	public static void main(String[] args)
	{
		double p = 10000, n = 0;
		Opinion o = new Opinion(p, n);
		System.out.println("B: " + o.getBelief());
		System.out.println("D: " + o.getDisbelief());
		System.out.println("U: " + o.getUncertainty());
		System.out.println("Sum: " + (o.getBelief() + o.getDisbelief() + o.getUncertainty()));
		System.out.println("PSL:" + o.getExpectationValue());
		System.out.println("PB:" + (p / (p + n)));
	}

}

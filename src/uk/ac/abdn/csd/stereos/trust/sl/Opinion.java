package uk.ac.abdn.csd.stereos.trust.sl;

/**
 * This class represents a Subjective Logic opinion on a binary state space.
 * 
 * @author Nir Oren, Chris Burnett
 * 
 */
public class Opinion
{
	private double b, d, u, a, r, s;

	/**
	 * Create a new opinion on a binary state space from the positive and
	 * negative evidences provided.
	 * 
	 * @param r
	 *            positive evidence
	 * @param s
	 *            negative evidence
	 */
	public Opinion(double r, double s)
	{
		this.r = r;
		this.s = s;
		this.a = 0.5;
		updateBDU();
	}

	/**
	 * Create a new opinion on a binary state space, specifying b,d and u
	 * 
	 * @param b
	 *            degree of belief
	 * @param d
	 *            disbelief
	 * @param u
	 *            uncertainty
	 */
	public Opinion(double b, double d, double u)
	{
		this.b = b;
		this.d = d;
		this.u = u;
		this.a = 0.5;
	}

	/**
	 * Explicitly create a new opinion
	 * 
	 * @param b
	 *            belief
	 * @param d
	 *            disbelief
	 * @param u
	 *            uncertainty
	 * @param a
	 *            base rate
	 */
	public Opinion(double b, double d, double u, double a)
	{
		this.b = b;
		this.d = d;
		this.u = u;
		this.a = a;
	}

	/**
	 * Creae an opinion from an existing one (copy constructor)
	 * 
	 * @param o
	 */
	public Opinion(Opinion o)
	{
		b = o.b;
		d = o.d;
		u = o.u;
		a = o.a;
		r = o.r;
		s = o.s;
	}


        /**
         * Josang's method
         */
	private void updateBDU()
	{
		this.b = r / (r + s + 2);
		this.d = s / (r + s + 2);
		this.u = 2 / (r + s + 2);

	}

//        /**
//         * Laplace smoothing
//         */
//        private void updateBDU()
//	{
//                double alpha = r / (r+s);
//		this.b = (r + 1) / (r + s + 2);
//		this.d = (s + 1) / (r + s + 2);
//		this.u = 1 / (r + s + 2);
//        }

	/**
	 * Get the probability expectation value of this opinion, using Josang's
	 * formula for producing probability expectation from an opinion.
	 * 
	 * @return E
	 */
	public double getExpectationValue()
	{
		return this.b + (this.a * this.u);
	}

	public double getBelief()
	{
		return b;
	}

	public double getPositives()
	{
		return r;
	}

	public void setPositives(double r)
	{
		this.r = r;
		this.updateBDU();
	}

	public double getNegatives()
	{
		return s;
	}

	public void setNegatives(double s)
	{
		this.s = s;
		this.updateBDU();
	}

	public void setBelief(double belief)
	{
		this.b = belief;
	}

	public double getDisbelief()
	{
		return d;
	}

	public void setDisbelief(double disbelief)
	{
		this.d = disbelief;
	}

	public double getUncertainty()
	{
		return u;
	}

	public void setUncertainty(double uncertainty)
	{
		this.u = uncertainty;
	}

	public double getBaseRate()
	{
		return a;
	}

	public void setBaseRate(double a)
	{
		this.a = a;
	}

	public static Opinion discount(Opinion o1, Opinion o2)
	{
		return new Opinion(o1.b * o2.b, o1.b * o2.d, o1.d + o1.u + o1.b * o2.u, o1.a);
	}

	public static Opinion consensus(Opinion o1, Opinion o2)
	{
		double k = o1.u + o2.u - 2 * o1.u * o2.u;
		if (k == 0) // either uncertainty is 0 or 1
		{
			return o1; // as they should be an agreement???
		}
		return new Opinion((o1.b * o2.u + o2.b * o1.u) / k, (o1.d * o2.u + o2.d * o1.u) / k, o1.u * o2.u / k, (o2.a
				* o1.u + o2.u * o1.a - (o1.a + o2.a) * o1.u * o2.u)
				/ k);
	}

	/**
	 * Return a friendly view of this opinion
	 */
	public String toString()
	{
                double sum = this.b+this.u+this.d;
		return "<" + this.b + ":" + this.d + ":" + this.u + ">" + " " + sum;
	}

	public String toStringWithBaseRate()
	{
		return "<" + this.b + ":" + this.d + ":" + this.u + ":" + this.a + ">";

	}
}

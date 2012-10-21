package util;

/**
 * A few oft-used static math tools
 * @author brendan
 *
 */
public class Math {

	
	/**
	 * Returns the probability that k successes were observed in n trials where the probability
	 * of each success is p
	 * @param k
	 * @param n
	 * @param p
	 * @return
	 */
	public static double binomPDF(int k, int n, double p) {		
		return nChooseK(n, k) * java.lang.Math.pow(p, k) * java.lang.Math.pow(1.0-p, n-k);
	}
	
	/**
	 * Returns the number of ways to select k items from a set of n total
	 * @param n
	 * @param k
	 * @return
	 */
	public static double nChooseK(int n, int k) {
		double prod = 1.0;
		for(double i=1; i<=k; i++) {
			prod *= (double)(n-k+i)/i;
		}
		return prod;
	}
}

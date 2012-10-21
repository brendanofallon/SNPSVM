package snpsvm.bamreading;

/**
 * Lightweight storage for individual variant calls
 * @author brendanofallon
 *
 */
public class Variant {

	public final String contig;
	public final int pos;
	public final char ref;
	public final char alt;
	public final double quality;
	public final double hetProb; 
	
	
	public Variant(String contig, int pos, char ref, char alt, double quality, double hetProb) {
		this.contig = contig;
		this.pos = pos;
		this.ref = ref;
		this.alt = alt;
		this.quality = quality;
		this.hetProb = hetProb;
	}
	
	/**
	 * Returns true if the relative probability that this variant is a heterozygote is greater than 1/2
	 * @return
	 */
	public boolean isHetMostLikely() {
		return hetProb > 0.50;
	}
	
	public String toString() {
		return contig + "\t" + pos + "\t" + ref + "\t" + alt + "\t" + quality + "\t" + hetProb;
	}
}

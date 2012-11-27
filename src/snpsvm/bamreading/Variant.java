package snpsvm.bamreading;

import java.text.DecimalFormat;

/**
 * Lightweight storage for individual variant calls
 * @author brendanofallon
 *
 */
public class Variant implements Comparable<Variant> {

	final DecimalFormat qualFormatter = new DecimalFormat("0.0000");
	public final String contig;
	public final int pos;
	public final char ref;
	public final char alt;
	public final double quality;
	public final double hetProb;
	public final int depth;
	
	
	public Variant(String contig, int pos, char ref, char alt, double quality, int depth, double hetProb) {
		this.contig = contig;
		this.pos = pos;
		this.ref = ref;
		this.alt = alt;
		this.quality = quality;
		this.hetProb = hetProb;
		this.depth = depth;
	}
	
	/**
	 * Returns true if the relative probability that this variant is a heterozygote is greater than 1/2
	 * @return
	 */
	public boolean isHetMostLikely() {
		return hetProb > 0.50;
	}
	
	public String toString() {
		String hetStr = "het";
		if (! isHetMostLikely())
			hetStr = "hom";
		return contig + "\t" + pos + "\t" + (pos+1) + "\t" + ref + "\t" + alt + "\t" + qualFormatter.format(quality) + "\t" + depth + "\t" + hetStr + "\t" + qualFormatter.format(hetProb);
	}

	@Override
	public int compareTo(Variant arg0) {
		if (this.contig.equals(arg0.contig)) {
			if (this.pos == arg0.pos)
				return 0;
			
			return this.pos < arg0.pos ? -1 : 1;
		}
		else {
			return this.contig.compareTo(arg0.contig);
		}
	}
}

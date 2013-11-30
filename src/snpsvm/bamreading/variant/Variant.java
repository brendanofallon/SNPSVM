package snpsvm.bamreading.variant;

import java.text.DecimalFormat;

/**
 * Lightweight storage for individual variant calls
 * @author brendanofallon
 *
 */
public class Variant implements Comparable<Variant> {

	final DecimalFormat qualFormatter = new DecimalFormat("0.000");
	public final String contig;
	public final int pos;
	public final char ref;
	public final char alt;
	public final double quality;
	public final int depth;
	public final int varDepth;
	public final double homRefProb;
	public final double hetProb;
	public final double homAltProb;
	
	
	public Variant(String contig, 
			int pos, 
			char ref, 
			char alt,
			double quality, 
			int depth,
			int varDepth,
			double homRefProb,
			double hetProb,
			double homAltProb) {
		this.contig = contig;
		this.pos = pos;
		this.ref = ref;
		this.alt = alt;
		this.quality = quality;
		this.depth = depth;
		this.varDepth = varDepth;
		this.homRefProb = homRefProb;
		this.hetProb = hetProb;
		this.homAltProb = homAltProb;
	}
	
	/**
	 * Returns true if the probability that this variant is a het is greater than other possibilities
	 * @return
	 */
	public boolean isHetMostLikely() {
		return hetProb > homRefProb && hetProb > homAltProb;
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

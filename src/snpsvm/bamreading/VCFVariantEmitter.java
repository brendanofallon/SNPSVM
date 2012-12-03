package snpsvm.bamreading;

import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import libsvm.LIBSVMModel;

/**
 * Writes a list of variants to a .VCF formatted file 
 * @author brendan
 *
 */
public class VCFVariantEmitter {
		
	public void writeHeader(PrintStream out, FastaReader2 reference, String sampleName, LIBSVMModel model) {
		out.println("##fileformat=VCFv4.1");
		out.println("##FORMAT=<ID=GT,Number=1,Type=String,Description=\"Genotype\">");
		out.println("##FORMAT=<ID=AD,Number=.,Type=Integer,Description=\"Allelic depths for the ref and alt alleles in the order listed\">");
		out.println("##FORMAT=<ID=DP,Number=1,Type=Integer,Description=\"Read Depth (only filtered reads used for calling)\">");
		out.println("##FORMAT=<ID=PL,Number=G,Type=Integer,Description=\"Normalized, Phred-scaled likelihoods for genotypes as defined in the VCF specification\">");
		
		out.println("##INFO=<ID=DP,Number=1,Type=Integer,Description=\"Total read depth at site\">");
		out.println("##INFO=<ID=VD,Number=A,Type=Integer,Description=\"Number of reads containing alt allele\">");
		
		List<String> contigs = new ArrayList<String>();
		FastaIndex refIndex = reference.getIndex();
		contigs.addAll( refIndex.getContigs() );
		Collections.sort(contigs);
		for(String contig : contigs) {
			out.println("##contig=<ID=" + contig + ",length=" + refIndex.getContigLength(contig) + ">");
		}
		
		out.println("##reference=file://" + reference.getFile().getAbsolutePath() );
		out.println("##model=file://" + model.getModelPath() );
		out.println("#CHROM\tPOS\tID\tREF\tALT\tQUAL\tFILTER\tINFO\tFORMAT\t" + sampleName);
		
	}

	public void writeVariants(List<Variant> vars, PrintStream output) throws IOException {
		
		for(Variant var : vars) {
			output.print(var.contig + "\t" + var.pos + "\t.\t" + var.ref + "\t" + var.alt + "\t" + var.qualFormatter.format(var.quality) + "\tPASS");
			
			//INFO fields
			output.print("\tDP=" + var.depth + ";VD=" + var.varDepth);
			
			//FORMAT description
			output.print("\tGT:AD:DP:PL");
			
			//FORMAT fields
			String hetStr = "1/1";
			if (var.isHetMostLikely())
				hetStr = "0/1";
			
			int homRefPL = (int)Math.round(probToPhred(var.homRefProb));
			int hetPL = (int)Math.round(probToPhred(var.hetProb));
			int homAltPL = (int)Math.round(probToPhred(var.homAltProb));
			
			output.print("\t" + hetStr + ":" + (var.depth - var.varDepth) + "," + var.depth + ":" + var.depth + ":" + homRefPL + "," + hetPL + "," + homAltPL );
			
			output.println();
		}
	}
	
	/**
	 * Returns phred-scaled quality value
	 * @param p1
	 * @param p2
	 * @return
	 */
	public static double probToPhred(double p) {
		return -10 * Math.log10( p );
	}
}

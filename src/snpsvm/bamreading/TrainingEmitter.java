package snpsvm.bamreading;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;

import snpsvm.bamreading.FastaIndex.IndexNotFoundException;
import snpsvm.counters.ColumnComputer;


/**
 * Emits a LIBSVM-parsable training file
 * @author brendan
 *
 */
public class TrainingEmitter extends ReferenceBAMEmitter {

	private VariantPositionList knownTrueSites = null;
	private VariantPositionList knownFalseSites = null;
	private int counted = 0;
	private int trueSites = 0;
	private int falseSites = 0;
	
	private double invariantFrac = 0.00001; //Probability that any non-variant individual site will be included in the no-variant class
	private int maxInvariants = 5000; //Dont ever include more than this number of non-variant sites
	private int invariantSites = 0; //Number of non-variant sites included so far
	
	public TrainingEmitter(File knownVarSites,
			File knownFalseSites,
			File reference, 
			File bamFile,
			List<ColumnComputer> counters) throws IOException, IndexNotFoundException {
		super(reference, bamFile, counters); 
		
		this.knownTrueSites = new VariantPositionList(knownVarSites);
		this.knownFalseSites = new VariantPositionList(knownFalseSites);
	}
	
	/**
	 * Set probability that a randomly selected invariant site will be included in the false training set
	 * @param prob
	 */
	public void setInvariantProb(double prob) {
		this.invariantFrac = prob;
	}
	
	public void emitLine(PrintStream out) {
		if (alnCol.getApproxDepth()>1) {
			try {
				knownTrueSites.loadContig(alnCol.getCurrentContig());
				knownFalseSites.loadContig(alnCol.getCurrentContig());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			final char refBase = refReader.getBaseAt(alnCol.getCurrentPosition());
			if (refBase == 'N') {
				return;
			}
			
			boolean hasTwoDiffering = alnCol.hasTwoDifferingBases(refBase);
			counted++;
			String prefix = null;
			if (knownTrueSites.hasSNP( alnCol.getCurrentPosition() ) && hasTwoDiffering) {
				prefix = "1";
				trueSites++;
			}

			if (prefix == null && knownFalseSites.hasSNP( alnCol.getCurrentPosition())) {
				prefix = "-1";
				falseSites++;
			}

			if (prefix == null && invariantSites < maxInvariants && (! hasTwoDiffering)) {
				double r = Math.random();
				if (r < invariantFrac) {
					prefix = "-1";
					invariantSites++;
				}
			}

			if (prefix != null) {
				int index = 1;

				out.print(prefix);
				//out.print(alnCol.getCurrentPosition() + "\t" + refReader.getCurrentBase() + " : " + alnCol.getBasesAsString());
				for(ColumnComputer counter : counters) {
					double[] values = counter.computeValue(refBase, refReader, alnCol);
					for(int i=0; i<values.length; i++) {
						if (values[i] < -1 || values[i] > 1) {
							throw new IllegalArgumentException("Invalid value for counter: " + counter.getName() + " found value=" + values[i]);
						}
						if (Double.isInfinite(values[i]) || Double.isNaN(values[i])) {
							throw new IllegalArgumentException("Invalid value for counter: " + counter.getName() + " found value=" + values[i]);
						}
						if (values[i] != 0)
							out.print("\t" + index + ":" + formatter.format(values[i]) );
						index++;
					}
				}
				out.println();
				
				
				if (positionWriter != null) {
					try {
						positionWriter.write( alnCol.getCurrentContig() + ":" + alnCol.getCurrentPosition() + ":" + refBase + ":" + alnCol.getBasesAsString() + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}		
	}

	public void emitTrainingCounts() {
		DecimalFormat formatter = new DecimalFormat("0.00");
		int tot = trueSites + falseSites + invariantSites;
		double trueFrac = (double)trueSites / (double)tot;
		double falseFrac = (double)falseSites / (double)tot;
		double invarFrac = (double)invariantSites / (double)tot;
		
		System.out.println("   Trainer total sites examined: " + tot);
		System.out.println("     True positive sites found : " + trueSites + "\t" + formatter.format(trueFrac));
		System.out.println("    False positive sites found : " + falseSites + "\t" + formatter.format(falseFrac));
		System.out.println("Invariant positive sites found : " + invariantSites + "\t" + formatter.format(invarFrac));
		//System.out.println("Trainer found \t" + trueSites + " true positive \t" + falseSites + " false positive \t" + invariantSites + " invariant \t"  + counted + " total sites");
	}
}

package snpsvm.bamreading;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import snpsvm.counters.ColumnComputer;


/**
 * Emits a LIBSVM-parsable training file
 * @author brendan
 *
 */
public class TrainingEmitter extends ReferenceBAMEmitter {

	private VariantList knownTrueSites = null;
	private VariantList knownFalseSites = null;
	private int counted = 0;
	private int trueSites = 0;
	private int falseSites = 0;
	
	private double invariantFrac = 0.0005; //Probability that any non-variant individual site will be included in the no-variant class
	private int maxInvariants = 400000; //Dont ever include more than this number of non-variant sites
	private int nonVariantSitesIncluded = 0; //Number of non-variant sites included so far
	
	public TrainingEmitter(File knownVarSites,
			File knownFalseSites,
			File reference, 
			File bamFile,
			List<ColumnComputer> counters) throws IOException {
		super(reference, bamFile, counters); 
		
		this.knownTrueSites = new VariantList(knownVarSites);
		this.knownFalseSites = new VariantList(knownFalseSites);
	}
	
	/**
	 * Set probability that a randomly selected invariant site will be included in the false training set
	 * @param prob
	 */
	public void setInvariantProb(double prob) {
		this.invariantFrac = prob;
	}
	
	public void emitLine(PrintStream out) {
		if (alnCol.getDepth() > 2) {
			try {
				knownTrueSites.loadContig(alnCol.getCurrentContig());
				knownFalseSites.loadContig(alnCol.getCurrentContig());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			final char refBase = refReader.getBaseAt(alnCol.getCurrentPosition());
			//final char refBase = refReader.getBaseAt(alnCol.getCurrentPosition()+1);
			if (refBase == 'N') {
				return;
			}
			
			counted++;
			String prefix = null;
			if (knownTrueSites.hasSNP( alnCol.getCurrentPosition() ) && alnCol.countDifferingBases( refBase) > 2) {
				prefix = "1";
				trueSites++;
			}

			if (prefix == null && knownFalseSites.hasSNP( alnCol.getCurrentPosition())) {
				prefix = "-1";
				falseSites++;
			}

			if (prefix == null && nonVariantSitesIncluded < maxInvariants && alnCol.countDifferingBases(refBase) < 2) {
				double r = Math.random();
				if (r < invariantFrac) {
					prefix = "-1";
					nonVariantSitesIncluded++;
				}
			}

			if (prefix != null) {
				int index = 1;

				out.print(prefix);
				//out.print(alnCol.getCurrentPosition() + "\t" + refReader.getCurrentBase() + " : " + alnCol.getBasesAsString());
				for(ColumnComputer counter : counters) {
					Double[] values = counter.computeValue(refBase, refReader, alnCol);
					for(int i=0; i<values.length; i++) {
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
		System.out.println("Trainer found \t" + trueSites + " true positive \t" + falseSites + " false positive \t" + nonVariantSitesIncluded + " invariant \t"  + counted + " total sites");
	}
}

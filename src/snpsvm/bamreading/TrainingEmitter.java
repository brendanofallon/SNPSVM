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
	
	private final double NO_VARIANT_FRAC = 0.0001; //Probability that any non-variant individual site will be included in the no-variant class
	private final int MAX_NO_VARIANTS = 10000; //Dont ever include more than this number of non-variant sites
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
	
	
	public void emitLine(PrintStream out) {
		if (alnCol.getDepth() > 2) {
			try {
				knownTrueSites.loadContig(refReader.getCurrentTrack());
				knownFalseSites.loadContig(refReader.getCurrentTrack());
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (refReader.getCurrentBase() == 'N') {
				return;
			}
			
			counted++;
			String prefix = null;
			if (knownTrueSites.hasSNP( alnCol.getCurrentPosition() ) && alnCol.countDifferingBases( refReader.getCurrentBase()) > 2) {
				prefix = "1";
				trueSites++;
			}

			if (prefix == null && knownFalseSites.hasSNP( alnCol.getCurrentPosition())) {
				prefix = "-1";
				falseSites++;
			}

			if (prefix == null && nonVariantSitesIncluded < MAX_NO_VARIANTS && alnCol.countDifferingBases(refReader.getCurrentBase()) < 3) {
				double r = Math.random();
				if (r < NO_VARIANT_FRAC) {
					prefix = "-1";
					nonVariantSitesIncluded++;
				}
			}

			if (prefix != null) {
				int index = 1;

				out.print(prefix);
				//out.print(alnCol.getCurrentPosition() + "\t" + refReader.getCurrentBase() + " : " + alnCol.getBasesAsString());
				for(ColumnComputer counter : counters) {
					Double[] values = counter.computeValue(refReader.getCurrentBase(), alnCol);
					for(int i=0; i<values.length; i++) {
						out.print("\t" + index + ":" + values[i] );
						index++;
					}
				}
				out.println();
				
				
				if (positionWriter != null) {
					try {
						positionWriter.write( refReader.getCurrentTrack() + ":" + alnCol.getCurrentPosition() + ":" + refReader.getCurrentBase() + ":" + alnCol.getBasesAsString() + "\n");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}		
	}
	
	
	public void emitWindow(String contig, int start, int end, PrintStream out) throws IOException {
		super.emitWindow(contig, start, end, out);
		
		System.out.println("Training set builder found \n" + trueSites + " true positive \n" + falseSites + " false positive \n" + nonVariantSitesIncluded + " invariant \n"  + counted + " total sites");
	}

}

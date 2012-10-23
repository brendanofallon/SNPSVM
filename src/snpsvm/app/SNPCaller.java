package snpsvm.app;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;

import javax.swing.SwingWorker;

import libsvm.LIBSVMModel;
import libsvm.LIBSVMPredictor;
import libsvm.LIBSVMResult;

import snpsvm.bamreading.BAMWindowStore;
import snpsvm.bamreading.BamWindow;
import snpsvm.bamreading.IntervalList;
import snpsvm.bamreading.ReferenceBAMEmitter;
import snpsvm.bamreading.ResultEmitter;
import snpsvm.bamreading.IntervalList.Interval;
import snpsvm.bamreading.ResultVariantConverter;
import snpsvm.bamreading.Variant;
import snpsvm.counters.ColumnComputer;

/**
 * Reads and emits information from an input BAMWindow, then uses a model to call
 * SNPs. 
 * @author brendanofallon
 *
 */
public class SNPCaller implements Runnable {

	protected static int instanceCount = 0;
	protected int myNumber = instanceCount;
	
	protected final File referenceFile;
	protected final File modelFile;
	protected final IntervalList intervals;
	protected List<ColumnComputer> counters;
	protected List<Variant> variants = null;
	protected BAMWindowStore bamWindows;
	
	private boolean removeTmpFiles = true; //Erase 'data' and 'positions' files after use 
	private int basesComputed = 0;
	
	public SNPCaller(File referenceFile, File modelFile, IntervalList intervals, List<ColumnComputer> counters, BAMWindowStore bamWindows) {
		this.referenceFile = referenceFile;
		this.intervals = intervals;
		this.modelFile = modelFile;
		this.counters = counters;
		this.bamWindows = bamWindows;
		instanceCount++;
	}
	
	
	@Override
	public void run()  {
		try {		
			BamWindow window = bamWindows.getWindow();
			ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(referenceFile, counters, window);

			//Store intermediate results in temporary files
			String tmpDataPrefix =  "." + generateRandomString(12);

			File data = new File(tmpDataPrefix + ".data");
			File positionsFile = new File(tmpDataPrefix + ".pos");
			emitter.setPositionsFile(positionsFile);

			//Read BAM file, write results to temporary file
			PrintStream dataStream = new PrintStream(new FileOutputStream(data));		

			for(String contig : intervals.getContigs()) {

				for(Interval interval : intervals.getIntervalsInContig(contig)) {
					emitter.emitWindow(contig, interval.getFirstPos(), interval.getLastPos(), dataStream);
					basesComputed = interval.getSize();
				}
			}
			
			//CRITICAL: must return its bamWindow to the BAMWindowStore
			bamWindows.returnToStore(window);
			dataStream.close();
	

			LIBSVMPredictor predictor = new LIBSVMPredictor();
			LIBSVMResult result = predictor.predictData(data, new LIBSVMModel(modelFile));
			result.setPositionsFile(positionsFile);

			ResultVariantConverter converter = new ResultVariantConverter();
			variants = converter.createVariantList(result);

			//Remove temporary files 
			if (removeTmpFiles) {
				data.delete();
				positionsFile.delete();
			}
		}
		catch (IOException iox) {

			iox.printStackTrace();
		}
		
	}

	/**
	 * Obtain the approximate number of bases so far called by this caller 
	 * @return
	 */
	public int getBasesComputed() {
		return basesComputed;
	}
	
	/**
	 * A reference to a list containing all called variants in the
	 * @return
	 */
	public List<Variant> getVariantList() {
		return variants;
	}
	
	/**
	 * Returns true if this worker has completed and a non-null variant list is present 
	 * @return
	 */
	public boolean isVariantListCreated() {
		return variants != null;
	}
	
	private static String generateRandomString(int length) {
		StringBuilder strB = new StringBuilder();
		while(strB.length() < length) {
			char c = chars.charAt( (int)(chars.length()*Math.random()) );
			strB.append(c);
		}
		return strB.toString();
	}
	
	private static final String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabscdefhgijklmnopqrstuvwxyz1234567890";
}

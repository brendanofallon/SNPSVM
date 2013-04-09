package snpsvm.bamreading;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;

import libsvm.LIBSVMModel;
import libsvm.LIBSVMPredictor;
import libsvm.LIBSVMResult;
import snpsvm.bamreading.FastaIndex.IndexNotFoundException;
import snpsvm.bamreading.IntervalList.Interval;
import snpsvm.counters.ColumnComputer;

/**
 * Reads and emits information from an input BAMWindow, then uses a model to call
 * SNPs. 
 * @author brendanofallon
 *
 */
public class SNPCaller implements Runnable, HasBaseProgress {

	protected static int instanceCount = 0;
	protected int myNumber = instanceCount;
	
	protected final File referenceFile;
	protected final File modelFile;
	protected final IntervalList intervals;
	protected List<ColumnComputer> counters;
	protected List<Variant> variants = null;
	protected BAMWindowStore bamWindows;
	protected CallingOptions options = null;
	
	private long basesComputed = 0;
	
	
	public SNPCaller(File referenceFile, 
			File modelFile, 
			IntervalList intervals, 
			List<ColumnComputer> counters, 
			BAMWindowStore bamWindows,
			CallingOptions options) {
		this.referenceFile = referenceFile;
		this.intervals = intervals;
		this.modelFile = modelFile;
		this.counters = counters;
		this.bamWindows = bamWindows;
		this.options = options;
		instanceCount++;
	}


	@Override
	public void run()  {
		try {		
			BamWindow window = bamWindows.getWindow();
			ReferenceBAMEmitter emitter = new ReferenceBAMEmitter(referenceFile, counters, window, options);

			//Store intermediate results in temporary files
			String tmpDataPrefix =  "." + generateRandomString(12);

			File data = new File(tmpDataPrefix + ".data");
			File positionsFile = new File(tmpDataPrefix + ".pos");
			BufferedWriter posWriter = new BufferedWriter(new FileWriter(positionsFile));
			emitter.setPositionsWriter(posWriter);

			//Read BAM file, write results to temporary file
			PrintStream dataStream = new PrintStream(new FileOutputStream(data));		
						
			for(String contig : intervals.getContigs()) {
				for(Interval interval : intervals.getIntervalsInContig(contig)) {
					emitter.emitWindow(contig, interval.getFirstPos(), interval.getLastPos(), dataStream);
					basesComputed += interval.getSize();
				}
			}
			
			//CRITICAL: must return its bamWindow to the BAMWindowStore
			bamWindows.returnToStore(window);
			dataStream.close();
			posWriter.close();
	
			LIBSVMPredictor predictor = new LIBSVMPredictor();
			
			LIBSVMResult result = predictor.predictData(data, new LIBSVMModel(modelFile));
			result.setPositionsFile(positionsFile);

			ResultVariantConverter converter = new ResultVariantConverter();
			converter.setVariantQualityCutoff(options.getMinQuality());
			variants = converter.createVariantList(result);

			//Remove temporary files 
			if (options.isRemoveTempFiles()) {
				data.delete();
				positionsFile.delete();
			}
		}
		catch (IOException iox) {

			iox.printStackTrace();
		} catch (IndexNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
		
	}

	/**
	 * Obtain the approximate number of bases so far called by this caller 
	 * @return
	 */
	public long getBasesCalled() {
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

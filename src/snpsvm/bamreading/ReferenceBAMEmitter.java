package snpsvm.bamreading;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

import snpsvm.counters.BinomProbComputer;
import snpsvm.counters.ColumnComputer;

public class ReferenceBAMEmitter {

	final FastaWindow refReader;
	final AlignmentColumn alnCol;
	private Map<String, Integer> contigMap;
	List<ColumnComputer> counters;
	protected File positionsFile = null;
	protected BufferedWriter positionWriter = null;
	protected DecimalFormat formatter = new DecimalFormat("0.0###");
	protected BinomProbComputer binomComputer = new BinomProbComputer(); //Used for initial filtering 
	
	public ReferenceBAMEmitter(File reference, File bamFile, List<ColumnComputer> counters) throws IOException {
		refReader = new FastaWindow(reference);
		contigMap = refReader.getContigSizes();
		alnCol = new AlignmentColumn(bamFile);
		this.counters = counters;
	}
	
	/**
	 * If non-null, positions of each site will be emitted to this file, which helps to resolve 
	 * where variant calls are. 
	 * @param file
	 * @throws IOException 
	 */
	public void setPositionsFile(File file) throws IOException {
		this.positionsFile = file;
		if (positionsFile != null) {
			positionsFile.createNewFile();
			positionWriter = new BufferedWriter(new FileWriter(positionsFile));
		}
		else {
			positionWriter = null;
		}
	}
	
	public void emitLine(PrintStream out) {
		
		if (alnCol.getApproxDepth() > 1) {
            final char refBase = refReader.getBaseAt(alnCol.getCurrentPosition());
            boolean hasTwoDifferringBases = alnCol.hasTwoDifferingBases(refBase);

            if (! hasTwoDifferringBases) {
               return;
            }

			//System.out.println(alnCol.getCurrentPosition() + "\t" + refBase + " : " + alnCol.getBasesAsString());
			out.print("-1"); //libsvm requires some label here but doesn't use it
			int index = 1;
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
	
	/**
	 * Emit all information for all contigs
	 * @throws IOException 
	 */
	public void emitAll(PrintStream out) throws IOException {
		for(String contig : contigMap.keySet()) {
			emitContig(contig, out);
		}
	}
	
	public void emitContig(String contig) throws IOException {
		emitContig(contig, System.out);
	}
	
	/**
	 * Emit all information for given contig
	 * @param contig
	 * @throws IOException 
	 */
	public void emitContig(String contig, PrintStream out) throws IOException {
		if (! contigMap.containsKey(contig)) {
			throw new IllegalArgumentException("Reference does not have contig : " + contig);
		}
		Integer size = contigMap.get(contig);
		emitWindow(contig, 1, size, out);
	}
	
	public void emitWindow(String contig, int start, int end) throws IOException {
		emitWindow(contig, start, end, System.out);
	}
	
	public void emitWindow(String contig, int start, int end, PrintStream out) throws IOException {
		
		refReader.resetTo(contig, Math.max(1, start-refReader.windowSize/2)); //Left edge of window, centers window on start position
		alnCol.advanceTo(contig, start);
		
		int curPos = start;
		while(curPos < end && alnCol.hasMoreReadsInCurrentContig()) {
			emitLine(out);
			
			if (refReader.indexOfLeftEdge()<(alnCol.getCurrentPosition()-refReader.windowSize/2))
				refReader.shift();
			alnCol.advance(1);
			curPos++;
			
			//Sanity check
			if (alnCol.getCurrentPosition() != (curPos)) {
				System.err.println("Yikes, bam reader position is not equal to current position");
			}
		}
		
		if (positionWriter != null)
			positionWriter.flush();
		
	}
	

}

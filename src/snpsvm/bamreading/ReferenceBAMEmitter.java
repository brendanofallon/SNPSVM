package snpsvm.bamreading;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.Map;

import snpsvm.counters.ColumnComputer;

public class ReferenceBAMEmitter {

	final FastaReader refReader;
	final AlignmentColumn alnCol;
	private Map<String, Integer> contigMap;
	List<ColumnComputer> counters;
	protected File positionsFile = null;
	protected BufferedWriter positionWriter = null;
	
	public ReferenceBAMEmitter(File reference, File bamFile, List<ColumnComputer> counters) throws IOException {
		refReader = new FastaReader(reference);
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
		if (alnCol.getDepth() > 2 && alnCol.countDifferingBases(refReader.getCurrentBase())>1) {
			//out.print(refReader.getCurrentBase() + " : " + alnCol.getBasesAsString());
			out.print("-1"); //libsvm requires some label here but doesn't use it
			int index = 1;
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
		emitWindow(contig, 10, size, out);
	}
	
	public void emitWindow(String contig, int start, int end) throws IOException {
		emitWindow(contig, start, end, System.out);
	}
	
	public void emitWindow(String contig, int start, int end, PrintStream out) throws IOException {
		refReader.advanceToTrack(contig);
		refReader.advance(start);
		alnCol.advanceTo(contig, start+1);
		
		int curPos = start;
		while(curPos < end && alnCol.hasMoreReadsInCurrentContig()) {
			emitLine(out);
			
			refReader.advance(1);
			alnCol.advance(1);
			curPos++;
			
			//Sanity check
			if (refReader.getCurrentPos() != curPos) {
				System.err.println("Yikes, reference reader position is not equal to current position");
			}
			if (alnCol.getCurrentPosition() != (curPos+1)) {
				System.err.println("Yikes, bam reader position is not equal to current position");
			}
		}
		
		if (positionWriter != null)
			positionWriter.flush();
		
	}
	

}

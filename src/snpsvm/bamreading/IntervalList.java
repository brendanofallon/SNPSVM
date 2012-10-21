package snpsvm.bamreading;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Container for a sorted list of Intervals, grouped by contig
 * @author brendan
 *
 */
public class IntervalList {

	protected Map<String, List<Interval>> intervals = new HashMap<String, List<Interval>>();
	
	/**
	 * Create a single new list that contains all intervals
	 * @return
	 */
	public List<Interval> asList() {
		List<Interval> list = new ArrayList<Interval>();
		for(String contig : getContigs() ) {
			for(Interval interval : getIntervalsInContig(contig)) {
				list.add(interval);
			}
		}
		return list;
	}
	
	/**
	 * Parse the given string to look for intervals, we currently use the form...
	 *  chr12:123-987,chrX:77632-99829,etc,etc
	 * @param inputStr
	 */
	public void buildFromString(String inputStr) {
		intervals = new HashMap<String, List<Interval>>();
		String[] toks = inputStr.split(",");
		for(int i=0; i<toks.length; i++) {
			String tok = toks[i].trim();
			if (tok.contains(":")) {
				String chr = tok.substring(0, tok.indexOf(":"));
				String posStr = tok.substring(tok.indexOf(":")+1, tok.length());
				if (posStr.contains("-")) {
					try {
						Integer startPos = Integer.parseInt(posStr.substring(0, posStr.indexOf("-")));
						Integer endPos = Integer.parseInt(posStr.substring(posStr.indexOf("-")+1, posStr.length()));
						System.out.println("Adding interval contig : " +  chr + " positions: " + startPos + "-" + endPos);
						addInterval(chr, new Interval(startPos, endPos));
					}
					catch(NumberFormatException nfe){ 
						System.err.println("Interval parsing error: could not parse interval from :" + inputStr);
						return;
					}
				}
				else {
					try {
						Integer pos = Integer.parseInt(posStr);
						addInterval(chr, new Interval(pos, pos));
					}
					catch(NumberFormatException nfe) {
						System.err.println("Interval parsing error: could not parse interval from :" + inputStr);
					}
				}
			}
			else {
				Interval interval = new Interval(1, Integer.MAX_VALUE);
				System.out.println("Adding interval contig : " +  tok + " positions: all");
				addInterval(tok, interval);
			}
		}
	}
	
	public void buildFromBEDFile(File bedFile) throws IOException {
		intervals = new HashMap<String, List<Interval>>();
		
		BufferedReader reader = new BufferedReader(new FileReader(bedFile));
		String line = reader.readLine();
		while(line != null && line.startsWith("#")) {
			line = reader.readLine();
		}
		
		while(line != null) {
			if (line.trim().length() > 1) {
				String[] toks = line.split("\t");
				if (toks.length > 2) {
					String contig = toks[0];
					try {
						int first = Integer.parseInt(toks[1]);
						int last = Integer.parseInt(toks[2]);
						Interval inter = new Interval(first, last);
						addInterval(contig, inter);
					}
					catch (NumberFormatException nfe) {
						System.err.println("Warning: Could not parse position for BED file line: " + line );
					}
					
				}
				else {
					System.err.println("Warning: Incorrect number of tokens on BED file line: " + line );
				}
			}
			line = reader.readLine();
		}
		
		sortAllIntervals();
		reader.close();
	}
	
	public void sortAllIntervals() {
		if (intervals != null) {
			for(String contig : intervals.keySet()) {
				Collections.sort( intervals.get(contig) );
			}
		}
	}
	
	/**
	 * Add the given interval to the list 
	 * @param contig
	 * @param interval
	 */
	public void addInterval(String contig, Interval interval) {
		List<Interval> cInts = intervals.get(contig);
		if (cInts == null) {
			cInts = new ArrayList<Interval>(256);
			intervals.put(contig, cInts);
		}
		cInts.add(interval);
	}
	
	/**
	 * Add a new interval spanning the given region to the list
	 * @param contig
	 * @param start
	 * @param end
	 */
	public void addInterval(String contig, int start, int end) {
		List<Interval> cInts = intervals.get(contig);
		if (cInts == null) {
			cInts = new ArrayList<Interval>(256);
			intervals.put(contig, cInts);
		}
		cInts.add(new Interval(start, end));
	}
	
	/**
	 * Returns the number of bases covered by all of the intervals
	 * @return
	 */
	public int getExtent() {
		int size = 0;
		if (intervals == null) {
			return 0;
		}
		
		for(String contig : getContigs()) {
			List<Interval> intList = getIntervalsInContig(contig);
			for(Interval interval : intList) {
				size += interval.getSize();
			}
		}
		return size;
	}
	
	/**
	 * Obtain a collection containing the names of all contigs (aka chromosomes, aka sequences)
	 * in this set of intervals
	 * @return
	 */
	public Collection<String> getContigs() {
		return intervals.keySet();
	}
	
	/**
	 * Obtain a list of all intervals in the given contig
	 * @param contig
	 * @return
	 */
	public List<Interval> getIntervalsInContig(String contig) {
		return intervals.get(contig);
	}
	
	/**
	 * Returns the number of intervals in this interval collections
	 * @return
	 */
	public int getIntervalCount() {
		
		if (intervals == null) {
			return 0;
		}
		
		int size = 0;
		for(String contig : intervals.keySet()) {
			List<Interval> intList = intervals.get(contig);
			size += intList.size();
		}
		return size;
	}
	
	public class Interval implements Comparable<Interval> {
		final int firstPos;
		final int lastPos;
		
		public Interval(int first, int last) {
			this.firstPos = first;
			this.lastPos = last;
		}
		
		public int getFirstPos() {
			return firstPos;
		}
		
		public int getLastPos() {
			return lastPos;
		}
		
		public int getSize() {
			return lastPos - firstPos + 1;
		}
		
		public int compareTo(Interval inter) {
			return this.firstPos - inter.firstPos;
		}
	}

	
}

package snpsvm.bamreading.intervalProcessing;

import java.util.Iterator;
import java.util.List;

import snpsvm.bamreading.intervalProcessing.IntervalList.Interval;

/**
 * This type of interval splitter does its best to subdivide a list of intervals into
 * two equally sized sets.. more or less bisecting the given list 
 * @author brendanofallon
 *
 */
public class IntervalBisector implements IntervalSplitter {

	@Override
	public IntervalList[] splitIntervals(IntervalList intervals) {
		IntervalList[] subIntervals = new IntervalList[2];
		subIntervals[0] = new IntervalList();
		subIntervals[1] = new IntervalList();
		
		//If only one interval, split it in half
		if (intervals.getIntervalCount() == 1) {
			List<Interval> intList = intervals.asList();
			String contig = intervals.getContigs().iterator().next();
			
			Interval interval = intList.get(0);
			int midPoint = (interval.getFirstPos() + interval.getLastPos())/2;
			if (Math.random() < 0.5) {
				subIntervals[0].addInterval(contig, interval.getFirstPos(), midPoint);
				subIntervals[1].addInterval(contig, midPoint, interval.getLastPos());
			}
			else {
				subIntervals[1].addInterval(contig, interval.getFirstPos(), midPoint);
				subIntervals[0].addInterval(contig, midPoint, interval.getLastPos());
			}
			return subIntervals;
		}
		
		//If exactly two intervals, just put one into each subinterval
		if (intervals.getIntervalCount() == 2) {
			List<Interval> intList = intervals.asList();
			
			Iterator<String> contigIt = intervals.getContigs().iterator();
			
			String contig0 = contigIt.next();
			String contig1 = contigIt.next();
			
			Interval interval1 = intList.get(0);
			Interval interval2 = intList.get(1);
			
			if (Math.random() < 0.5) {
				subIntervals[0].addInterval(contig0, interval1);
				subIntervals[1].addInterval(contig1, interval2);
			}
			else {
				subIntervals[1].addInterval(contig0, interval1);
				subIntervals[0].addInterval(contig1, interval2);
			}
			return subIntervals;
		}
		
		long fullSize = intervals.getExtent();
		long extentSoFar = 0;
		int index = 0;
		for(String contig : intervals.getContigs()) {
			for(Interval interval : intervals.getIntervalsInContig(contig)) {
				if (extentSoFar > fullSize / 2) {
					index = 1;
				}
				subIntervals[ index ].addInterval(contig, interval);
				extentSoFar += interval.getSize();
			}
		}
		
		//Detect error that crops up when one last interval encountered is bigger
		//than half the full extent, in this case last interval size will be zero
		if (index == 0) {
			Interval bigInterval = subIntervals[0].biggestInterval();
			String contig = subIntervals[0].contigOfInterval(bigInterval);
			if (bigInterval != null) {
				subIntervals[0].removeInterval(bigInterval);
				subIntervals[1].addInterval(contig, bigInterval);
			}
		}
		
		if (subIntervals[0].getExtent() == 0 || subIntervals[1].getExtent() == 0) {
			throw new IllegalStateException("Interval subdivision failed...");
		}
		
		return subIntervals;
	}

}

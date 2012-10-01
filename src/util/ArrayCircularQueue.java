package util;

/**
 * A simple array-based circular queue implementation, with constant-time random access to elements.
 * This is only used in FastaWindow and is strictly char-based for performance reasons
 * Chars are added to the right edge and removed from the left edge 
 *   
 * @author brendan
 *
 */
public class ArrayCircularQueue {
	
	private int leftIndex = 0;
	private int rightIndex = 0;
	private char[] queue;
	
	public ArrayCircularQueue (int maxElements) {
		queue=new char[maxElements+1];
	}
	
	/**
	 * Remove all elements from the queue
	 */
	public void clear() {
		leftIndex = 0;
		rightIndex = 0;
	}
	
	/**
	 * Add a new object to the right end of the queue
	 * @param o
	 * @throws FullQueueException
	 */
	public void add(char c) throws FullQueueException {
		int temp =rightIndex;
		if (leftIndex== (rightIndex+1)% queue.length ) {
			rightIndex = temp;
			throw new FullQueueException();
		}
		queue[rightIndex ] = c;
		rightIndex = (rightIndex + 1) %queue.length;
	}

	public int size() {
		if (rightIndex > leftIndex)
			return rightIndex - leftIndex;
		else
			return rightIndex - leftIndex + queue.length;
	}
	
	public char get(int which) {
		if (which >= size())
			throw new IllegalArgumentException("Only " + size() + " objects in queue, cannot retrieve " + which);
		return queue[ (leftIndex+which)%queue.length ];
	}
	
	
	public boolean isEmpty () {
		return leftIndex==rightIndex ;
	}

	public boolean isFull () {
		return ((rightIndex + 1) %queue.length) ==leftIndex;
	}

	public Object remove() throws EmptyQueueException {
		if (leftIndex==rightIndex )
			throw new EmptyQueueException();
		leftIndex= (leftIndex+ 1) %queue.length;
		return queue[leftIndex];
	}	
	
	
	public class FullQueueException extends Exception {
		
	}
	
	public class EmptyQueueException extends Exception {
	}
	
	public String toString() {
		StringBuilder strB = new StringBuilder();
		strB.append("Size:" + size() +"\t");
		for(int i=0; i<size(); i++) {
			strB.append( get(i) + ", ");
		}
		return strB.toString();
	}
	
}

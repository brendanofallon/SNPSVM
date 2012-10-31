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
	
	private static int count = 0;
	private int leftIndex = 0;
	private int rightIndex = 0;
	private char[] queue;
	
	public ArrayCircularQueue (int maxElements) {
		queue=new char[maxElements+1];
		count++;
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
		if (rightIndex >= leftIndex)
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
	
	
	public class FullQueueException extends RuntimeException {
		
	}
	
	public class EmptyQueueException extends RuntimeException {
	}
	
	public String toString() {
		StringBuilder strB = new StringBuilder();
		strB.append("Size:" + size() +"\t empty: " + isEmpty() + " full:" + isFull() + "\t");
		for(int i=0; i<size(); i++) {
			strB.append( get(i) + ", ");
		}
		return strB.toString();
	}
	
//	public static void main(String[] args) {
//		
//		ArrayCircularQueue q = new ArrayCircularQueue(6);
//		try {
//			System.out.println(q);
//			q.add('A');
//			System.out.println(q);
//			q.add('B');
//			System.out.println(q);
//			q.add('C');
//			System.out.println(q);
//			q.add('D');
//			System.out.println(q);
//			q.add('E');
//			System.out.println(q);
//			q.add('F');
//			System.out.println(q);
//			q.remove();
//			q.remove();
//			q.remove();
//			q.remove();
//			q.remove();
//			q.remove();
//			System.out.println(q);
//		} catch (FullQueueException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		} catch (EmptyQueueException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		
//	}
	
}

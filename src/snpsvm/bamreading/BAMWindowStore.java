package snpsvm.bamreading;

import java.io.File;
import java.util.Stack;

/**
 * Thread-safe access to a small number of BAMWindows so we're not always creating
 * and destroying them
 * @author brendanofallon
 *
 */
public class BAMWindowStore {

	final int MAX_WINDOWS = 8;
	private static int initialWindowCount = 2;
	
	private File bamSource = null;
	private Stack<BamWindow> windows = new Stack<BamWindow>();
	
	public BAMWindowStore(File bamSourceFile, int initialSize) {
		bamSource = bamSourceFile;
		initialWindowCount = initialSize;
		for(int i=0; i<initialWindowCount; i++) {
			windows.push(new BamWindow(bamSource));
		}
	}

	/**
	 * Obtain a new window from the store of windows
	 * @return
	 */
	public synchronized BamWindow getWindow() {
		if (windows.isEmpty()) {
			System.out.println("Warning : creating new bam window since stack size is zero");
			return new BamWindow(bamSource);
		}
		else {
			return windows.pop();
		}
	}
	
	public synchronized void returnToStore(BamWindow window) {
		if (windows.size() < MAX_WINDOWS)
			windows.push(window);
	}
		
}

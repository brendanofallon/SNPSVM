package util;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A simple timer that can be repeatedly started and stopped and given a label. It keeps an elapsed time
 * which is the total amount of time between calls to start and stop. This class also provides some
 * static getters so Timers can be easily accessed 'globally'
 * 
 * @author brendan
 *
 */
public class Timer {

	private boolean isRunning = false;
	private long lastStartTime = -1;
	private long totalTime = 0;
	private int starts = 0;
	
	static int totalCount;
	static List<Timer> timers = new ArrayList<Timer>(5);
	static Map<String, Integer> labelTable = new HashMap<String, Integer>();
	
	String label = String.valueOf(totalCount);
	
	NumberFormat formatter = new DecimalFormat("###0.00");
	
	public Timer() {
		totalCount++;
		timers.add(this);
	}
	
	public Timer(String label) {
		if (labelExists(label)) {
			throw new IllegalArgumentException("A timer with label " + label + " already exists");
		}
		totalCount++;
		this.label = label;
		labelTable.put(label, timers.size());
		timers.add(this);
	}
	
	/**
	 * Destroy all current timers. 
	 */
	public static void clearAllTimers() {
		timers.clear();
		labelTable.clear();
	}
	
	public String getLabel() {
		return label;
	}
	
	public String toString() {
		return label + " elapsed time (secs): " + formatter.format(getTotalTimeSeconds()) + " calls: " + starts + " time per call (ms): " + formatter.format(getTotalTimeMS() / (double)starts);
	}
	
	public static Timer getTimer(int index) {
		if (index >= timers.size())
			return null;
		else
			return timers.get(index);
	}
	
	/**
	 * Obtain the timer with the given label, or null if no such timer exists
	 * @param label
	 * @return
	 */
	public static Timer getTimer(String label) {
		Integer index = labelTable.get(label);
		if (index == null)
			return null;
		else {
			return getTimer(index);
		}
	}
	
	/**
	 * Start the timer with the given label. Returns true if a timer with
	 * the label was found, otherwise false. 
	 * @param label
	 * @return
	 */
	public synchronized static boolean startTimer(String label) {
		Timer t = getTimer(label);
		if (t != null) {
			t.start();
			return true;
		}
		else {
			return false;
		}
	}
	
	
	/**
	 * Stop the timer with the given label. Returns true if a timer with
	 * the label was found, otherwise false. 
	 * @param label
	 * @return
	 */
	public synchronized static boolean stopTimer(String label) {
		Timer t = getTimer(label);
		if (t != null) {
			t.stop();
			return true;
		}
		else {
			return false;
		}
	}
	
	public static void listAllTimers() {
		for(Timer timer : timers) {
			System.out.println(timer);
		}
	}
	
	/**
	 * Begin tracking elapsed time
	 */
	public synchronized void start() {
		if (!isRunning) {
			lastStartTime = System.currentTimeMillis();
			starts++;
		}
		isRunning = true;
	}
	
	/**
	 * Stop this timer from running. Has no effect if timer is not running
	 */
	public synchronized void stop() {
		if (isRunning) {
			long elapsed = System.currentTimeMillis() - lastStartTime;
			totalTime += elapsed;
			isRunning = false;
		}
	}
	
	/**
	 * Returns the total time this timer has been running in milliseconds
	 * @return
	 */
	public long getTotalTimeMS() {
		if (isRunning) {
			return totalTime + lastStartTime-System.currentTimeMillis();
		}
		else {
			return totalTime;
		}
	}
	
	/**
	 * Set the total time of this timer back to zero
	 */
	public void clear() {
		totalTime = 0;
	}
	
	public double getTotalTimeSeconds() {
		return (double)getTotalTimeMS() / 1000.0;
	}
	
	
	/**
	 * Returns true if a timer with the given label already exists
	 * @param label
	 * @return
	 */
	private boolean labelExists(String label) {
		for(String lab : labelTable.keySet()) {
			if (lab.equals(label)) {
				return true;
			}
		}
		return false;
	}
}

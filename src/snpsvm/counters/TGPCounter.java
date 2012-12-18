package snpsvm.counters;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import snpsvm.bamreading.AlignmentColumn;
import snpsvm.bamreading.FastaWindow;

/**
 * Computes the frequency in 1000 Genomes of variants at the site
 * @author brendan
 *
 */
public class TGPCounter implements ColumnComputer {

	final double[] value = new double[1];
	
	static final Map<String, Map<Integer, Integer>> map = Collections.synchronizedMap( new HashMap<String, Map<Integer, Integer>>());
	static boolean initialized = false;
	static boolean initializing = false;
	
	static String dataFilePath = "/home/brendan/workspace/SNPSVM/1000G.exome.freqs.csv";
	
	static {
		initializing = true;
		try {
			BufferedReader reader = new BufferedReader(new FileReader(dataFilePath));
			String line =reader.readLine();
			while(line != null) {
				String[] toks = line.split("\t");
				if (toks.length != 6) {
					line= reader.readLine();
					continue;
				}
				String contig = toks[0];
				Integer pos = Integer.parseInt(toks[1]);
				Double freq = Double.parseDouble(toks[5]);
				Integer intFreq = (int)(freq*10000.0);
				
				if (! map.containsKey(contig)) {
					Map<Integer, Integer> contigMap = new HashMap<Integer, Integer>(100000);
					map.put(contig, contigMap);
				}
				
				Map<Integer, Integer> contigMap = map.get(contig);
				contigMap.put(pos, intFreq);
				
				line = reader.readLine();
			}
			initialized = true;
			reader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			
		}
		initializing = false;
	}
	
	@Override
	public String getName() {
		return "1000G.freq";
	}

	@Override
	public int getColumnCount() {
		return 1;
	}

	@Override
	public String getColumnDesc(int which) {
		return "Frequency in 1000 Genomes project";
	}

	private boolean isInitializing() {
		return initializing;
	}
	
	@Override
	public double[] computeValue(char refBase, FastaWindow window,
			AlignmentColumn col) {

		while( isInitializing() ) {
			System.out.println("Waiting for initialization to complete..");
			try {
				Thread.sleep(250);
			} catch (InterruptedException e) {
			}
			
		}
		
		if (! initialized) {
			throw new IllegalStateException("Map has not been initialized!");
		}
		
		Map<Integer, Integer> contigMap = map.get(col.getCurrentContig());
		
		value[0] = -1;
		if (contigMap != null) {
			Integer freqInt = contigMap.get(col.getCurrentPosition());
			if (freqInt != null) {
				double freq =  ((double)freqInt) / 10920.0;
				freq= Math.exp( freq * freq );
				//System.out.println("Freq at pos: " + col.getCurrentContig() + ":" + col.getCurrentPosition() + ":" + freq + "\t" + col.getBasesAsString());
				freq = (freq/Math.exp(1.0))*1.9 - 1.0;
				value[0] = 1.0;
			}
			
		}
		
		return value;
	}

}

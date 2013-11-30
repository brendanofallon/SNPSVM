package snpsvm.bamreading.variant;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Structure that holds a list of variant sites but no other information, useful for 
 * holding big lists of 'known' variants
 * @author brendan
 *
 */
public class VariantPositionList {

	private Set<Integer> knownSites = null;
	private File sourceFile = null;
	private String currentContig = null;
	
	public VariantPositionList(File vcfFile) {
		sourceFile = vcfFile;
	}
	
	public String getCurrentContig() {
		return currentContig;
	}
	
	/**
	 * True if this list contains a variant at the current contig and at the given position
	 * @param pos
	 * @return
	 */
	public boolean hasSNP(int pos) {
		return knownSites.contains(pos);
	}
	
	public void loadContig(String contig) throws IOException {
		if (contig.equals(currentContig)) {
			return;
		}
		
		knownSites = new HashSet<Integer>();
		currentContig = contig;
		
		BufferedReader reader = new BufferedReader(new FileReader(sourceFile));
		String line = reader.readLine();
		
		while(line != null && line.startsWith("#")) {
			line = reader.readLine();
		}
		
		while(line != null) {
			
			int index = line.indexOf("\t");
			if (index > 0) {
				String chr = line.substring(0, index).trim();
				if (chr.equals(contig)) {
					String[] toks = line.split("\t");
					if (toks.length < 4) {
						System.err.println("Could not parse info from vcf for line : " + line);
						continue;
					}
					
					int pos = Integer.parseInt(toks[1]);
					String ref = toks[3];
					String alt = toks[4];
					if (ref.length()==1 && (!ref.equals("-") && alt.length()==1 && (!alt.equals("-")))) {
						knownSites.add(pos);
					}
				}
			}
			line = reader.readLine();
		}
		
		reader.close();
	}
}

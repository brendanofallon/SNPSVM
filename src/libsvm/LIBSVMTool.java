package libsvm;

import java.io.IOException;

import snpsvm.app.CommandLineApp;

public class LIBSVMTool {

	public static boolean DEBUG = false;
	
	protected String libsvmPath = null;
	protected boolean initialized = false;
	
	protected void initialize() {
		String libsvmHome = CommandLineApp.configModule.getProperty("libsvm");
		if (libsvmHome == null) {
			System.err.println("LIBSVM tool can't run because it can't find libsvm. Please set the path to \"libsvm\" using config");
		}
		
		libsvmPath = libsvmHome;
		initialized = true;
	}

	protected void executeCommand(String command) {
		ProcessBuilder procBuilder = new ProcessBuilder("bash", "-c", command);
		
		
		//procBuilder.r
		if (DEBUG)
			System.out.println("LIBSVM tool executing command : " + command);
		
		try {
			Process proc = procBuilder.start();
			int exitVal = proc.waitFor();
			if (exitVal != 0) {
				System.err.println("Warning: libsvm process executing command " + command + " reported an error");
			}
		}
		catch (InterruptedException ex) {
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

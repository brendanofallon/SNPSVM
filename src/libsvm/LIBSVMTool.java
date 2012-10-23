package libsvm;

import java.io.IOException;

import snpsvm.app.CommandLineApp;

public class LIBSVMTool {

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
		//System.out.println("Executing command : " + command);
		
		try {
			Process proc = procBuilder.start();
			int exitVal = proc.waitFor();
		}
		catch (InterruptedException ex) {
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}

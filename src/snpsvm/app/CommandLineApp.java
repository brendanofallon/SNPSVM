
package snpsvm.app;

import java.io.IOException;

import util.Timer;

/**
 * Main entry point for command-line application. Mostly we just parse the first argument
 * in the arg list and look for a module recognizes the arg as its name. 
 * @author brendanofallon
 *
 */
public class CommandLineApp {
	
	
	//Stores some useful, persistent key=value pairs set by user, such as location of libsvm
	public static ConfigModule configModule = new ConfigModule();
	
	public static void main(String[] args) throws IOException {

		if (args.length == 0 || args[0].equals("help")) {
			System.out.println("\n \tSNP-SVM v. 0.01");
			System.out.println("\tBrendan O'Fallon, ARUP Labs, Salt Lake City, Utah");
			System.out.println("\tbrendan.d.ofallon@aruplab.com");
			
			if (configModule.getProperty("libsvm") == null) {
				System.out.println("\n  To begin, you must install libsvm. It's freely available from : http://www.csie.ntu.edu.tw/~cjlin/libsvm/");
				System.out.println("  Once you have downloaded and installed libsvm, tell SNPSVM where to find it, like this: ");
				System.out.println("  java snpsvm.jar config -add libsvm=/path/to/libsvm/ ");
			}
			else {
				System.out.println("\n  To begin, enter the name of a module, for instance 'buildmodel' to train a new model or 'predict' to call snps.");
			}
			return;
		}
		
		Timer mainTimer = new Timer("main");
		mainTimer.start();
		
		if (args.length > 0) {
			ModuleList modules = new ModuleList();
			Module mod = modules.getModuleForName(args[0]);
			if (mod == null) {
				System.err.println("Could not find a module with name: " + args[0]);
				return;
			}
			System.err.println("Loading module " + mod.getClass().toString().replace(".class", ""));
			
			ArgParser argParser = new ArgParser(args);
			if ((args.length==2 && args[1].equals("help"))) {
				mod.emitUsage();
			}
			else {
				mod.performOperation(args[0], argParser);	
			}
			
			mainTimer.stop();
			System.err.println("\n Elapsed time :  " + mainTimer.getTotalTimeSeconds() + " seconds");
			
			return;
		}
		
		mainTimer.stop();
		System.err.println("Elapsed time :  " + mainTimer.getTotalTimeSeconds() + " seconds");
	}
}

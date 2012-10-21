package snpsvm.app;

import java.io.IOException;
import util.Timer;

public class CommandLineApp {
	
	
	//Stores some useful, persistent key=value pairs set by user, such as location of libsvm
	public static ConfigModule configModule = new ConfigModule();
	
	public static void main(String[] args) throws IOException {

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
			if (args.length==1 || (args.length==2 && args[1].equals("help"))) {
				mod.emitUsage();
			}
			else {
				mod.performOperation(args[0], argParser);	
			}
			
			mainTimer.stop();
			System.err.println("Elapsed time :  " + mainTimer.getTotalTimeSeconds() + " seconds");
			
			return;
		}
		
		mainTimer.stop();
		System.err.println("Elapsed time :  " + mainTimer.getTotalTimeSeconds() + " seconds");
	}
}

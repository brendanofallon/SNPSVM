package snpsvm.app;

public abstract class AbstractModule implements Module {

	
	public String getRequiredStringArg(ArgParser args, String arg, String errorMessage) {
		if (args.hasOption(arg)) {
			return args.getStringArg(arg);
		}
		else {
			System.err.println(errorMessage);
			return null;
		}
	}
	
	/**
	 * Return the given argument if it was given, otherwise return null and emit no message
	 * @param args
	 * @param arg
	 * @return
	 */
	public String getOptionalStringArg(ArgParser args, String arg) {
		if (args.hasOption(arg)) {
			return args.getStringArg(arg);
		}
		else {
			return null;
		}
	}
	
}

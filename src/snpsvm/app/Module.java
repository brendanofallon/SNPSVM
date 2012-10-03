package snpsvm.app;

/**
 * Interface for primary 'modules', such as model building and new data classification
 * Each module must be able to recognize a name (such as "buildmodel") or ("train")
 * @author brendan
 *
 */
public interface Module {

	/**
	 * Write a helpful usage string to stdout
	 */
	public void emitUsage();
	
	/**
	 * Check to see if the given String matches this modules name / id
	 * @param name
	 * @return
	 */
	public boolean matchesModuleName(String name);
	
	/**
	 * Actually perform the operation in this modules
	 * @param name
	 * @param args
	 */
	public void performOperation(String name, ArgParser args);
	
}

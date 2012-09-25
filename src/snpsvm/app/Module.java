package snpsvm.app;

/**
 * Interface for primary 'modules', such as model building and new data classification
 * Each module must be able to recognize a name (such as "buildmodel") or ("train")
 * @author brendan
 *
 */
public interface Module {

	public boolean matchesModuleName(String name);
	
	public void performOperation(String name, ArgParser args);
	
}

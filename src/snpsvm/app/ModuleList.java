package snpsvm.app;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a list of known Modules and hands out references to Modules based on name 
 * @author brendan
 *
 */
public class ModuleList {

	List<Module> modules;
	
	public ModuleList() {
		constructList();
	}
	
	/**
	 * Obtain the module referenced by the given name, or null if there is no such module
	 * @param name
	 * @return
	 */
	public Module getModuleForName(String name) {
		for(Module mod : modules) {
			if (mod.matchesModuleName(name))
				return mod;
		}
		return null;
	}

	private void constructList() {
		modules = new ArrayList<Module>();
		modules.add(new ModelBuilder());
		modules.add(new Predictor());
		
	}
	
}

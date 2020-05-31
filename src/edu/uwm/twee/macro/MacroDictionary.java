package edu.uwm.twee.macro;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

import org.eclipse.core.runtime.ICoreRunnable;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import edu.uwm.twee.Activator;
import edu.uwm.twee.preferences.PreferenceConstants;
import edu.uwm.util.JSONReader;
import edu.uwm.util.Resource;

public class MacroDictionary {
	private static MacroDictionary instance;
	
	public static MacroDictionary getInstance() {
		if (instance == null) {
			instance = new MacroDictionary();
			instance.initialize();
			Activator.getDefault().getPreferenceStore().addPropertyChangeListener(new IPropertyChangeListener() {
				@Override
				public void propertyChange(PropertyChangeEvent event) {
					if (event.getProperty().equals(PreferenceConstants.P_MACROPATH)) {
						Job reInit = Job.create("reinitialize macro definitions", (ICoreRunnable) (ip) -> instance.initialize());
						reInit.setPriority(Job.BUILD);
						reInit.schedule();
					}
				}				
			});
		}
		return instance;
	}

	private void initialize() {
		System.out.println("Initializing macro definitions.");
		Map<String,MacroSyntax> tmp = new HashMap<>();
		InputStream builtin = Resource.getStream("macros.json");
		if (builtin == null) {
			showError("Cannot open macro definition file: macros.json");
		} else {
			addMacroDefinitions(builtin, tmp);
		}
		String macroPath = Activator.getDefault().getPreferenceStore().getString(PreferenceConstants.P_MACROPATH);
		if (macroPath != null && !macroPath.isEmpty()) {
			try (FileInputStream fis = new FileInputStream(macroPath)) {
				addMacroDefinitions(fis, tmp);
			} catch (IOException e) {
				showError("trying to read " + macroPath + ": " + e.getMessage());
			}
		}
		synchronized (table) {
			table.clear();
			table.putAll(tmp);
		}
	}
	
	private final Map<String,MacroSyntax> table = new HashMap<>();
	
	private MacroDictionary() { }

	private static void addMacroDefinitions(InputStream s, Map<String, MacroSyntax> map) {
		Map<?,?> object;
		try (JSONReader r = new JSONReader(new InputStreamReader(s))) {
			Object dict = r.next();
			if (dict instanceof Throwable) {
				Throwable ex = (Throwable)dict;
				showError(ex);
				return;
			}
			if (dict instanceof Map<?,?>) object = ((Map<?,?>)dict);
			else return;
		} catch (IOException|NoSuchElementException e) {
			showError(e);
			return;
		}
		for (Map.Entry<?,?> ent : object.entrySet()) {
			if (ent.getKey() instanceof String) { // always true for JSON objects
				String key = (String)ent.getKey();
				try {
					MacroSyntax ms = MacroSyntax.fromJSON(ent.getValue());
					map.put(key, ms);
				} catch (IllegalArgumentException e) {
					showError("Description for macro " + key + " broken: " + e.getMessage());
					return;
				}
			}
		}
	}

	protected static void showError(Throwable ex) {
		String message = ex.getLocalizedMessage();
		showError(message);
	}

	protected static void showError(String message) {
		PlatformUI.getWorkbench().getDisplay().asyncExec(new Runnable() {
		    public void run() {
		        Shell activeShell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
				MessageDialog.openError(activeShell, "Error reading SugarCube macro definition file", message);
		    }
		});
	}
	
	/**
	 * Check a macro call and return a non-null string
	 * if there's a problem.
	 * @param macroCall the call after the "<<"...">>" is stripped off 
	 * and the result is trimmed.
	 * @return null if OK, non-null for error.
	 */
	public String check(String macroCall) {
		if (macroCall.isEmpty()) {
			return "<<>> not allowed";
		}
		int ch = macroCall.charAt(0);
		if (ch == '/') {
			String name = macroCall.substring(1);
			MacroSyntax ms = table.get(name);
			if (ms == null) return "no macro <<" + name + ">>";
			if (!ms.needsEndTag()) return "<<" + name + ">> does not use end tag";
			return null;
		}
		int n = macroCall.length();
		int nameEnd = 1;
		if (Character.isJavaIdentifierStart(ch)) {
			while (nameEnd < n && Character.isJavaIdentifierPart(macroCall.charAt(nameEnd))) {
				++nameEnd;
			}
		}
		String name = macroCall.substring(0,nameEnd);
		MacroSyntax ms = table.get(name);
		if (ms == null) return "no macro <<" + name + ">> defined";
		StringBuilder sb = new StringBuilder();
		ms.parseArguments(macroCall.substring(nameEnd), (s) -> { if(sb.length() > 0) sb.append("; "); sb.append(s);});
		if (sb.length() > 0) {
			return sb.toString();
		}
		return null;
	}
}

package ij.plugin;

import ij.*;
import ij.gui.*;

/** Edit › Options › Studio Theme... : choose Auto / Dark / Light. */
public class StudioThemeOptions implements PlugIn {
	public void run(String arg) {
		java.util.prefs.Preferences p = java.util.prefs.Preferences.userRoot().node("aster");
		String current = p.get(StudioTheme.PREF_KEY, "auto");
		String[] choices = {"Auto (follow macOS)", "Dark", "Light"};
		String[] values = {"auto", "dark", "light"};
		int idx = 0;
		for (int i=0; i<values.length; i++) if (values[i].equals(current)) idx = i;
		GenericDialog gd = new GenericDialog("Aster Theme");
		gd.addChoice("Appearance:", choices, choices[idx]);
		gd.addMessage("The new theme is applied the next time\nAster starts.");
		gd.showDialog();
		if (gd.wasCanceled()) return;
		String v = values[gd.getNextChoiceIndex()];
		p.put(StudioTheme.PREF_KEY, v);
		try { p.flush(); } catch (Exception e) {}
		IJ.showStatus("Studio theme set to "+v+" (restart to apply)");
	}
}

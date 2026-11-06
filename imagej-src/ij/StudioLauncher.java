package ij;

import java.io.File;

/** Entry point of the Aster macOS app. Prepares a writable user folder
 *  for plugins/macros, matches the native appearance to the Studio theme and
 *  then starts the standard ImageJ application. */
public class StudioLauncher {

	public static void main(String[] args) {
		boolean dark = ij.gui.StudioTheme.isDark();
		// ImageJ's AWT dialogs (GenericDialog, MessageDialog, ...) set light backgrounds;
		// under DarkAqua their labels turn white and become unreadable, so native
		// widgets always use the light appearance. Aster's own surfaces follow the theme.
		System.setProperty("apple.awt.application.appearance", "NSAppearanceNameAqua");
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		System.setProperty("apple.awt.application.name", "Aster");

		String home = System.getProperty("user.home");
		File base = new File(home, "Library/Application Support/Aster");
		if (!System.getProperty("os.name","").toLowerCase().startsWith("mac"))
			base = new File(home, ".aster");
		File plugins = new File(base, "plugins");
		File macros = new File(base, "macros");
		plugins.mkdirs();
		macros.mkdirs();
		if (System.getProperty("plugins.dir")==null)
			System.setProperty("plugins.dir", plugins.getAbsolutePath());
		if (System.getProperty("user.dir")==null || "/".equals(System.getProperty("user.dir")))
			System.setProperty("user.dir", base.getAbsolutePath());

		ImageJ.main(args);
	}
}

package ij.gui;

import java.awt.*;
import java.io.*;

/** Aster design tokens: a modern light/dark palette shared by the main
 *  window, toolbar, status bar, image windows and the command palette. */
public class StudioTheme {

	public static final String PREF_KEY = "studio.theme"; // auto | dark | light

	private static boolean dark = resolveDark();

	public static Color bg, surface, hover, border, text, muted, accent, accentSoft, accentText, canvasBg;

	static { apply(); }

	/** True if the dark palette is active. */
	public static boolean isDark() { return dark; }

	public static void setDark(boolean d) { dark = d; apply(); }

	private static void apply() {
		if (dark) {
			bg = new Color(28, 29, 34);
			surface = new Color(38, 40, 47);
			hover = new Color(50, 53, 62);
			border = new Color(58, 61, 70);
			text = new Color(232, 233, 238);
			muted = new Color(150, 154, 166);
			accent = new Color(118, 146, 255);
			accentSoft = new Color(56, 66, 112);
			accentText = Color.white;
			canvasBg = new Color(22, 23, 27);
		} else {
			bg = new Color(246, 247, 249);
			surface = Color.white;
			hover = new Color(232, 234, 240);
			border = new Color(218, 221, 228);
			text = new Color(30, 34, 41);
			muted = new Color(107, 114, 128);
			accent = new Color(74, 108, 247);
			accentSoft = new Color(222, 229, 254);
			accentText = Color.white;
			canvasBg = new Color(236, 238, 242);
		}
		try { ij.ImageJ.backgroundColor = bg; } catch (Throwable t) {}
	}

	private static boolean resolveDark() {
		String mode = System.getProperty("studio.theme");
		if (mode==null) {
			try {
				mode = java.util.prefs.Preferences.userRoot().node("aster").get(PREF_KEY, "auto");
			} catch (Throwable t) { mode = "auto"; }
		}
		if ("dark".equals(mode)) return true;
		if ("light".equals(mode)) return false;
		return systemIsDark();
	}

	/** Reads the macOS appearance setting. */
	public static boolean systemIsDark() {
		String os = System.getProperty("os.name", "").toLowerCase();
		if (!os.startsWith("mac")) return false;
		try {
			Process p = new ProcessBuilder("defaults", "read", "-g", "AppleInterfaceStyle").redirectErrorStream(true).start();
			BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream()));
			String line = r.readLine();
			p.waitFor();
			return line!=null && line.trim().equalsIgnoreCase("Dark");
		} catch (Throwable t) {
			return false;
		}
	}

	private static String uiFamily;

	/** Modern UI font (SF Pro on macOS, falls back to Helvetica Neue / SansSerif). */
	public static Font uiFont(int style, float size) {
		if (uiFamily==null) {
			uiFamily = "SansSerif";
			try {
				String[] names = GraphicsEnvironment.getLocalGraphicsEnvironment().getAvailableFontFamilyNames();
				String[] wanted = {"SF Pro Text", "SF Pro", ".AppleSystemUIFont", "Helvetica Neue", "Inter", "Segoe UI"};
				outer:
				for (String w : wanted)
					for (String n : names)
						if (n.equals(w)) { uiFamily = w; break outer; }
			} catch (Throwable t) {}
		}
		return new Font(uiFamily, style, 1).deriveFont(style, size);
	}

	public static Color withAlpha(Color c, int alpha) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
	}

	/** Maps legacy (dark) icon colours so they stay legible on the dark toolbar. */
	public static Color adaptIconColor(Color c) {
		if (!dark || c==null) return c;
		int r=c.getRed(), g=c.getGreen(), b=c.getBlue();
		int max = Math.max(r, Math.max(g, b));
		double lum = (0.299*r + 0.587*g + 0.114*b)/255.0;
		if (max<110) return text;
		if (lum<0.45) return new Color((r+255)/2, (g+255)/2, (b+255)/2);
		return c;
	}

	public static void enableAA(Graphics g) {
		if (g instanceof Graphics2D) {
			Graphics2D g2 = (Graphics2D)g;
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			g2.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON);
		}
	}
}

package ij.gui;

import ij.IJ;
import java.awt.*;
import java.awt.event.*;

/** Rounded "Search commands ⌘L" button shown in the Aster status bar. */
public class StudioSearchPill extends Canvas implements MouseListener {
	private boolean hover;
	private final double scale = ij.Prefs.getGuiScale();

	public StudioSearchPill() {
		setBackground(StudioTheme.bg);
		addMouseListener(this);
		setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	public Dimension getPreferredSize() {
		return new Dimension((int)(184*scale), (int)(28*scale));
	}

	public void update(Graphics g) { paint(g); }

	public void paint(Graphics g0) {
		Dimension d = getSize();
		Image buf = createImage(Math.max(1,d.width), Math.max(1,d.height));
		Graphics2D g = (Graphics2D)buf.getGraphics();
		StudioTheme.enableAA(g);
		g.setColor(StudioTheme.bg);
		g.fillRect(0, 0, d.width, d.height);
		int h = (int)(20*scale);
		int y = (d.height-h)/2;
		int w = d.width-(int)(8*scale);
		g.setColor(hover?StudioTheme.hover:StudioTheme.surface);
		g.fillRoundRect(0, y, w, h, h, h);
		g.setColor(hover?StudioTheme.withAlpha(StudioTheme.accent,160):StudioTheme.border);
		g.drawRoundRect(0, y, w-1, h-1, h, h);
		// magnifier glyph
		float s = (float)scale;
		g.setStroke(new BasicStroke(1.5f*s, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
		g.setColor(StudioTheme.muted);
		int cx = (int)(9*s), cy = y+h/2-(int)(1*s), r=(int)(4*s);
		g.drawOval(cx, cy-r, 2*r, 2*r);
		g.drawLine(cx+2*r-(int)(1*s), cy+r-(int)(1*s), cx+2*r+(int)(2*s), cy+r+(int)(2*s));
		g.setFont(StudioTheme.uiFont(Font.PLAIN, 12f*s));
		FontMetrics fm = g.getFontMetrics();
		int ty = y + (h+fm.getAscent()-fm.getDescent())/2;
		g.drawString("Search commands", (int)(24*s), ty);
		String key = IJ.isMacOSX()?"⌘L":"Ctrl+L";
		g.setFont(StudioTheme.uiFont(Font.PLAIN, 11f*s));
		fm = g.getFontMetrics();
		int kw = fm.stringWidth(key)+(int)(10*s);
		int kx = w-kw-(int)(4*s);
		int kh = h-(int)(6*s);
		g.setColor(StudioTheme.bg);
		g.fillRoundRect(kx, y+(int)(3*s), kw, kh, (int)(8*s), (int)(8*s));
		g.setColor(StudioTheme.muted);
		g.drawString(key, kx+(int)(5*s), y + (h+fm.getAscent()-fm.getDescent())/2);
		g.dispose();
		g0.drawImage(buf, 0, 0, null);
	}

	public void mouseClicked(MouseEvent e) {}
	public void mousePressed(MouseEvent e) {
		IJ.doCommand("Find Commands...");
	}
	public void mouseReleased(MouseEvent e) {}
	public void mouseEntered(MouseEvent e) { hover=true; repaint(); }
	public void mouseExited(MouseEvent e) { hover=false; repaint(); }
}

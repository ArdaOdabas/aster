package ij.plugin;

import ij.*;
import ij.gui.StudioTheme;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.io.File;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.*;

/** Aster command palette (⌘L): a fast, fuzzy, keyboard-driven
 *  launcher for every ImageJ menu command, plugin and macro. */
public class CommandPalette implements PlugIn {

	private static final String RECENT_KEY = "studio.recent";
	private static JDialog openDialog;

	static class Cmd {
		String name, path; int score;
		Cmd(String n, String p) { name=n; path=p; }
	}

	private List<Cmd> all;
	private DefaultListModel<Cmd> model = new DefaultListModel<Cmd>();
	private JList<Cmd> list;
	private JTextField field;
	private JLabel countLabel;
	private JDialog dialog;

	public void run(String arg) {
		if (openDialog!=null && openDialog.isVisible()) {
			openDialog.toFront();
			return;
		}
		all = collect();
		SwingUtilities.invokeLater(new Runnable() { public void run() { show(); }});
	}

	// ---- data -------------------------------------------------------------

	private List<Cmd> collect() {
		LinkedHashMap<String,Cmd> map = new LinkedHashMap<String,Cmd>();
		MenuBar mb = Menus.getMenuBar();
		if (mb!=null) {
			for (int i=0; i<mb.getMenuCount(); i++) {
				Menu m = mb.getMenu(i);
				if (m!=null && !"Window".equals(m.getLabel())) walk(m, m.getLabel(), map);
				else if (m!=null) { // keep Window commands, skip open-window entries
					for (int j=0; j<m.getItemCount(); j++) {
						MenuItem it = m.getItem(j);
						if (it==null || it instanceof Menu) continue;
						String l = it.getLabel();
						if (l!=null && Menus.getCommands().get(l)!=null && !map.containsKey(l)) map.put(l, new Cmd(l, "Window"));
					}
				}
			}
		}
		Hashtable commands = Menus.getCommands();
		if (commands!=null) {
			for (Object k : commands.keySet()) {
				String name = (String)k;
				if (name.startsWith("/") || name.indexOf(File.separatorChar)>=0) continue;
				if (!map.containsKey(name)) map.put(name, new Cmd(name, ""));
			}
		}
		return new ArrayList<Cmd>(map.values());
	}

	private void walk(Menu menu, String path, Map<String,Cmd> map) {
		for (int i=0; i<menu.getItemCount(); i++) {
			MenuItem item = menu.getItem(i);
			if (item==null) continue;
			String label = item.getLabel();
			if (label==null || label.equals("-") || label.trim().length()==0) continue;
			if (label.startsWith("/") || label.startsWith("~") || label.indexOf(File.separatorChar)>=0 && !(item instanceof Menu)) continue;
			if (item instanceof Menu && label.equals("Open Recent")) continue;
			if (item instanceof Menu)
				walk((Menu)item, path+"  ›  "+label, map);
			else if (!map.containsKey(label))
				map.put(label, new Cmd(label, path));
		}
	}

	private List<String> recents() {
		String s = Prefs.get(RECENT_KEY, "");
		List<String> r = new ArrayList<String>();
		for (String t : s.split("\u001F")) if (t.length()>0) r.add(t);
		return r;
	}

	private void remember(String cmd) {
		List<String> r = recents();
		r.remove(cmd);
		r.add(0, cmd);
		while (r.size()>12) r.remove(r.size()-1);
		StringBuilder sb = new StringBuilder();
		for (String t : r) { if (sb.length()>0) sb.append('\u001F'); sb.append(t); }
		Prefs.set(RECENT_KEY, sb.toString());
	}

	private static int score(String text, String q) {
		String t = text.toLowerCase(Locale.US);
		if (t.equals(q)) return 1000;
		if (t.startsWith(q)) return 800 - t.length();
		int idx = t.indexOf(q);
		if (idx>0) {
			char before = t.charAt(idx-1);
			if (!Character.isLetterOrDigit(before)) return 650 - idx;
			return 450 - idx;
		}
		// fuzzy subsequence, rewarding word starts
		int ti=0, gaps=0, bonus=0;
		for (int qi=0; qi<q.length(); qi++) {
			char c = q.charAt(qi);
			int found = -1;
			for (int k=ti; k<t.length(); k++) if (t.charAt(k)==c) { found=k; break; }
			if (found<0) return -1;
			if (found==0 || !Character.isLetterOrDigit(t.charAt(found-1))) bonus += 12;
			gaps += found-ti;
			ti = found+1;
		}
		return Math.max(1, 200 + bonus - gaps*3);
	}

	private void filter() {
		String q = field.getText().trim().toLowerCase(Locale.US);
		List<String> rec = recents();
		List<Cmd> res = new ArrayList<Cmd>();
		if (q.length()==0) {
			for (String r : rec) for (Cmd c : all) if (c.name.equals(r)) { c.score=0; res.add(c); break; }
			List<Cmd> rest = new ArrayList<Cmd>();
			for (Cmd c : all) if (!rec.contains(c.name)) rest.add(c);
			Collections.sort(rest, new Comparator<Cmd>() {
				public int compare(Cmd a, Cmd b) { return a.name.compareToIgnoreCase(b.name); }});
			res.addAll(rest);
		} else {
			String[] tokens = q.split("\\s+");
			for (Cmd c : all) {
				int total = 0; boolean ok = true;
				for (String tok : tokens) {
					int s = score(c.name, tok);
					if (s<0) {
						int ps = c.path.toLowerCase(Locale.US).contains(tok) ? 120 : -1;
						if (ps<0) { ok=false; break; }
						s = ps;
					}
					total += s;
				}
				if (!ok) continue;
				if (tokens.length==1) { int whole = score(c.name, q); if (whole>0) total = Math.max(total, whole); }
				if (rec.contains(c.name)) total += 120 - rec.indexOf(c.name)*5;
				c.score = total;
				res.add(c);
			}
			Collections.sort(res, new Comparator<Cmd>() {
				public int compare(Cmd a, Cmd b) {
					if (b.score!=a.score) return b.score-a.score;
					return a.name.compareToIgnoreCase(b.name);
				}});
		}
		model.clear();
		for (Cmd c : res) model.addElement(c);
		if (model.size()>0) { list.setSelectedIndex(0); list.ensureIndexIsVisible(0); }
		countLabel.setText(model.size()+" command"+(model.size()==1?"":"s"));
	}

	// ---- UI ---------------------------------------------------------------

	private void show() {
		Frame owner = IJ.getInstance();
		dialog = new JDialog(owner, "Command Palette", false);
		openDialog = dialog;
		dialog.setUndecorated(true);
		dialog.setBackground(new Color(0,0,0,0));
		dialog.getRootPane().putClientProperty("Window.shadow", Boolean.TRUE);

		final int radius = 18;
		JPanel root = new JPanel(new BorderLayout()) {
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D)g.create();
				StudioTheme.enableAA(g2);
				g2.setColor(StudioTheme.surface);
				g2.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
				g2.setColor(StudioTheme.border);
				g2.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, radius, radius);
				g2.dispose();
			}
		};
		root.setOpaque(false);

		// search row
		JPanel top = new JPanel(new BorderLayout(10, 0)) {
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D)g.create();
				StudioTheme.enableAA(g2);
				g2.setColor(StudioTheme.border);
				g2.fillRect(0, getHeight()-1, getWidth(), 1);
				g2.dispose();
			}
		};
		top.setOpaque(false);
		top.setBorder(new EmptyBorder(14, 18, 14, 18));
		JComponent glyph = new JComponent() {
			public Dimension getPreferredSize() { return new Dimension(20, 20); }
			protected void paintComponent(Graphics g) {
				Graphics2D g2 = (Graphics2D)g.create();
				StudioTheme.enableAA(g2);
				g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
				g2.setColor(StudioTheme.accent);
				int cy = getHeight()/2;
				g2.drawOval(2, cy-8, 12, 12);
				g2.drawLine(12, cy+2, 17, cy+7);
				g2.dispose();
			}
		};
		field = new JTextField();
		field.setBorder(null);
		field.setOpaque(false);
		field.setFont(StudioTheme.uiFont(Font.PLAIN, 19f));
		field.setForeground(StudioTheme.text);
		field.setCaretColor(StudioTheme.accent);
		field.setSelectionColor(StudioTheme.accentSoft);
		field.setSelectedTextColor(StudioTheme.text);
		JLabel hint = new JLabel("ASTER");
		hint.setFont(StudioTheme.uiFont(Font.BOLD, 11f));
		hint.setForeground(StudioTheme.accent);
		top.add(glyph, BorderLayout.WEST);
		top.add(field, BorderLayout.CENTER);
		top.add(hint, BorderLayout.EAST);

		// list
		list = new JList<Cmd>(model);
		list.setOpaque(false);
		list.setBackground(StudioTheme.surface);
		list.setFixedCellHeight(44);
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setCellRenderer(new Renderer());
		list.setFocusable(false);
		list.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				int i = list.locationToIndex(e.getPoint());
				if (i>=0) { list.setSelectedIndex(i); if (e.getClickCount()>=1) runSelected(); }
			}
		});
		list.addMouseMotionListener(new MouseMotionAdapter() {
			public void mouseMoved(MouseEvent e) {
				int i = list.locationToIndex(e.getPoint());
				if (i>=0 && i!=list.getSelectedIndex()) list.setSelectedIndex(i);
			}
		});
		JScrollPane sp = new JScrollPane(list);
		sp.setBorder(new EmptyBorder(6, 8, 6, 8));
		sp.setOpaque(false);
		sp.getViewport().setOpaque(false);
		sp.getVerticalScrollBar().setUnitIncrement(22);

		// footer
		JPanel footer = new JPanel(new BorderLayout()) {
			protected void paintComponent(Graphics g) {
				g.setColor(StudioTheme.border);
				g.fillRect(0, 0, getWidth(), 1);
			}
		};
		footer.setOpaque(false);
		footer.setBorder(new EmptyBorder(9, 18, 10, 18));
		JLabel keys = new JLabel("↑↓  navigate      ↵  run      esc  close");
		keys.setFont(StudioTheme.uiFont(Font.PLAIN, 11.5f));
		keys.setForeground(StudioTheme.muted);
		countLabel = new JLabel();
		countLabel.setFont(StudioTheme.uiFont(Font.PLAIN, 11.5f));
		countLabel.setForeground(StudioTheme.muted);
		footer.add(keys, BorderLayout.WEST);
		footer.add(countLabel, BorderLayout.EAST);

		root.add(top, BorderLayout.NORTH);
		root.add(sp, BorderLayout.CENTER);
		root.add(footer, BorderLayout.SOUTH);
		dialog.setContentPane(root);

		field.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { filter(); }
			public void removeUpdate(DocumentEvent e) { filter(); }
			public void changedUpdate(DocumentEvent e) { filter(); }
		});
		field.addKeyListener(new KeyAdapter() {
			public void keyPressed(KeyEvent e) {
				int n = model.size(), i = list.getSelectedIndex();
				switch (e.getKeyCode()) {
					case KeyEvent.VK_DOWN: move(Math.min(n-1, i+1)); e.consume(); break;
					case KeyEvent.VK_UP: move(Math.max(0, i-1)); e.consume(); break;
					case KeyEvent.VK_PAGE_DOWN: move(Math.min(n-1, i+8)); e.consume(); break;
					case KeyEvent.VK_PAGE_UP: move(Math.max(0, i-8)); e.consume(); break;
					case KeyEvent.VK_ENTER: runSelected(); e.consume(); break;
					case KeyEvent.VK_ESCAPE: close(); e.consume(); break;
				}
			}
		});
		dialog.addWindowListener(new WindowAdapter() {
			public void windowDeactivated(WindowEvent e) { close(); }
		});

		filter();
		Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int w = 660, h = 460;
		dialog.setSize(w, h);
		dialog.setLocation(screen.x+(screen.width-w)/2, screen.y+screen.height/5);
		dialog.setVisible(true);
		field.requestFocusInWindow();
	}

	private void move(int i) {
		if (i<0) return;
		list.setSelectedIndex(i);
		list.ensureIndexIsVisible(i);
	}

	private void close() {
		if (dialog!=null) { dialog.dispose(); openDialog = null; }
	}

	private void runSelected() {
		Cmd c = list.getSelectedValue();
		if (c==null) return;
		remember(c.name);
		close();
		IJ.doCommand(c.name);
	}

	class Renderer extends JComponent implements ListCellRenderer<Cmd> {
		Cmd cmd; boolean selected;
		public Component getListCellRendererComponent(JList<? extends Cmd> l, Cmd value, int index, boolean isSelected, boolean focus) {
			cmd = value; selected = isSelected;
			return this;
		}
		protected void paintComponent(Graphics g) {
			Graphics2D g2 = (Graphics2D)g.create();
			StudioTheme.enableAA(g2);
			int w = getWidth(), h = getHeight();
			if (selected) {
				g2.setColor(StudioTheme.accentSoft);
				g2.fillRoundRect(2, 2, w-4, h-4, 12, 12);
				g2.setColor(StudioTheme.accent);
				g2.fillRoundRect(6, h/2-9, 3, 18, 3, 3);
			}
			String name = cmd.name.endsWith("...")?cmd.name.substring(0, cmd.name.length()-3):cmd.name;
			g2.setFont(StudioTheme.uiFont(Font.PLAIN, 14f));
			FontMetrics fm = g2.getFontMetrics();
			int ty = (h+fm.getAscent()-fm.getDescent())/2;
			g2.setColor(selected && !StudioTheme.isDark()?StudioTheme.accent.darker():StudioTheme.text);
			g2.drawString(name, 18, ty);
			if (cmd.name.endsWith("...")) {
				g2.setColor(StudioTheme.muted);
				g2.drawString("…", 18+fm.stringWidth(name)+2, ty);
			}
			if (cmd.path.length()>0) {
				g2.setFont(StudioTheme.uiFont(Font.PLAIN, 12f));
				FontMetrics pm = g2.getFontMetrics();
				String p = cmd.path;
				int maxW = w/2;
				while (pm.stringWidth(p)>maxW && p.length()>4) p = "…"+p.substring(2);
				g2.setColor(StudioTheme.muted);
				g2.drawString(p, w-16-pm.stringWidth(p), (h+pm.getAscent()-pm.getDescent())/2);
			}
			g2.dispose();
		}
	}
}

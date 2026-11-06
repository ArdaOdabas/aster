import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Draws the Aster app icon (1024x1024 PNG): a macOS-style squircle
 *  with a microscope "lens" looking at a fluorescent cell. */
public class IconGen {
	public static void main(String[] a) throws Exception {
		int S = 1024;
		BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = img.createGraphics();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
		g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

		// squircle body (macOS grid: 824px body centred, 100px margin)
		double m = 100, b = S-2*m;
		Shape body = new RoundRectangle2D.Double(m, m, b, b, 370, 370);
		// drop shadow
		for (int i=24; i>0; i--) {
			g.setColor(new Color(0,0,0, 3));
			g.fill(new RoundRectangle2D.Double(m-i*0.5, m+12-i*0.2+i*0.6, b+i, b+i*0.6, 370+i, 370+i));
		}
		g.setPaint(new GradientPaint(0, (float)m, new Color(38, 42, 74), 0, (float)(m+b), new Color(14, 15, 28)));
		g.fill(body);
		g.setClip(body);
		// ambient glow
		g.setPaint(new RadialGradientPaint(new Point2D.Double(S*0.5, S*0.48), 430f,
			new float[]{0f, 1f}, new Color[]{new Color(110, 120, 255, 90), new Color(110,120,255,0)}));
		g.fill(body);

		double cx = S*0.5, cy = S*0.5;
		// lens ring
		double R = 300;
		g.setStroke(new BasicStroke(34f));
		g.setPaint(new GradientPaint((float)(cx-R), (float)(cy-R), new Color(160, 178, 255), (float)(cx+R), (float)(cy+R), new Color(88, 92, 210)));
		g.draw(new Ellipse2D.Double(cx-R, cy-R, 2*R, 2*R));
		// lens field
		Shape field = new Ellipse2D.Double(cx-R+17, cy-R+17, 2*R-34, 2*R-34);
		g.setPaint(new RadialGradientPaint(new Point2D.Double(cx, cy), (float)R,
			new float[]{0f, 1f}, new Color[]{new Color(18, 26, 52), new Color(8, 10, 22)}));
		g.fill(field);
		Shape oldClip = g.getClip();
		g.clip(field);

		// small background cells
		drawCell(g, cx-170, cy+150, 70, new Color(255, 92, 170), 0.55f);
		drawCell(g, cx+185, cy-150, 55, new Color(80, 220, 255), 0.5f);
		drawCell(g, cx+170, cy+175, 45, new Color(140, 255, 160), 0.4f);
		// hero cell
		Path2D cell = new Path2D.Double();
		int n = 64;
		for (int i=0; i<=n; i++) {
			double t = i*2*Math.PI/n;
			double r = 165 + 18*Math.sin(3*t+0.6) + 10*Math.cos(5*t);
			double x = cx-10 + r*Math.cos(t), y = cy-5 + r*0.86*Math.sin(t);
			if (i==0) cell.moveTo(x,y); else cell.lineTo(x,y);
		}
		cell.closePath();
		for (int i=6; i>=1; i--) {
			g.setColor(new Color(60, 255, 170, 12));
			g.setStroke(new BasicStroke(i*14f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
			g.draw(cell);
		}
		g.setPaint(new RadialGradientPaint(new Point2D.Double(cx-40, cy-40), 220f,
			new float[]{0f, 0.7f, 1f}, new Color[]{new Color(120, 255, 200, 230), new Color(30, 200, 150, 200), new Color(20, 150, 120, 220)}));
		g.fill(cell);
		g.setStroke(new BasicStroke(6f));
		g.setColor(new Color(190, 255, 225, 230));
		g.draw(cell);
		// nucleus
		Shape nuc = new Ellipse2D.Double(cx-20-70, cy-10-58, 140, 116);
		g.setPaint(new RadialGradientPaint(new Point2D.Double(cx-40, cy-30), 110f,
			new float[]{0f, 1f}, new Color[]{new Color(140, 150, 255), new Color(70, 80, 230)}));
		g.fill(nuc);
		g.setColor(new Color(200, 205, 255, 200));
		g.fill(new Ellipse2D.Double(cx-48, cy-22, 26, 22));

		// crosshair ticks
		g.setClip(oldClip);
		g.setColor(new Color(170, 185, 255, 200));
		g.setStroke(new BasicStroke(14f, BasicStroke.CAP_ROUND, BasicStroke.CAP_ROUND));
		double o = R+28, l = 46;
		g.draw(new Line2D.Double(cx, cy-o, cx, cy-o-l));
		g.draw(new Line2D.Double(cx, cy+o, cx, cy+o+l));
		g.draw(new Line2D.Double(cx-o, cy, cx-o-l, cy));
		g.draw(new Line2D.Double(cx+o, cy, cx+o+l, cy));

		// glass highlight
		g.setClip(body);
		g.setPaint(new GradientPaint(0, (float)m, new Color(255,255,255,40), 0, (float)(m+b*0.5), new Color(255,255,255,0)));
		g.fill(new Ellipse2D.Double(m-200, m-560, b+400, 900));
		g.setClip(null);
		g.setStroke(new BasicStroke(3f));
		g.setColor(new Color(255,255,255,40));
		g.draw(body);
		g.dispose();
		ImageIO.write(img, "png", new File(a[0]));
	}

	static void drawCell(Graphics2D g, double x, double y, double r, Color c, float alpha) {
		g.setPaint(new RadialGradientPaint(new Point2D.Double(x, y), (float)r,
			new float[]{0f, 1f}, new Color[]{new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha*255)), new Color(c.getRed(), c.getGreen(), c.getBlue(), 0)}));
		g.fill(new Ellipse2D.Double(x-r, y-r, 2*r, 2*r));
		g.setColor(new Color(c.getRed(), c.getGreen(), c.getBlue(), (int)(alpha*200)));
		g.fill(new Ellipse2D.Double(x-r*0.35, y-r*0.35, r*0.7, r*0.7));
	}
}

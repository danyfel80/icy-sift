package plugins.danyfel80.registration.sift.overlay;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.List;

import algorithms.features.sift.Feature;

import icy.canvas.IcyCanvas;
import icy.painter.Overlay;
import icy.sequence.Sequence;

public class SIFTFeatureOverlay extends Overlay {

	private List<Feature> features;

	private SIFTFeatureOverlay() {
		super("SIFT features");
	}

	public SIFTFeatureOverlay(List<Feature> features) {
		super("SIFT features");
		this.features = features;
	}

	@Override
	public void paint(Graphics2D g, Sequence sequence, IcyCanvas canvas) {
		if (g != null && features != null) {
			g.setColor(Color.GREEN);
			g.setStroke(new BasicStroke());

			Feature f;
			for (int i = 0; i < features.size(); i++) {
				f = features.get(i);
				g.drawOval((int) (f.location[0] - f.scale) + 1, (int) (f.location[1] - f.scale) + 1, (int) (2 * f.scale),
				    (int) (2 * f.scale));
				int x2 = (int) (f.location[0] + (f.scale * Math.cos(f.orientation)));
				int y2 = (int) (f.location[1] + (f.scale * Math.sin(f.orientation)));
				g.drawLine((int) f.location[0], (int) f.location[1], x2, y2);
			}
		}
	}
}

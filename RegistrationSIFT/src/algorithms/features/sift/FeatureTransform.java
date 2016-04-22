package algorithms.features.sift;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import algorithms.models.Point;
import algorithms.models.PointMatch;

import icy.sequence.Sequence;

/**
 * @author Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * @version 0.4b
 * @modified by Hoai Thu NGUYEN for RegistrationSIFT plugin in Icy
 */
abstract public class FeatureTransform<T extends FloatArray2DFeatureTransform<?>> {
	final protected T t;

	/**
	 * Constructor
	 *
	 * @param t
	 *          feature transformation
	 */
	public FeatureTransform(final T t) {
		this.t = t;
	}

	/**
	 * Extract features from a Sequence
	 *
	 * @param seq
	 * @param features
	 *          collects all features
	 *
	 * @return number of detected features
	 */
	public void extractFeatures(final Sequence seq, final Collection<Feature> features) {
		final FloatArray2D fa = ImageArrayConverter.SequenceToFloatArray2DNormalize(seq);

		t.init(fa);
	}

	final public Collection<Feature> extractFeatures(final Sequence seq) {
		final Collection<Feature> features = new ArrayList<Feature>();
		extractFeatures(seq, features);
		return features;
	}

	/**
	 * Identify corresponding features
	 *
	 * @param fs1
	 *          feature collection from set 1
	 * @param fs2
	 *          feature collection from set 2
	 * @param matches
	 *          collects the matching coordinates
	 * @param rod
	 *          Ratio of distances (closest/next closest match)
	 */
	static public void matchFeatures(final Collection<Feature> fs1, final Collection<Feature> fs2,
	    final List<PointMatch> matches, final float rod, final boolean spatial, final int sc) {
		for (final Feature f1 : fs1) {
			Feature best = null;
			double best_d = Double.MAX_VALUE;
			double second_best_d = Double.MAX_VALUE;
			double[] l1 = f1.location;

			for (final Feature f2 : fs2) {
				double[] l2 = f2.location;
				double dis2 = Math.pow(l1[0] - l2[0], 2) + Math.pow(l1[1] - l2[1], 2);

				if (!spatial || dis2 < Math.pow(sc, 2)) {
					final double d = f1.descriptorDistance(f2);
					if (d < best_d) {
						second_best_d = best_d;
						best_d = d;
						best = f2;
					} else if (d < second_best_d)
						second_best_d = d;
				}
			}
			if (best != null && second_best_d < Double.MAX_VALUE && best_d / second_best_d < rod)
				matches.add(new PointMatch(new Point(new double[] { f1.location[0], f1.location[1] }),
				    new Point(new double[] { best.location[0], best.location[1] })));
		}

		// now remove ambiguous matches
		for (int i = 0; i < matches.size();) {
			boolean amb = false;
			final PointMatch m = matches.get(i);
			final double[] m_p2 = m.getP2().getL();
			for (int j = i + 1; j < matches.size();) {
				final PointMatch n = matches.get(j);
				final double[] n_p2 = n.getP2().getL();
				if (m_p2[0] == n_p2[0] && m_p2[1] == n_p2[1]) {
					amb = true;
					matches.remove(j);
				} else
					++j;
			}
			if (amb)
				matches.remove(i);
			else
				++i;
		}
	}
}

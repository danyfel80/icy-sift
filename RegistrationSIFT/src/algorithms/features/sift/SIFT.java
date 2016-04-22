package algorithms.features.sift;

import java.util.Collection;

import icy.sequence.Sequence;
import icy.sequence.SequenceUtil;

/**
 * Scale Invariant Feature Transform Based on the implementation of Stephan
 * Saalfeld <saalfeld@mpi-cbg.de>
 * 
 * @author Hoai Thu NGUYEN
 * @date 16-01-2016
 */
public class SIFT extends FeatureTransform<FloatArray2DSIFT> {
	/**
	 * Constructor
	 * 
	 * @param t
	 *          Feature transform.
	 */
	public SIFT(final FloatArray2DSIFT t) {
		super(t);
	}

	/**
	 * Extract SIFT features from a Sequence
	 * 
	 * @param seq
	 *          Sequence to extract features from.
	 * @param features
	 *          The list to be filled.
	 */
	final public void extractFeatures(final Sequence seq, final Collection<Feature> features) {
		float scale = 1.0f;
		/*
		 * make sure that integer rounding does not result in an image of
		 * t.getMaxOctaveSize() + 1
		 */

		final float maxSize = t.getMaxOctaveSize() - 1;

		FloatArray2D fa;
		if (maxSize < seq.getSizeX() || maxSize < seq.getSizeY()) {
			/* scale the image respectively */
			scale = (float) Math.min(maxSize / seq.getSizeX(), maxSize / seq.getSizeY());
			Sequence seqscaled = SequenceUtil.scale(seq, Math.round(seq.getSizeX() * scale),
			    Math.round(seq.getSizeY() * scale));
			fa = ImageArrayConverter.SequenceToFloatArray2DNormalize(seqscaled);
		} else {
			fa = ImageArrayConverter.SequenceToFloatArray2DNormalize(seq);
		}

		final float[] initialKernel;

		final float initialSigma = t.getInitialSigma();
		if (initialSigma < 1.0) {
			scale *= 2.0f;
			t.setInitialSigma(initialSigma * 2);
			final FloatArray2D fat = new FloatArray2D(fa.width * 2 - 1, fa.height * 2 - 1);
			FloatArray2DScaleOctave.upsample(fa, fat);

			fa = fat;
			initialKernel = Filter.createGaussianKernel((float) Math.sqrt(t.getInitialSigma() * t.getInitialSigma() - 1.0),
			    true);
		} else
			initialKernel = Filter.createGaussianKernel((float) Math.sqrt(initialSigma * initialSigma - 0.25), true);

		fa = Filter.convolveSeparable(fa, initialKernel, initialKernel);

		t.init(fa);
		t.extractFeatures(features);
		if (scale != 1.0f) {
			for (Feature f : features) {
				f.scale /= scale;
				f.location[0] /= scale;
				f.location[1] /= scale;
			}
			t.setInitialSigma(initialSigma);
		}
	}
}

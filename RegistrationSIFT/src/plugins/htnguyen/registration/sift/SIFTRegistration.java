package plugins.htnguyen.registration.sift;

import icy.gui.dialog.MessageDialog;
import icy.sequence.Sequence;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzStoppable;
import plugins.adufour.ezplug.EzVarBoolean;
import plugins.adufour.ezplug.EzVarFloat;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.danyfel80.registration.sift.overlay.SIFTFeatureOverlay;
import plugins.kernel.roi.roi2d.ROI2DPoint;
import algorithms.features.sift.Feature;
import algorithms.features.sift.FeatureTransform;
import algorithms.features.sift.FloatArray2DSIFT;
import algorithms.features.sift.SIFT;
import algorithms.models.Point;
import algorithms.models.PointMatch;
import algorithms.models.SimilarityTransform;

/**
 * Plugin for SIFT points registration. Based on SIFT_ExtractPointRoi class
 * developed by Stephan Saalfeld <saalfeld@mpi-cbg.de>
 * 
 * @author Hoai Thu NGUYEN
 * @date 16-01-2016
 */
public class SIFTRegistration extends EzPlug implements EzStoppable {

	/**
	 * static Parameter class to minimize code.
	 * 
	 * @author Daniel Felipe Gonzalez Obando
	 */
	static private class Param {
		public final FloatArray2DSIFT.Param sift = new FloatArray2DSIFT.Param();

		/**
		 * Closest/next closest neighbor distance ratio
		 */
		public float rod = 0.77f;

		/**
		 * Spatial constraint
		 */
		public int sc = 100;

		/**
		 * Maximal allowed alignment error in px
		 */
		public float maxEpsilon = 5.0f;

		/**
		 * Minimal absolute number of inliers
		 */
		public int NumInliers = 4;

		/**
		 * Number of iteration RANSAC
		 */
		public int NumIter = 100;

	}

	// EzPlug Components
	/**
	 * Input image 1.
	 */
	private EzVarSequence inSequence1;
	/**
	 * Input image 2.
	 */
	private EzVarSequence inSequence2;

	/**
	 * Maximum resolution. The resolution of the first octave in the scale-space,
	 * set to image size to consider all the smallest details of image.
	 */
	private EzVarInteger inMaxResolution;
	/**
	 * Minimum resolution. The last octave of the scale space will have a
	 * resolution larger or equal to this value. Set to small value to consider
	 * the large forms of the image.
	 */
	private EzVarInteger inMinResolution;
	/**
	 * Feature Descriptor Size. The number of 4x4px patch considered around each
	 * keypoint.
	 */
	private EzVarInteger inFeatDescSize;
	/**
	 * Feature descriptor orientation bins. The number of orientation bins in each
	 * 4x4px patch.
	 */
	private EzVarInteger inFeatDescOBinSize;
	/**
	 * Closest/Next closest ratio. Correspondence candidates from local descriptor
	 * matching are accepted only if the Euclidean distance to the nearest
	 * neighbor is significantly smaller than that to the next nearest neighbor.
	 * Increase of there is a large deformation between 2 images.
	 */
	private EzVarFloat inClosestToNextRatio;
	/**
	 * Add spatial constraint in preliminary matching. Uncheck in case of 2 images
	 * with different sizes.
	 */
	private EzVarBoolean inIsSpatialConstraint;
	/**
	 * Spatial constraint. Correspondence candidates have to be in a certain
	 * region around the location of the keypoint considered.
	 */
	private EzVarInteger inSpatialConstraint;
	/**
	 * Maximal Alignment Error. After transformation, the distance between 2
	 * points has to be smaller than this value to be considered inlier
	 */
	private EzVarFloat inMaxAlignErr;
	/**
	 * Number of random inliers. The number of random correspondent pairs selected
	 * randomly by RANSAC in each iteration.
	 */
	private EzVarInteger inRandomInliersNum;
	/**
	 * Number of iteration RANSAC.
	 */
	private EzVarInteger inRansacIterNum;
	/**
	 * Debug boolean to show debug and intermediate information.
	 */
	private EzVarBoolean inIsDebug;

	// Input and parameters
	private Sequence seq1;
	private Sequence seq2;
	final private List<Feature> fs1 = new ArrayList<Feature>();
	final private List<Feature> fs2 = new ArrayList<Feature>();

	final static private Param p = new Param();

	private boolean stopFlag;

	@Override
	protected void initialize() {
		// GUI setup
		super.getUI().center();
		super.getUI().setPreferredSize(new Dimension(500, 600));

		inSequence1 = new EzVarSequence("Image 1");
		inSequence2 = new EzVarSequence("Image 2");

		EzGroup groupSequences = new EzGroup("Images", inSequence1, inSequence2);
		super.addEzComponent(groupSequences);

		inMaxResolution = new EzVarInteger("Maximum resolution", p.sift.maxOctaveSize, 64, 32768, 32);
		inMaxResolution.setToolTipText("Resolution of the first octave in the scale-space, "
		    + "set to image size to consider all the smallest " + "details of image");

		inMinResolution = new EzVarInteger("Minimum resolution", p.sift.minOctaveSize, 0, 128, 4);
		inMinResolution.setToolTipText(
		    "Last octave of the scale space will have a resolution larger or equal to this value. Set to small value to consider the large forms of image");

		inFeatDescSize = new EzVarInteger("Feature descriptor size", p.sift.fdSize, 4, 400, 2);
		inFeatDescSize.setToolTipText("Number of 4x4px patch considered around each keypoint");

		inFeatDescOBinSize = new EzVarInteger("Feature descriptor orientation bins", p.sift.fdBins, 8, 20, 4);
		inFeatDescOBinSize.setToolTipText("Number of orientation bins in each 4x4px patch");

		inClosestToNextRatio = new EzVarFloat("Closest/Next closest ratio", p.rod, 0.5f, 1.0f, 0.01f);
		inClosestToNextRatio.setToolTipText(
		    "Correspondence candidates from local descriptor matching are accepted only if the Euclidean distance to the nearest neighbour is significantly smaller than that to the next nearest neighbour. Increase of there is a large deformation between 2 images");

		inSpatialConstraint = new EzVarInteger("Spatial constraint", p.sc, 0, 1000, 5);
		inSpatialConstraint.setToolTipText(
		    "Correspondance candidates have to be in a certain region around the location of the keypoint considered");

		inIsSpatialConstraint = new EzVarBoolean("Add spatial constraint in prelimiary matching", true);
		inIsSpatialConstraint.setToolTipText("Uncheck in case of 2 images with different sizes");

		EzGroup groupDescriptor = new EzGroup("Feature Descriptor", inMaxResolution, inMinResolution, inFeatDescSize,
		    inFeatDescOBinSize, inClosestToNextRatio, inIsSpatialConstraint, inSpatialConstraint);
		super.addEzComponent(groupDescriptor);

		inMaxAlignErr = new EzVarFloat("Maximal Alignment Error", p.maxEpsilon, 5.0f, 100.0f, 1.0f);

		inSpatialConstraint.setToolTipText(
		    "After transformation, the distance between 2 points has to be smaller than this value to be considered inliers");

		inRandomInliersNum = new EzVarInteger("Number of random inliers", p.NumInliers, 4, 10, 1);
		inRandomInliersNum
		    .setToolTipText("Number of random correspondant pairs selected randomly by RANSAC in each iteration");

		inRansacIterNum = new EzVarInteger("Number of iteration RANSAC", p.NumIter, 100, 100000, 50);

		EzGroup groupFilter = new EzGroup("Geometric Consensus Filter", inMaxAlignErr, inRandomInliersNum, inRansacIterNum);
		super.addEzComponent(groupFilter);

		inIsDebug = new EzVarBoolean("Debug", false);
		inIsDebug.setToolTipText(
		    "Execute the plugin in debug mode. All the intermediate images will be displayed, as well as the debug log.");
		super.addEzComponent(inIsDebug);
	}

	@Override
	protected void execute() {
		seq1 = inSequence1.getValue();
		seq2 = inSequence2.getValue();

		p.sift.maxOctaveSize = inMaxResolution.getValue();
		p.sift.minOctaveSize = inMinResolution.getValue();
		p.sift.fdSize = inFeatDescSize.getValue();
		p.sift.fdBins = inFeatDescOBinSize.getValue();
		p.rod = inClosestToNextRatio.getValue();
		p.sc = inSpatialConstraint.getValue();
		p.maxEpsilon = inMaxAlignErr.getValue();
		p.NumInliers = inRandomInliersNum.getValue();
		p.NumIter = inRansacIterNum.getValue();

		// Verify if two images are selected
		if (seq1 == null || seq2 == null) {
			MessageDialog.showDialog("You must select 2 images to register!", 0);
			return;
		}

		// Cleanup feature lists
		fs1.clear();
		fs2.clear();

		this.stopFlag = false;

		// Execute with parameter p
		final FloatArray2DSIFT sift = new FloatArray2DSIFT(p.sift);
		final SIFT icySIFT = new SIFT(sift);

		// Show gray images
		// if (inIsDebug.getValue()) {
		// FloatArray2D fa1 =
		// ImageArrayConverter.SequenceToFloatArray2DNormalize(seq1);
		// Sequence seq1gray = ImageArrayConverter.FloatArray2DtoSequence(fa1);
		// addSequence(seq1gray);
		// FloatArray2D fa2 =
		// ImageArrayConverter.SequenceToFloatArray2DNormalize(seq2);
		// Sequence seq2gray = ImageArrayConverter.FloatArray2DtoSequence(fa2);
		// addSequence(seq2gray);
		// }

		long startTime = 0;
		long totalTime = 0;
		long partialTime = 0;

		if (stopFlag) {
			endExecution();
			return;
		}

		// Extract features from 1st image
		System.out.println("Processing SIFT for image 1...");
		startTime = System.nanoTime();
		icySIFT.extractFeatures(seq1, fs1);
		partialTime = (System.nanoTime() - startTime) / 1000000;
		totalTime += System.nanoTime() - startTime;
		System.out.println(" took " + partialTime + "ms.");
		System.out.println(" " + fs1.size() + " features extracted.");

		if (stopFlag) {
			endExecution();
			return;
		}

		// Extract features from 2nd image
		System.out.println("Processing SIFT for image 2...");
		startTime = System.nanoTime();
		icySIFT.extractFeatures(seq2, fs2);
		partialTime = (System.nanoTime() - startTime) / 1000000;
		totalTime += System.nanoTime() - startTime;
		System.out.println(" took " + partialTime + "ms.");
		System.out.println(" " + fs2.size() + " features extracted.");

		if (inIsDebug.getValue()) {
			SIFTFeatureOverlay overlay1 = new SIFTFeatureOverlay(fs1);
			seq1.addOverlay(overlay1);
			SIFTFeatureOverlay overlay2 = new SIFTFeatureOverlay(fs2);
			seq2.addOverlay(overlay2);
		}

		if (stopFlag) {
			endExecution();
			return;
		}

		// Preliminary matching with Lowe's criterion
		System.out.println("Preliminary matching ...");
		startTime = System.nanoTime();
		List<PointMatch> candidates = new ArrayList<PointMatch>();
		FeatureTransform.matchFeatures(fs1, fs2, candidates, p.rod, inIsSpatialConstraint.getValue(), p.sc);
		partialTime = (System.nanoTime() - startTime) / 1000000;
		totalTime += System.nanoTime() - startTime;
		System.out.println(" took " + partialTime + "ms.");
		System.out.println(candidates.size() + " potentially corresponding features identified.");

		// Show keypoints after preliminary matching
		if (inIsDebug.getValue()) {
			Sequence preliminaryKeypoints1 = new Sequence();
			Sequence preliminaryKeypoints2 = new Sequence();
			preliminaryKeypoints1.copyFrom(seq1, false);
			preliminaryKeypoints1.setName("Image 1 preliminar matching keypoints");
			preliminaryKeypoints2.copyFrom(seq2, false);
			preliminaryKeypoints2.setName("Image 2 preliminar matching keypoints");

			for (int i = 0; i < candidates.size(); i++) {
				PointMatch match = candidates.get(i);
				Point p1 = match.getP1();
				ROI2DPoint roiPt1 = new ROI2DPoint(p1.getL()[0], p1.getL()[1]);
				roiPt1.setStroke(4);
				roiPt1.setName(Integer.toString(i));
				roiPt1.setShowName(true);
				preliminaryKeypoints1.addROI(roiPt1);

				Point p2 = match.getP2();
				ROI2DPoint roiPt2 = new ROI2DPoint(p2.getL()[0], p2.getL()[1]);
				roiPt2.setStroke(4);
				roiPt2.setName(Integer.toString(i));
				roiPt2.setShowName(true);
				preliminaryKeypoints2.addROI(roiPt2);
			}

			addSequence(preliminaryKeypoints1);
			addSequence(preliminaryKeypoints2);
		}

		if (stopFlag) {
			endExecution();
			return;
		}

		// RANSAC
		System.out.println("Filtering correspondence candidates by RANSAC ...");
		startTime = System.nanoTime();
		List<PointMatch> inliers = new ArrayList<PointMatch>();
		final double[] z = SimilarityTransform.ransac(candidates, inliers, p.NumIter, p.NumInliers, p.maxEpsilon);
		partialTime = (System.nanoTime() - startTime) / 1000000;
		totalTime += System.nanoTime() - startTime;
		System.out.println(" took " + partialTime + "ms.");
		System.out.println(" number of correspondances: " + inliers.size());

		if (inliers.size() != 0) {
			// Print parameters of transformation model
			System.out.println("Similarity model : a = " + z[0] + ", b = " + z[1] + ", t1 = " + z[2] + ", t2 = " + z[3]);

			// Add ROIs
			for (int i = 0; i < inliers.size(); i++) {
				PointMatch match = inliers.get(i);
				Point p1 = match.getP1();
				ROI2DPoint roiPt1 = new ROI2DPoint(p1.getL()[0], p1.getL()[1]);
				// roiPt1.setStroke(4);
				roiPt1.setName(Integer.toString(i));
				roiPt1.setShowName(true);
				seq1.addROI(roiPt1);
				Point p2 = match.getP2();
				ROI2DPoint roiPt2 = new ROI2DPoint(p2.getL()[0], p2.getL()[1]);
				// roiPt2.setStroke(4);
				roiPt2.setName(Integer.toString(i));
				roiPt2.setShowName(true);
				seq2.addROI(roiPt2);
			}
		}

		System.out.println("SIFT Registration finished");
		System.out.println(" total execution time: " + totalTime + "ms.");

	}

	@Override
	public void clean() {
	}

	@Override
	public void stopExecution() {
		// this method is from the EzStoppable interface
		// if this interface is implemented, a "stop" button is displayed
		// and this method is called when the user hits the "stop" button
		stopFlag = true;
	}

	private void endExecution() {
		System.err.println("Execution interrumpted.");
	}
}

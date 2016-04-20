package plugins.danyfel80.registration.sift;

import icy.gui.dialog.MessageDialog;
import icy.sequence.Sequence;

import java.util.ArrayList;
import java.util.List;

import plugins.adufour.blocks.lang.Block;
import plugins.adufour.blocks.util.VarList;
import plugins.adufour.ezplug.EzGroup;
import plugins.adufour.ezplug.EzPlug;
import plugins.adufour.ezplug.EzVarInteger;
import plugins.adufour.ezplug.EzVarSequence;
import plugins.adufour.vars.lang.Var;
import plugins.danyfel80.registration.sift.overlay.SIFTFeatureOverlay;
import algorithms.features.sift.Feature;
import algorithms.features.sift.FloatArray2DSIFT;
import algorithms.features.sift.FloatArray2DSIFT.Param;
import algorithms.features.sift.SIFT;

/**
 * SIFT Featured Extraction Plugin
 * Using the implementation of Hoai Thu NGUYEN.
 * @author Daniel Felipe Gonzalez Obando
 */
public class SIFTFeatureExtraction extends EzPlug implements Block {

  // EzPlug Components
  /**
   * Input image 1.
   */
  private EzVarSequence inSequence1;
  
  /**
   * Maximum resolution.
   * The resolution of the first octave in the scale-space, set to image size
   * to consider all the smallest details of image.
   */
  private EzVarInteger inMaxResolution;
  /**
   * Minimum resolution.
   * The last octave of the scale space will have a resolution larger or
   * equal to this value. Set to small value to consider the large forms
   * of the image.
   */
  private EzVarInteger inMinResolution;
  /**
   * Feature Descriptor Size.
   * The number of 4x4px patch considered around each keypoint.
   */
  private EzVarInteger inFeatDescSize;
  /**
   * Feature descriptor orientation bins.
   * The number of orientation bins in each 4x4px patch.
   */
  private EzVarInteger inFeatDescOBinSize;


  // Input and parameters
  Param siftParam = new Param();
  Sequence seq1;
  List<Feature> fs1 = new ArrayList<Feature>();

  @Override
  protected void initialize() {
    super.getDescriptor().setName("SIFT Features Extractor");
    inSequence1 = new EzVarSequence("Image 1");
    
    EzGroup groupSequences = new EzGroup("Images", inSequence1);
    super.addEzComponent(groupSequences);


    inMaxResolution = new EzVarInteger("Maximum resolution", siftParam.maxOctaveSize, 64, 32768, 32);
    inMaxResolution.setToolTipText(
        "Resolution of the first octave in the scale-space, " +
            "set to image size to consider all the smallest details" +
            " of image."
        );

    inMinResolution = new EzVarInteger("Minimum resolution", siftParam.minOctaveSize, 0, 128, 4);
    inMinResolution.setToolTipText("Last octave of the scale space will have a resolution larger or equal to this value. Set to small value to consider the large forms of image");

    inFeatDescSize = new EzVarInteger("Feature descriptor size", siftParam.fdSize, 4, 400, 2);
    inFeatDescSize.setToolTipText("Number of 4x4px patch considered around each keypoint");

    inFeatDescOBinSize = new EzVarInteger("Feature descriptor orientation bins", siftParam.fdBins, 8, 20, 4);
    inFeatDescOBinSize.setToolTipText("Number of orientation bins in each 4x4px patch");

    EzGroup groupDescriptor = new EzGroup("Feature Descriptor", inMaxResolution, inMinResolution, inFeatDescSize, inFeatDescOBinSize);
    super.addEzComponent(groupDescriptor);
  }

  @Override
  protected void execute() {

    seq1 = inSequence1.getValue();

    siftParam.maxOctaveSize = inMaxResolution.getValue();
    siftParam.minOctaveSize = inMinResolution.getValue();
    siftParam.fdSize = inFeatDescSize.getValue();
    siftParam.fdBins = inFeatDescOBinSize.getValue();

    // Verify if two images are selected
    if ( seq1 == null){
      MessageDialog.showDialog("You must select an image to process!", 0);
      return;
    }

    // Cleanup feature lists
    fs1.clear();


    // Execute with parameter p
    final FloatArray2DSIFT sift = new FloatArray2DSIFT( siftParam );
    final SIFT icySIFT = new SIFT( sift );
    
    long startTime = 0;
    long partialTime = 0;

    // Extract features from 1st image
    System.out.println( "Processing SIFT for image..." );
    startTime = System.nanoTime();
    icySIFT.extractFeatures( seq1, fs1 );
    partialTime = (System.nanoTime() - startTime) / 1000000;
    System.out.println( " took " + partialTime + "ms." );
    System.out.println( " " + fs1.size() + " features extracted." );
    
    SIFTFeatureOverlay overlay1 = new SIFTFeatureOverlay(fs1);
    seq1.addOverlay(overlay1);
  }

  @Override
  public void clean() {
    System.gc();
  }

  @Override
  public void declareInput(VarList inputMap) {
    inSequence1 = new EzVarSequence("Image 1");
    
    inMaxResolution = new EzVarInteger("Maximum resolution", siftParam.maxOctaveSize, 64, 32768, 32);
    inMaxResolution.setToolTipText(
        "Resolution of the first octave in the scale-space, " +
            "set to image size to consider all the smallest details" +
            " of image."
        );

    inMinResolution = new EzVarInteger("Minimum resolution", siftParam.minOctaveSize, 0, 128, 4);
    inMinResolution.setToolTipText("Last octave of the scale space will have a resolution larger or equal to this value. Set to small value to consider the large forms of image");

    inFeatDescSize = new EzVarInteger("Feature descriptor size", siftParam.fdSize, 4, 400, 2);
    inFeatDescSize.setToolTipText("Number of 4x4px patch considered around each keypoint");

    inFeatDescOBinSize = new EzVarInteger("Feature descriptor orientation bins", siftParam.fdBins, 8, 20, 4);
    inFeatDescOBinSize.setToolTipText("Number of orientation bins in each 4x4px patch");
    
    inputMap.add(inSequence1.name, inSequence1.getVariable());
    inputMap.add(inMaxResolution.name, inMaxResolution.getVariable());
    inputMap.add(inMinResolution.name, inMinResolution.getVariable());
    inputMap.add(inFeatDescSize.name, inFeatDescSize.getVariable());
    inputMap.add(inFeatDescOBinSize.name, inFeatDescOBinSize.getVariable());
  }

  
  private Var<List<Feature>> outFeatures1 = new Var<List<Feature>>("Features Image 1", new ArrayList<Feature>());
  @Override
  public void declareOutput(VarList outputMap) {
    outFeatures1.setValue(fs1);
    outputMap.add(inSequence1.name, inSequence1.getVariable());
    outputMap.add(outFeatures1.getName(), outFeatures1);
  }
}

package algorithms.features.sift;

/**
 * 
 * @author Hoai Thu NGUYEN
 * @date Jan-16
 */
import icy.sequence.Sequence;
import icy.type.DataType;
import icy.image.IcyBufferedImage;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

final public class ImageArrayConverter {
	final static public FloatArray2D SequenceToFloatArray2D( final Sequence seq)
	{
		int w = seq.getSizeX();
		int h = seq.getSizeY();
		IcyBufferedImage im = seq.getFirstImage();
		
		//Convert image to gray
		float[] data = new float[w*h];
		for ( int x = 0; x<w; ++x )
		{
			for (int y = 0; y<h;++y){
				final int rgb = im.getRGB(x, y);
				final int b = rgb & 0xff;
				final int g = ( rgb >> 8 ) & 0xff;
				final int r = ( rgb >> 16 ) & 0xff;
				data[ x+y*w ] = 0.2989f * r + 0.5870f * g + 0.1140f * b;
			}
		}
		FloatArray2D fa = new FloatArray2D(data, w, h);
		return fa;
	}
	
	final static public FloatArray2D SequenceToFloatArray2DNormalize ( final Sequence seq )
	{
		//Normalize data in a sequence to a floatarray2D with range from 0 to 1
		FloatArray2D fa = SequenceToFloatArray2D(seq);
		FloatArray2D fa_normalized = fa.clone();
		
		List<Float> list = Arrays.asList(ArrayUtils.toObject(fa.data)); 
		float min = Collections.min(list);
		float max = Collections.max(list);
		
	    float scale = (float) (1.0/(max-min));
	    
	    for (int i = 0; i<fa_normalized.data.length; ++i){
	    	fa_normalized.data[i] = (fa_normalized.data[i] - min)*scale;
	    }
		
		return fa_normalized;
	}
	
	final static public Sequence FloatArray2DtoSequence ( final FloatArray2D fa )
	{
		IcyBufferedImage image = new IcyBufferedImage(fa.width, fa.height, 1, DataType.FLOAT);

        // get a direct reference to first component data
        float[] dataBuffer = image.getDataXYAsFloat(0);

        // fill data
        System.arraycopy(fa.data, 0, dataBuffer, 0, fa.data.length);

        // notify to icy that data has changed to refresh internal state and display
        image.dataChanged();

        // create a sequence from the generated image
        Sequence sequence = new Sequence("Float Image", image);

        return sequence;
	}
	
	
	
}

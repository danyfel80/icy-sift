package algorithms.models;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import algorithms.utils.Matrix;


/**
 * Find the optimal similarity transformation between 2 images
 * using Procrutes analysis
 * Only in 2D
 * 
 * Implementation based on article of Lippolis et al (2013)
 * 
 * @author Hoai Thu NGUYEN
 * @date Jan-16
 * 
 */

public class SimilarityTransform {

	private SimilarityTransform(){}
	
	//Algorithm RANSAC
	static public double[] ransac (List<PointMatch> candidates, final List<PointMatch> inliers, int nbIter, int nbrand, double maxEpsilon) {
	
		int N = candidates.size();
		
		int maxInliers = 0;
		boolean[] inlierornot = new boolean[N];
		
		for (int i=0; i<nbIter; i++)
		{
			List<PointMatch> randomkeypoints = new ArrayList<PointMatch>();
			//Select randomly nbrand point pairs
			for (int j=0; j<nbrand; j++)
			{
				Random rand = new Random();
				int randomnb = rand.nextInt(N);
				randomkeypoints.add(candidates.get(randomnb));
			}
			
			double[] z = ProcrustesAnalyze(randomkeypoints);
			boolean[] tempboolean = getInliers(candidates, z, maxEpsilon);
			int count = 0;
			for (int j=0; j<N; j++)
				if (tempboolean[j])
					count += 1;
			if (count > maxInliers){
				maxInliers = count;
				inlierornot = tempboolean;
			}					
		}
		double[] zfinal = new double[4];
		if (maxInliers != 0){
			//Get best inliers
			for (int i=0; i<N; i++)
				if (inlierornot[i])
					inliers.add(candidates.get(i));
			
			//Estime model with best inliers 
			zfinal = ProcrustesAnalyze(inliers);
		}
		
		return zfinal;
	}

	//Find  z = (a, b, t1, t2) with BT*B*z=BT*Y  
	static public double[] ProcrustesAnalyze(List<PointMatch> candidates)
	{		
		int N = candidates.size();
		
		double [] Y = new double[2*N];
		double[][] BT = new double[4][2*N];
		
		for (int i=0; i<N; i++)
		{
			PointMatch match = candidates.get(i);
			Point p1 = match.getP1();
			Point p2 = match.getP2();
			
			double[] l1 = p1.getL();
			double[] l2 = p2.getL();
			
			//Vector Y contains the coordinates of point correspondances from 2nd image
			Y[2*i] = l2[0];
			Y[2*i+1] = l2[1];
			
			//Matrix BT
			BT[0][2*i] = l1[0];
			BT[0][2*i+1] = l1[1];
			BT[1][2*i] = -l1[1];
			BT[1][2*i+1] = l1[0];
			BT[2][2*i] = 1;
			BT[2][2*i+1] = 0;
			BT[3][2*i] = 0;
			BT[3][2*i+1] = 1;			
		}	
		
		double[][] B = Matrix.transpose(BT);
		double[][] A = Matrix.multiply(BT, B);
		double[] C = Matrix.multiply(BT, Y);
		//Invert A
		double[][] A1 = Matrix.invert(A);
		double[] z = Matrix.multiply(A1, C);	
		
		return z;
	}
	
	
	//Tranform coordianates using similarity model and filter correspondance
	static public boolean[] getInliers (List<PointMatch> candidates, double[] z, double maxEpsilon){
		int N = candidates.size();
		double epsilon = maxEpsilon * maxEpsilon;
		boolean[] inlierornot = new boolean[N];
		
		for (int i=0; i<N; i++)
		{
			PointMatch match = candidates.get(i);
			Point p1 = match.getP1();
			Point p2 = match.getP2();
			double[] l1 = p1.getL();
			double[] l2 = p2.getL();
			
			double[][] z1 = new double[2][2];
			z1[0][0] = z[0];
			z1[0][1] = - z[1];
			z1[1][0] = z[1];
			z1[1][1] = z[0];			
			double[] temp = Matrix.multiply(z1, l1);
			
			//Transformed coordinates
			double[] lt = new double[2];
			lt[0] = temp[0] + z[2];
			lt[1] = temp[1] + z[3];
			
			//Filter to get inliers
			double dis2 = Math.pow(lt[0] - l2[0], 2) + Math.pow(lt[1] - l2[1], 2);
			if (dis2 > epsilon)
				inlierornot[i] = false;
			else
				inlierornot[i] = true;
		}		    			
		
		return inlierornot;
	}
	
}

/*

Implementation of the naive Bayes version of the EM algorithm.

*/

package classes;

import java.math.*;
import java.util.*;

public class EM{
	
	public static void main(String[] args) {
		int[][] X = {{1, 0, 0, 0, 0}, {1, 0, 0, 0, 0}, 
				{0, 0, 1, 0, 0}, {0, 1, 1, 1, 0}};
		int[] Y = {0, 1, 2};
		EM obj = new EM(X, Y);
		obj.getQxy();
		obj.getQy();
		obj.getDelta(); 
		obj.getResult();
	}

	int n; int d; int k;
	double[][] Qxy; double[] Qy; double[][] delta;
	int[] result;

	public EM(int[][] X, int[] Y) { //create an EM object

		k = Y.length; //size of classes
		n = X.length; //size of training data
		d = X[0].length; //size of feature

		Qy = new double[k];
		Qxy = new double[k][d];
		delta = new double[n][k];		

		double[] QyPrime = new double[k];
		double[][] QxyPrime = new double[k][d];

		//Intialization
		init(Qy, Qxy);

		do {
			System.arraycopy(Qy, 0, QyPrime, 0, Qy.length);
			for (int i = 0; i < Qxy.length; i++) {
    				QxyPrime[i] = Qxy[i].clone();
			}
			//E step: calculate delta
			E_step(Qxy, Qy, delta, X);
			//M step: update Qy and Qxy based upon delta
			M_step(Qxy, Qy, delta, X);
					
		} while (!Converge(Qy, QyPrime, Qxy, QxyPrime)); 

		//Output result
		result = new int[n];
		for (int i = 0; i < n; i++) {
			result[i] = maxIndex(delta[i]);
		}				
	}

	public double[][] getQxy() {
		return Qxy;
	}
	public double[] getQy() {
		return Qy;
	}
	public double[][] getDelta() {
		return delta;
	}
	public int[] getResult() {
		return result;
	}

	////////////////////////////////
	///INDIVIDUAL STEP FUNCTION/////
	////INIT, E, M, CONVERGE////////
	////////////////////////////////

	//E Step
	public static void E_step(double[][] Qxy, double[] Qy, double[][] delta, int[][] X) {
		int k = Qy.length; //size of classes
		int n = X.length; //size of training data
		int d = X[0].length; //size of feature

		//perform the E_step of EM
		for (int i = 0; i < n; i++) {					
			double sum = 0;
			double[] temp = new double[k]; //temporary array holding nominator of class prob of a training example
			for (int j = 0; j < k; j++) {					
				temp[j] = Qy[j];				
				for (int l = 0; l < d; l++) {
					if (X[i][l] == 1) {temp[j] = temp[j]*Qxy[j][l];}
					else { temp[j] = temp[j]*(1 - Qxy[j][l]); }
					}
				sum += temp[j]; 
				}
			for (int j = 0; j < k; j++) {
				if (sum == 0){ 
					delta[i][j] = 1/ (double) k;
					}
				else {
					delta[i][j] = temp[j]/ sum;
					} 
				}
		}
	}

	//M step
	public static void M_step(double[][] Qxy, double[] Qy, double[][] delta, int[][] X) {

	int k = Qy.length; //size of classes
	int n = X.length; //size of training data
	int d = X[0].length; //size of feature

		for (int i = 0; i < k; i++) {						
			double temp = 0;						
			for (int j = 0; j < n; j++) {
				temp += delta[j][i];
			}						
			Qy[i] = temp/ (double) n; //update Qy
		}

		double[][] inverted_delta = invert(delta);
		double[][] inverted_X = invert(X);
		for (int index_of_k = 0; index_of_k < k; index_of_k++) {
			for (int index_of_d = 0; index_of_d < d; index_of_d++) {
				Qxy[index_of_k][index_of_d] = multiply(inverted_delta[index_of_k], inverted_X[index_of_d])/ (double) sum(inverted_delta[index_of_k]);	
			}
		}
	}

	//init step
	public static void init(double[] Qy, double[][] Qxy) {
		double sum = 0;
		for (int i = 0; i < Qy.length; i++) {
			Qy[i] = Math.random();
			sum += Qy[i];
		} 
		for (int i = 0; i < Qy.length; i++) {
			Qy[i] = Qy[i]/ sum;
		}
		for (int i = 0; i < Qxy.length; i++) {
			for (int j = 0; j < Qxy.length; j++) {
				Qxy[i][j] = Math.random();
			}
		}

	}

	public static boolean Converge(double[] Qy, double[] QyPrime, double[][] Qxy, double[][] QxyPrime) {
		//System.out.println(Arrays.toString(Qy));
		//System.out.println(Arrays.toString(QyPrime));
		double threshold = 0.01;
		double diffQy = 0.0, diffQxy = 0.0;
		for (int i = 0; i < Qy.length; i++) {
			diffQy += Math.abs(Qy[i] - QyPrime[i]);
		}
		for (int i = 0; i < Qxy.length; i++) {
			for (int j = 0; j < Qxy.length; j++) {
			diffQxy += Math.abs(Qxy[i][j] - QxyPrime[i][j]);
			}
		}
		return ((diffQy < threshold) && (diffQxy < threshold));
	}

	//////////////////////////////
	////ADDING HELPER FUNCTIONS///
	//////////////////////////////

	public static int maxIndex(double[] array) {
		int max_index = 0;
		double max = array[0];
		for (int i = 1; i < array.length; i++) {
			if (array[i] > max) {
				max = array[i];
				max_index = i;
			}
		}
		return max_index;
	}	

	public static double sum(double[] array) {
		double sum = 0;
		for (double i : array) {
			sum += i;
		}
		return sum;
	}

	public static double multiply(double[] array1, double[] array2) {
		double m = 0;
		for (int i = 0; i < array1.length; i++) {
			m += array1[i] * array2[i];
		}
		return m;
	}

	public static double[][] invert(double[][] double_array) {

		int a = double_array.length;
		int b = double_array[0].length;
		double[][] inverted_double_array = new double[b][a];
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				inverted_double_array[j][i] = double_array[i][j];
			}
		}

		return inverted_double_array;

	}

	public static double[][] invert(int[][] int_array) {

		int a = int_array.length;
		int b = int_array[0].length;
		double[][] inverted_int_array = new double[b][a];
		for (int i = 0; i < a; i++) {
			for (int j = 0; j < b; j++) {
				inverted_int_array[j][i] = int_array[i][j];
			}
		}

		return inverted_int_array;

	}

}

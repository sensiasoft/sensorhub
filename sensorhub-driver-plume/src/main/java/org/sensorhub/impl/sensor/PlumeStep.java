package org.sensorhub.impl.sensor;

public class PlumeStep {
	double time;
	int numParticles;
	double[][]  points;	

	public PlumeStep(double time, int numParticles, double[][] points) {
		this.time = time;
		this.numParticles = numParticles;
		this.points = points;
	}
}

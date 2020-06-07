/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sparqloptimization.optimizers;

import sparqloptimization.ReorderingJoinsOptimizer;


public abstract class StochasticOptimizer implements ReorderingJoinsOptimizer {
    
    public abstract double[] getEvaluations();
    
    public double[] getBestEvaluations(){ //best fitness until i-th evaluation
        double[] evaluations = getEvaluations();
        double[] bestEvaluations = new double[evaluations.length];
        for (int i=0; i<evaluations.length; i++)
            bestEvaluations[i] = i == 0 ? evaluations[0] : Math.min(evaluations[i], bestEvaluations[i-1]);
        return bestEvaluations;
    }

    protected int seed; 
    public void setSeed(int seed){
        this.seed = seed;
    }
}

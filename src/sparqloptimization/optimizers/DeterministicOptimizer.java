/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sparqloptimization.optimizers;


public abstract class DeterministicOptimizer {
    protected double evaluation;
    
    public double getEvaluation(){
        return evaluation;
    }
}

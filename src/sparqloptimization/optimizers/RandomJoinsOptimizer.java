package sparqloptimization.optimizers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
//import java.util.concurrent.ThreadLocalRandom;
import sparqloptimization.ExecutionPlanCostEstimator;
import sparqloptimization.ReorderingJoinsOptimizer;
import sparqloptimization.TriplePattern;

/**
 *Ordering Joins-Based Optimizer using random strategy to find the optimal ordering
 *
 */
public class RandomJoinsOptimizer extends StochasticOptimizer implements ReorderingJoinsOptimizer  {
    
    ExecutionPlanCostEstimator evaluator;
    
    public RandomJoinsOptimizer(ExecutionPlanCostEstimator evaluator) {
        this.evaluator = evaluator;
    }
    
    static <T> List<T> getPermutation(Random rnd, List<T> p){
        List<T> perm = new ArrayList<>(p);
        
        for (int i=0; i<perm.size(); i++)
        {
            int swapWith = rnd.nextInt(perm.size() - i) + i;
            T temp = perm.get(i);
            perm.set(i, perm.get(swapWith));
            perm.set(swapWith, temp);
        }
        return perm;
    }

    double[] evaluations  = new double[1000];

    @Override
    public double[] getEvaluations() {
        return evaluations;
    }
    
    @Override
    public List<TriplePattern> OptimizeQuery(List<TriplePattern> joins) {
       
        Random rnd = new Random(seed);
        int N = evaluations.length; // Number of iterations
        
        double optimalExecutionPlanCost = Double.MAX_VALUE;
        List<TriplePattern> optimalExecutionPlan = joins;
        for (int i=0; i<N; i++)
        {
            List<TriplePattern> currentExecutionPlan = getPermutation(rnd, joins);
            
            double currentExecutionPlanCost = evaluator.evaluatePlan(currentExecutionPlan);
            evaluations[i] = currentExecutionPlanCost;

            if (currentExecutionPlanCost < optimalExecutionPlanCost)
            {
                optimalExecutionPlanCost = currentExecutionPlanCost;
                optimalExecutionPlan = currentExecutionPlan;
                //System.out.println("\n"+i+","+ optimalExecutionPlanCost);
            }
        }
        
        return optimalExecutionPlan;
    }
}

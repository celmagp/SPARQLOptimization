package sparqloptimization.optimizers;

import java.util.ArrayList;
import java.util.List;
import sparqloptimization.ExecutionPlanCostEstimator;
import sparqloptimization.QueryCardinalityEstimator;
import sparqloptimization.ReorderingJoinsOptimizer;
import sparqloptimization.TriplePattern;

/**
 *Ordering Joins-Based Optimizer using a greedy strategy that finds the optimal join ordering in a constructive (bottom-up) way.
 *
 */
public class BottomUpGreedyJoinsOptimizer extends DeterministicOptimizer implements ReorderingJoinsOptimizer {

    QueryCardinalityEstimator evaluator;
    public BottomUpGreedyJoinsOptimizer(QueryCardinalityEstimator evaluator){
        this.evaluator = evaluator;
    }
    
    @Override
    public List<TriplePattern> OptimizeQuery(List<TriplePattern> joins) {
        
        List<TriplePattern> currentSelection = new ArrayList<>();
        boolean[] used = new boolean[joins.size()];
        
        double total = 0;
        
        for(int i=0; i<joins.size(); i++)
        {
            double bestOptionValue = Double.MAX_VALUE;
            int nextOption = -1;
            for (int j=0; j<joins.size(); j++)
                if (!used[j]){
                    currentSelection.add(joins.get(j));
                    double eval = evaluator.estimateQuery(currentSelection);
                    if (eval < bestOptionValue)
                    {
                        nextOption = j;
                        bestOptionValue= eval;
                    }
                    currentSelection.remove(currentSelection.size()-1);
                }
            used[nextOption]= true;
            currentSelection.add(joins.get(nextOption));
            total += bestOptionValue;
        }
        
        System.out.println("Eval for bottom-up"+total);

        this.evaluation = total;

        return currentSelection;
    }
}

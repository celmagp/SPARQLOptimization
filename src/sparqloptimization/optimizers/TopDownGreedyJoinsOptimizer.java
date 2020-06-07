package sparqloptimization.optimizers;

import java.util.ArrayList;
import java.util.List;
import sparqloptimization.ExecutionPlanCostEstimator;
import sparqloptimization.QueryCardinalityEstimator;
import sparqloptimization.ReorderingJoinsOptimizer;
import sparqloptimization.TriplePattern;

/**
 *Ordering Joins Based Optimizer using a greedy strategy that finds the optimal join ordering in a reductive (top-down) way.
 *
 */
public class TopDownGreedyJoinsOptimizer extends DeterministicOptimizer implements ReorderingJoinsOptimizer{
    
    QueryCardinalityEstimator evaluator;
            
    public TopDownGreedyJoinsOptimizer(QueryCardinalityEstimator evaluator){
        this.evaluator = evaluator;
    }

    @Override
    public List<TriplePattern> OptimizeQuery(List<TriplePattern> joins) {
        List<TriplePattern> currentSelection = new ArrayList<>();
        
        List<TriplePattern> pool = new ArrayList<>();
        pool.addAll(joins);

        boolean[] used = new boolean[joins.size()];
        double total = evaluator.estimateQuery(pool);
        
        for(int i=0; i<joins.size(); i++)
        {
            double bestOptionValue = Double.MAX_VALUE;
            int nextOption = -1;
            for (int j=0; j<joins.size(); j++)
                if (!used[j]){
                   // currentSelection.add(joins.get(j));
                    pool.remove(joins.get(j));
                    double eval = evaluator.estimateQuery(pool);
                    if (eval < bestOptionValue)
                    {
                        nextOption = j;
                        bestOptionValue= eval;
                    }
                   // currentSelection.remove(currentSelection.size()-1);
                    pool.add(joins.get(j));
                }
            used[nextOption]= true;
            currentSelection.add(joins.get(nextOption));
            pool.remove(joins.get(nextOption));
            
            total += bestOptionValue;
        }
        
        List<TriplePattern> reverse = new ArrayList<>();
        for (int i = currentSelection.size()-1; i>=0; i--)
            reverse.add(currentSelection.get(i));
        
        this.evaluation = total;
        System.out.println("Top-down eval: "+total);
        
        return reverse;
    }

}

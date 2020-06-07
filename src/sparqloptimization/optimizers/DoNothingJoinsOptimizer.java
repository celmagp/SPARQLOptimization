package sparqloptimization.optimizers;

import java.util.List;
import sparqloptimization.ReorderingJoinsOptimizer;
import sparqloptimization.TriplePattern;

/**
 * Optimizer that keeps the same join ordering of the initial query. 
 * Used for comparisons study.
 */
public class DoNothingJoinsOptimizer implements ReorderingJoinsOptimizer{

    @Override
    public List<TriplePattern> OptimizeQuery(List<TriplePattern> joins) {
        return joins;
    }
    
}

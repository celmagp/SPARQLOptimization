package sparqloptimization.estimators;

import java.util.List;
import sparqloptimization.ExecutionPlanCostEstimator;
import sparqloptimization.QueryCardinalityEstimator;
import sparqloptimization.TriplePattern;

/**
 * Uses a QueryCardinalityEstimator in order to evaluate a cost of a plan based on the amount of intermediate results involved.
 */
public class IntermediateResultEstimator implements ExecutionPlanCostEstimator{

    QueryCardinalityEstimator estimator;
    
    public IntermediateResultEstimator(QueryCardinalityEstimator estimator) {
        this.estimator = estimator;
    }

    @Override
    public double evaluatePlan(List<TriplePattern> executionPlan) {
        double results = 0;
        
        for (int i=0; i<executionPlan.size(); i++){
            List<TriplePattern> intermediateQuery = executionPlan.subList(0, i+1);
            results += estimator.estimateQuery(intermediateQuery);
        }
        
        return results;
    }
}

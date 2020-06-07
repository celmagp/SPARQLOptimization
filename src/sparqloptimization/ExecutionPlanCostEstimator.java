package sparqloptimization;

import java.util.List;

/**
 * Represents a cost estimator for a specific join ordering representing an execution plan of the query.
 */
public interface ExecutionPlanCostEstimator {
    double evaluatePlan (List<TriplePattern> executionPlan);
}

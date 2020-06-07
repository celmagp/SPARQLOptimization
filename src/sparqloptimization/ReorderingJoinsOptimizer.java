package sparqloptimization;

import java.util.List;

/**
 * Represents a joins reordering optimizer for a specific query.
 */
public interface ReorderingJoinsOptimizer {
    List<TriplePattern> OptimizeQuery (List<TriplePattern> joins);
}

package sparqloptimization;

import java.util.List;

/**
 * Represents an estimator of the amount of RDF triples matching a SPARQL query pattern.
 */
public interface QueryCardinalityEstimator {
    
    double estimateQuery(List<TriplePattern> query);
    
}

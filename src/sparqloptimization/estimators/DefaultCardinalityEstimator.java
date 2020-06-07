package sparqloptimization.estimators;

import java.util.List;
import sparqloptimization.QueryCardinalityEstimator;
import sparqloptimization.SparqlQueryExecutor;
import sparqloptimization.TriplePattern;

/**
 * Uses a SparqlQueryExecutor to estimate the cardinality of the query based on the real execution.
 */
public class DefaultCardinalityEstimator implements QueryCardinalityEstimator{

    SparqlQueryExecutor executor;
    public DefaultCardinalityEstimator(SparqlQueryExecutor executor){
        this.executor = executor;
    }
    
    @Override
    public double estimateQuery(List<TriplePattern> query) {
        return executor.getQueryResultSize(query);
    }
    
}

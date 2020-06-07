package sparqloptimization;

import java.util.List;

/**
 * Represents an executor of a SPARQL query represented as a list of triple pattern joins over a specific repository.
 */
public interface SparqlQueryExecutor {
    List<VariableBindingSet> executeQuery (List<TriplePattern> joins);
    double getQueryResultSize(List<TriplePattern> joins);
    double getQueryDistinctResultSize(List<TriplePattern> joins, String select);
}



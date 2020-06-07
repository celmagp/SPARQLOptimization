package sparqloptimization;

/**
 * Provides statistic information about the number of occurrences of specifics components or triples over a RDF dataset.
 */
public interface RDFStatisticsProvider {
    int ObjectOccurrences(String value);
    int SubjectOccurrences(String subject);
    int PredicateOccurrences(String predicate);
    
    /**
     * When implemented determines the ratio of triples containing p2 continuing triples containing p1
     */
    float DependencyRatio (String p1, String p2);
    
    float GetDistribution(String p, String objValue);    
    
    /**
     * When implemented determines the number of distinct elements associated to the predicate.
     * @param p
     * @return 
     */
    float DistinctObjects(String p);
    
    /**
     * When implemented determines the number of distinct subjects associated to the predicate
     * @param p
     * @return 
     */
    float DistinctSubjects(String p);

    int TriplesCount ();
    
    void dispose();
}

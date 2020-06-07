package graphdbsparqloptimization;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.RDF4JException;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResult;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.http.HTTPRepository;


import sparqloptimization.SparqlQueryExecutor;
import sparqloptimization.TriplePattern;
import sparqloptimization.VariableBindingSet;


/**
 * Represents an executor of a query in a repository of the GraphDB engine.
 * 
 */
public class GraphDBExecutor implements SparqlQueryExecutor {
     
    HTTPRepository repo;
    String prefixes;

    public GraphDBExecutor(String repositoryName, String prefixes) throws Exception{
        this.prefixes = prefixes;
        //repo = new HTTPRepository("http://localhost:7700/repositories/"+repositoryName);
        repo = new HTTPRepository("http://localhost:3030/repositories/"+repositoryName);

        RepositoryConnection con = repo.getConnection();
        con.prepareTupleQuery(QueryLanguage.SPARQL, "select * where { }").evaluate();
    }
    
    /**
     * Creates a query string from a list of triple patterns joins
     * @param joins The list of triple patterns
     * @return The SPARQL query string
     */
    private String createQuery(List<TriplePattern> joins) {
        HashMap<String, String> variables= new HashMap<>();
        for (TriplePattern x : joins)
        {
            if (x.Subject.IsVariable)
                variables.put(x.Subject.Value, x.Subject.Value);
            if (x.Predicate.IsVariable)
                variables.put(x.Predicate.Value, x.Predicate.Value);
            if (x.Object.IsVariable)
                variables.put(x.Object.Value, x.Object.Value);
        }
        
        String select = String.join(" ", variables.keySet());
        
        String where = joins.stream().map(t->t.toString()).reduce("", (t, a)-> a=="" ? t : t == "" ? a : a+"."+t);
        
        String  query = prefixes+"\nSELECT "+select+" WHERE {"+where+"}";
        
        return query;
    }

    /**
     * Creates a query string from a list of triple patterns joins
     * @param joins The list of triple patterns
     * @return The SPARQL query string
     */
    private String createCountQuery(List<TriplePattern> joins) {
        HashMap<String, String> variables= new HashMap<>();
        for (TriplePattern x : joins)
        {
            if (x.Subject.IsVariable)
                variables.put(x.Subject.Value, x.Subject.Value);
            if (x.Predicate.IsVariable)
                variables.put(x.Predicate.Value, x.Predicate.Value);
            if (x.Object.IsVariable)
                variables.put(x.Object.Value, x.Object.Value);
        }
        
        String select = String.join(" ", variables.keySet());
        
        String where = joins.stream().map(t->t.toString()).reduce("", (t, a)-> a=="" ? t : t == "" ? a : a+"."+t);
        
        String  query = prefixes+"\nSELECT (count(*) as ?cnt) WHERE {"+where+"}";
        
        return query;
    }
    
    /**
     * Creates a query string from a list of triple patterns joins
     * @param joins The list of triple patterns
     * @return The SPARQL query string
     */
    private String createCountDisticntQuery(List<TriplePattern> joins, String select) {
        String where = joins.stream().map(t->t.toString()).reduce("", (t, a)-> a=="" ? t : t == "" ? a : a+"."+t);
        
        String  query = prefixes+"\nSELECT (count(distinct("+select+")) as ?cnt) WHERE {"+where+"}";
        
        return query;
    }
    
    /**
     * Executes a query over a specified repository
     * @param joins The triple patterns representing the query  
     * @return A list with the query variables bound to their corresponding values
     */
    @Override
    public List<VariableBindingSet> executeQuery(List<TriplePattern> joins) {
       String queryString = createQuery(joins);
       
       //System.out.println("Query String to execute: ");
       //System.out.println(queryString);

        List<VariableBindingSet> results = new ArrayList<>();
        
        try {
            try (RepositoryConnection connection = repo.getConnection()) {
                TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
                int count =0;
                try (TupleQueryResult result = tupleQuery.evaluate()) {
                    while (result.hasNext()) {
                        BindingSet bindingSet = result.next();
                        count++;
                        results.add(new GraphDBVariableBindingSet(bindingSet));
                    }
                }
            }
        } catch (RDF4JException e) {
            // handle exception
        }
        
        return results;
    }
    
    /**
     * Executes a query over a specified repository
     * @param joins The triple patterns representing the query  
     * @return A list with the query variables bound to their corresponding values
     */
    @Override
    public double getQueryResultSize(List<TriplePattern> joins) {
       String queryString = createCountQuery(joins);
       
       //System.out.println("Query String to get result count: ");
       //System.out.println(queryString);

        try {
            try (RepositoryConnection connection = repo.getConnection()) {
                
                TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
                
                try (TupleQueryResult result = tupleQuery.evaluate()) {
                    while (result.hasNext()) {
                        BindingSet bindingSet = result.next();
                        Value v = bindingSet.getValue("cnt");
                        return Double.parseDouble(v.stringValue());
                    }
                }
            }
        } catch (RDF4JException e) {
            // handle exception
        }
        
        return 0.0;
    }

    @Override
    public double getQueryDistinctResultSize(List<TriplePattern> joins, String select) {

        String queryString = createCountDisticntQuery(joins, select);
       
       //System.out.println("Query String to get result count: ");
       //System.out.println(queryString);

        try {
            try (RepositoryConnection connection = repo.getConnection()) {
                
                TupleQuery tupleQuery = connection.prepareTupleQuery(QueryLanguage.SPARQL, queryString);
                
                try (TupleQueryResult result = tupleQuery.evaluate()) {
                    while (result.hasNext()) {
                        BindingSet bindingSet = result.next();
                        Value v = bindingSet.getValue("cnt");
                        return Double.parseDouble(v.stringValue());
                    }
                }
            }
        } catch (RDF4JException e) {
            // handle exception
        }
        
        return 0.0;
    }
    
    /**
     * Wrapper of the result of a SPARQL query in GraphDB engine
     */
    static class GraphDBVariableBindingSet implements VariableBindingSet{
        BindingSet bindings;
        
        public GraphDBVariableBindingSet(BindingSet bindings){ 
            this.bindings = bindings;
        }

        @Override
        public Set<String> getVariables() {
            return bindings.getBindingNames();
        }

        @Override
        public String getValueOf(String variable) {
            return bindings.getBinding(variable).getValue().stringValue();
        }
    }
}

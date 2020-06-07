/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package graphdbsparqloptimization;

import java.util.List;
import sparqloptimization.SparqlQueryExecutor;
import sparqloptimization.TriplePattern;
import sparqloptimization.VariableBindingSet;




/**
 * Represents an executor of a query in a repository of Jena engine.
 * 
 */
public class JenaExecutor implements SparqlQueryExecutor
{

    @Override
    public List<VariableBindingSet> executeQuery(List<TriplePattern> joins) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getQueryResultSize(List<TriplePattern> joins) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public double getQueryDistinctResultSize(List<TriplePattern> joins, String select) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sparqloptimization.optimizers;

import java.util.ArrayList;
import java.util.List;
import sparqloptimization.QueryCardinalityEstimator;
import sparqloptimization.TriplePattern;


public abstract class PathEvaluatorBase {
    
    List<TriplePattern> originalQuery;
    
    QueryCardinalityEstimator estimator;
    
    public PathEvaluatorBase(List<TriplePattern> query, QueryCardinalityEstimator estimator){
        this.originalQuery = query;
        this.estimator = estimator;
    }
    
    public abstract double evaluate (int[] solution, int count, long bitRep);
    
    public static PathEvaluatorBase GetEvaluator(List<TriplePattern> query, QueryCardinalityEstimator estimator){
        if (query.size() < 20) // can be used array path evaluator
            return new ArrayPathEvaluator(query, estimator);
        throw new UnsupportedOperationException();
    }
    
    static class ArrayPathEvaluator extends PathEvaluatorBase{

        double[] cache;
        
        public ArrayPathEvaluator(List<TriplePattern> query, QueryCardinalityEstimator estimator) {
            super(query, estimator);
            cache = new double[1 << query.size()];
        }

        @Override
        public double evaluate(int[] solution, int count, long bitRep) {
            if (cache[(int)bitRep] == 0.0){
                List<TriplePattern> subquery = new ArrayList<>();
                for(int i=0; i<count; i++)
                    subquery.add(originalQuery.get(solution[i]));
                cache[(int)bitRep] = estimator.estimateQuery(subquery);
            }
            return cache[(int)bitRep];
        }
        
    }
}

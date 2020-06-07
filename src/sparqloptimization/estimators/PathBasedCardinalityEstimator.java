/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package sparqloptimization.estimators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import sparqloptimization.Component;
import sparqloptimization.QueryCardinalityEstimator;
import sparqloptimization.RDFStatisticsProvider;
import sparqloptimization.TriplePattern;
import sparqloptimization.tools.DisjointSet;

public class PathBasedCardinalityEstimator implements QueryCardinalityEstimator {
    RDFStatisticsProvider statistics;
    
    public PathBasedCardinalityEstimator(RDFStatisticsProvider statistics){
        this.statistics = statistics;
    }
    
    class ValueToVariableConverter{
        public int vvars = 0;
        public HashMap<String,String> boundValues= new HashMap<>();
        public HashMap<String, String> valuesAsVariables=new HashMap<>();
        public Component convertToVariable (Component cmp){
            if (cmp.IsVariable)
                return cmp;
            if (!valuesAsVariables.containsKey(cmp.Value))
            {
                String variableName= "?__V"+(vvars++);
                valuesAsVariables.put(cmp.Value, variableName);
                boundValues.put(variableName, cmp.Value);
            }
            return new Component(valuesAsVariables.get(cmp.Value));
        }
    }
    
    static class SubqueryInfo{
        public double cardinality;
        public List<TriplePattern> query;
        public HashMap<String, Double> variables;
    }
    
    SubqueryInfo Join2(SubqueryInfo q, TriplePattern p){
        SubqueryInfo result = new SubqueryInfo();
        result.query = new ArrayList<>(q.query);
        result.variables = new HashMap<>();
        for (String v : q.variables.keySet())
            result.variables.put(v, q.variables.get(v));
        
        double pCard = statistics.PredicateOccurrences(p.Predicate.Value);  
        double distinctObj = statistics.DistinctObjects(p.Predicate.Value);
        double distinctSub = statistics.DistinctSubjects(p.Predicate.Value);
        
        double gradSub = Math.max(1, pCard/distinctSub);
        double gradObj = Math.max(1, pCard/distinctObj);
        
        if (!result.variables.containsKey(p.Subject.Value) &&
            !result.variables.containsKey(p.Object.Value)) // cartessian
        {
            result.cardinality = q.cardinality * pCard;
            result.variables.put(p.Subject.Value, distinctSub);
            result.variables.put(p.Object.Value, distinctObj);
        }
        else
            if (!result.variables.containsKey(p.Subject.Value)) // join with obj
            {
                double pSel = 1;
                
                for (TriplePattern t : q.query)
                    if (t.Subject.Value.equals(p.Object.Value))
                    {
                        pSel = Math.min(pSel, statistics.DependencyRatio(p.Predicate.Value, t.Predicate.Value));
                    }
                
                double varSel = Math.min(1, distinctObj / q.variables.get(p.Object.Value));
                result.cardinality = q.cardinality * pSel * gradObj * varSel;
                
                result.variables.put (p.Object.Value, Math.min(q.variables.get(p.Object.Value), distinctObj));
                result.variables.put (p.Subject.Value, distinctSub);
            }
        else
        if (!result.variables.containsKey(p.Object.Value)) // join with sub
        {
           double pSel = 1;

            for (TriplePattern t : q.query)
                if (t.Object.Value.equals(p.Subject.Value))
                {
                    pSel = Math.min(pSel, statistics.DependencyRatio(t.Predicate.Value, p.Predicate.Value));
                }

            double varSel = Math.min(1, distinctSub/q.variables.get(p.Subject.Value));

            result.cardinality = q.cardinality * pSel * gradSub * varSel;

            result.variables.put (p.Subject.Value, Math.min(q.variables.get(p.Subject.Value), distinctSub));
            result.variables.put (p.Object.Value, distinctObj);
        }
        else // both variables are already bound
        {

            double subSel = Math.min(1, distinctSub/q.variables.get(p.Subject.Value));
            double objSel = Math.min(1, distinctObj/q.variables.get(p.Object.Value));

            result.cardinality = q.cardinality * subSel * objSel;

            result.variables.put (p.Subject.Value, Math.min(q.variables.get(p.Subject.Value), distinctSub));
            result.variables.put (p.Object.Value, Math.min(q.variables.get(p.Object.Value), distinctObj));
        }
        result.query.add(p);
        
        return result;
    }
    
    SubqueryInfo Join(SubqueryInfo q, TriplePattern p){
        SubqueryInfo result = new SubqueryInfo();
        result.query = new ArrayList<>(q.query);
        result.variables = new HashMap<>();
        for (String v : q.variables.keySet())
            result.variables.put(v, q.variables.get(v));
        
        double pCard = statistics.PredicateOccurrences(p.Predicate.Value);      // Occ(p_P) 
        double distinctObj = statistics.DistinctObjects(p.Predicate.Value);     // D_o(p_P)
        double distinctSub = statistics.DistinctSubjects(p.Predicate.Value);    // D_s(p_P)
        
        double Vo = q.variables.containsKey(p.Object.Value) ? q.variables.get(p.Object.Value) : 1;
        double Vs = q.variables.containsKey(p.Subject.Value) ? q.variables.get(p.Subject.Value) : 1;

        double deg = 
                !q.variables.containsKey(p.Object.Value) && !q.variables.containsKey(p.Subject.Value) ? 1 :
                !q.variables.containsKey(p.Subject.Value) ? Math.min(1.0 / Vo, 1.0 / distinctObj) :
                !q.variables.containsKey(p.Object.Value) ? Math.min(1.0 / Vs, 1.0 / distinctSub) : 
                Math.min(1, distinctSub / Vs)*Math.min(1, distinctObj / Vo)/pCard;
        
        double pSel = 1; // sigma(p)
    
        for (TriplePattern t : q.query)
        {
            if (t.Subject.Value.equals(p.Object.Value))
                pSel = Math.min(pSel, statistics.DependencyRatio(p.Predicate.Value, t.Predicate.Value));
            if (t.Object.Value.equals(p.Subject.Value))
                pSel = Math.min(pSel, statistics.DependencyRatio(t.Predicate.Value, p.Predicate.Value));
        }        
        
        result.cardinality = q.cardinality * pCard * deg * pSel;

        result.variables.put(p.Subject.Value, !q.variables.containsKey(p.Subject.Value) ?
                distinctSub :
                Math.min(q.variables.get(p.Subject.Value), distinctSub)
                );
        result.variables.put(p.Object.Value, !q.variables.containsKey(p.Object.Value) ?
                distinctObj :
                Math.min(q.variables.get(p.Object.Value), distinctObj)
                );

        result.query.add(p);
        
        return result;
    }
    
    static <T> List<T> getPermutation(Random rnd, List<T> p){
        List<T> perm = new ArrayList<>(p);
        
        for (int i=0; i<perm.size(); i++)
        {
            int swapWith = rnd.nextInt(perm.size() - i) + i;
            T temp = perm.get(i);
            perm.set(i, perm.get(swapWith));
            perm.set(swapWith, temp);
        }
        return perm;
    }
    
//    static int Compare (TriplePattern p1, TriplePattern p2){
//        int cmp;
//        cmp = p1.Subject.Value.compareTo(p2.Subject.Value);
//        if (cmp != 0)
//            return cmp;
//        cmp = p1.Object.Value.compareTo(p2.Object.Value);
//        if (cmp != 0)
//            return cmp;
//        return p1.Predicate.Value.compareTo(p2.Predicate.Value);
//    }
//    
//    static void Sort(List<TriplePattern> q){
//        for(int i=0;i < q.size(); i++)
//            for (int j=i+1; j < q.size(); j++)
//                if (Compare (q.get(i), q.get(j))>0)
//                {
//                    TriplePattern temp = q.get(i);
//                    q.set(i, q.get(j));
//                    q.set(j, temp);
//                }
//    }
    
    @Override
    public double estimateQuery(List<TriplePattern> query) {
        // all bound subjects and objects are transformed to a bound variable.
        List<TriplePattern> tQuery = new ArrayList<>();
        ValueToVariableConverter converter = new ValueToVariableConverter();
        
        for (TriplePattern triple : query)
            if (!triple.Predicate.Value.equals("rdf:type"))
                tQuery.add(new TriplePattern(converter.convertToVariable(triple.Subject), triple.Predicate, converter.convertToVariable(triple.Object)));
      
        if (tQuery.size() == 0)
            return 0;
        
        SubqueryInfo q = new SubqueryInfo();
        q.cardinality = 1;
        q.query = new ArrayList<>(); // empty query
        q.variables = new HashMap<>(); // empty variable set
        
        HashMap<String, Double> varSel = new HashMap<>();
        
        boolean[] taken = new boolean[tQuery.size()];
        for (int i=0; i < tQuery.size(); i++)
        {
            double bestNextQueryCard = Double.MAX_VALUE;
            SubqueryInfo bestNextQuery = null;
            int bestQueryIndex = -1;
            for (int j=0; j < tQuery.size(); j++)
                if (!taken[j])
                {
                    SubqueryInfo next = Join2(q, tQuery.get(j));
                    
                    double potentialCard = next.cardinality;
                    
                    for(String v : next.variables.keySet())
                        if (converter.boundValues.containsKey(v))
                            potentialCard *= (1.0 / next.variables.get(v));
                    
                    if (potentialCard < bestNextQueryCard)
                    {
                        bestNextQueryCard = potentialCard;
                        bestNextQuery = next;
                        bestQueryIndex = j;
                    }
                }
            q = bestNextQuery;
            taken[bestQueryIndex] = true;
        }
        
//        for (TriplePattern t: tQuery)
//        {
//            q = Join2(q, t);
////            
////            if (!varSel.containsKey(t.Subject.Value))
////                varSel.put(t.Subject.Value, 1.0);
////            if (!varSel.containsKey(t.Object.Value))
////                varSel.put(t.Object.Value, 1.0);
////            
////            if (converter.boundValues.containsKey(t.Subject.Value))
////                varSel.put(t.Subject.Value, Math.min(varSel.get(t.Subject.Value),
////                        1.0/statistics.DistinctSubjects(t.Predicate.Value)
////                        ));
////            
////            if (converter.boundValues.containsKey(t.Object.Value))
////                varSel.put(t.Object.Value, Math.min(varSel.get(t.Object.Value),
////                        1.0/statistics.DistinctObjects(t.Predicate.Value)
////                        ));
//        }
        
        for(String v : q.variables.keySet())
            if (converter.boundValues.containsKey(v))
                q.cardinality *= (1.0 / q.variables.get(v));
       
 //       for (Double sel : varSel.values())
 //           q.cardinality *= sel;
        
        return q.cardinality;
    }
    
}

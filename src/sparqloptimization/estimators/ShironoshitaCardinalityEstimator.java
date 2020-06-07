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
import sparqloptimization.Component;
import sparqloptimization.QueryCardinalityEstimator;
import sparqloptimization.RDFStatisticsProvider;
import sparqloptimization.TriplePattern;
import sparqloptimization.tools.DisjointSet;


public class ShironoshitaCardinalityEstimator implements QueryCardinalityEstimator{

    RDFStatisticsProvider statistics;
    
    public ShironoshitaCardinalityEstimator(RDFStatisticsProvider statistics){
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
    
    List<String> ingoing (String var, List<TriplePattern> query){
        List<String> result = new ArrayList<>();
        for (TriplePattern cmp : query)
            if (cmp.Object.Value.equals(var))
                result.add(cmp.Subject.Value);
        return result;
    }
    
    void saveAllMaximalPaths (List<TriplePattern> query, List<String> currentPath, List<List<String>> paths){
        String var = currentPath.get(currentPath.size()-1); // last variable in current Path. v0 p01 v1 p12 v2 ... pn-1n vn
        boolean maximal= true;
        for (TriplePattern cmp : query)
            if (cmp.Subject.Value.equals(var) && !currentPath.contains(cmp.Object.Value))
            {
                maximal = false;
                
                currentPath.add(cmp.Predicate.Value);
                currentPath.add(cmp.Object.Value);

                saveAllMaximalPaths(query, currentPath, paths);
                
                currentPath.remove(currentPath.size()-1);
                currentPath.remove(currentPath.size()-1);
            }        
        
        if (maximal)
            paths.add(new ArrayList<String>(currentPath));
    }
    
    List<List<String>> getMaximalPaths (HashSet<String> variables, List<TriplePattern> query){
        
        List<List<String>> paths= new ArrayList<>();
        
        for (String v: variables){
            List<String> path = new ArrayList<>();
            path.add(v);
            saveAllMaximalPaths(query, path, paths);
        }
        
        List<List<String>> maximalPaths = new ArrayList<>();
        for (List<String> p : paths){
            boolean maximalPath = true;
            for (String pVar : ingoing(p.get(0), query))
                if (!p.contains(pVar))
                    maximalPath = false;
            if (maximalPath)
                maximalPaths.add(p);
        }
        
        return maximalPaths;
    }
    
    double estimateCardinality (List<String> path, HashMap<String,String> boundValues, DisjointSet<String> ds){
        double prob = statistics.PredicateOccurrences(path.get(1));
        String lastProp = path.get(1);
        for(int i = 3; i < path.size(); i+=2){
            String nextProp = path.get(i);
            prob *= statistics.DependencyRatio(lastProp, nextProp);
            lastProp = nextProp;
        }
        
        // now multiply bound variable constraints ratios ...

        //for (int i=2;i<path.size(); i+=2)
//        int i = path.size()-1;
//        {
//            String variable = path.get(i);
//            if (boundValues.containsKey(variable))
//            {
//                String value  = boundValues.get(variable);
//                
//                if (value.startsWith("<")) // uri
//                    prob *= statistics.ObjectOccurrences(value) / (float)statistics.TriplesCount();
//                else // literal
//                    prob *= statistics.GetDistribution(path.get(i-1), value);
//            }
//        }
        
        return prob;
    }
    
    boolean sameGroup (String v1, String v2, DisjointSet<String> ds){
        return ds.representative(v1).equals(ds.representative(v2));
    }
    
    @Override
    public double estimateQuery(List<TriplePattern> query) {
        // all bound subjects and objects are transformed to a bound variable.
        List<TriplePattern> tQuery = new ArrayList<>();
        ValueToVariableConverter converter = new ValueToVariableConverter();
        for (TriplePattern triple : query)
            if (!triple.Predicate.Value.equals("rdt:type"))
                tQuery.add(new TriplePattern(converter.convertToVariable(triple.Subject), triple.Predicate, converter.convertToVariable(triple.Object)));
      
        HashSet<String> variables = new HashSet<>();
        for (TriplePattern t: tQuery)
        {
            variables.add(t.Subject.Value);
            variables.add(t.Object.Value);
        }
        
        // build Maximal Paths
        List<List<String>> maximalPaths = getMaximalPaths(variables, tQuery);
        
        DisjointSet<String> dsVariables = new DisjointSet<>(variables);
        for (TriplePattern t : tQuery)
            dsVariables.join(t.Subject.Value, t.Object.Value);
        
        // Detect groups (every conected component can be estimated separately. then the cross multiplication between them is the total value
        List<String> representatives = new ArrayList<>();
        for (String v : variables)
            if (!representatives.contains(dsVariables.representative(v)))
                representatives.add(dsVariables.representative(v));
        
        HashMap<String, Double> cardinalitiesPerGroup = new HashMap<>();
        for (String v : representatives)
            cardinalitiesPerGroup.put(v, 0.0);
        HashMap<String, Double> variablesSelectivity = new HashMap<>();
        for (String v : converter.boundValues.keySet())
            variablesSelectivity.put(v, 1.0);
        
        for (List<String> path : maximalPaths){
            double probPath = estimateCardinality(path, converter.boundValues, dsVariables);
            String rep = dsVariables.representative(path.get(0));
            double prevCard = cardinalitiesPerGroup.get(rep);
            cardinalitiesPerGroup.put(rep, prevCard + probPath);
            
            for(int i=2; i<path.size(); i+=2)
            {
                String v = path.get(i);
                if (converter.boundValues.containsKey(v)){
                    String value  = converter.boundValues.get(v);
                    double sel = 1;
                    if (value.startsWith("<")) // uri
                        sel = 1.0 / statistics.DistinctObjects(path.get(i-1));         
                    else // literal
                        sel = statistics.GetDistribution(path.get(i-1), value);
                    variablesSelectivity.put(v, Math.min(sel, variablesSelectivity.get(v)));
                }
            }
        }

        double totalCard = 1;
        for(String rep : representatives)
            totalCard *= cardinalitiesPerGroup.get(rep);
        
        for(String v : converter.boundValues.keySet()){
            totalCard *= variablesSelectivity.get(v);
        }
        
        return totalCard;
    }
    
}

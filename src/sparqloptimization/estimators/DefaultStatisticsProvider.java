package sparqloptimization.estimators;

import com.sun.org.apache.bcel.internal.generic.AllocationInstruction;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import sparqloptimization.Component;
import sparqloptimization.RDFStatisticsProvider;
import sparqloptimization.SparqlQueryExecutor;
import sparqloptimization.TriplePattern;

/**
 * Uses a SparqlQueryExecutor to queries the statistic information provided by the engine through the actual execution of the query. 
 * Maintains a cache with the statistics already calculated.
 */
public class DefaultStatisticsProvider implements RDFStatisticsProvider {
    SparqlQueryExecutor executor;
    String cacheFile;
    public DefaultStatisticsProvider(SparqlQueryExecutor executor, String cacheFile){
        this.executor = executor;
        this.cacheFile= cacheFile;
        try {
            InputStream stream = new BufferedInputStream(new FileInputStream(cacheFile));
            Load(stream);
            stream.close();
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DefaultStatisticsProvider.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DefaultStatisticsProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    boolean disposed= false;
    public void dispose(){
        try {
            if (!disposed)
            {
                OutputStream stream = new BufferedOutputStream(new FileOutputStream(cacheFile));
                Save(stream);
                stream.close();
            }
            disposed = true;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(DefaultStatisticsProvider.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(DefaultStatisticsProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    protected void finalize(){
        try {
            super.finalize();
            dispose();
        } catch (Throwable ex) {
            Logger.getLogger(DefaultStatisticsProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void Save(OutputStream ostream){
        try {
            ObjectOutputStream objStream = new ObjectOutputStream(ostream);
            objStream.writeObject(objectCache);            
            objStream.writeObject(subjectCache);
            objStream.writeObject(predicateCache);
            objStream.writeObject((Integer)count);
            objStream.writeObject(dependency);
            objStream.writeObject(valueDistribution);
            objStream.writeObject(distinctMap);
        } catch (IOException ex) {
            Logger.getLogger(DefaultStatisticsProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    void Load(InputStream istream){
        try {
            ObjectInputStream objStream = new ObjectInputStream(istream);
            objectCache = (Map<String,Integer>)objStream.readObject();
            subjectCache = (Map<String,Integer>)objStream.readObject();
            predicateCache = (Map<String,Integer>)objStream.readObject();
            count = (Integer)objStream.readObject();
            dependency = (Map<String, Map<String, Float>>)objStream.readObject();
            valueDistribution = (Map<String, Map<String, Float>>)objStream.readObject();
            distinctMap = (Map<String,Float>)objStream.readObject();
        } catch (IOException ex) {
            Logger.getLogger(DefaultStatisticsProvider.class.getName()).log(Level.SEVERE, null, ex);
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(DefaultStatisticsProvider.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    Map<String, Integer> objectCache = new HashMap<>();
    @Override
    public int ObjectOccurrences(String value) {
        if (!objectCache.containsKey(value))
        {
            TriplePattern pattern = new TriplePattern(new Component("?s"), new Component("?p"), new Component(value));
            List<TriplePattern> query = new ArrayList<>();
            query.add(pattern);
            int occurrences = (int)executor.getQueryResultSize(query);
            objectCache.put(value, occurrences);
        }
        return objectCache.get(value);
    }

    Map<String, Integer> subjectCache = new HashMap<>();
    @Override
    public int SubjectOccurrences(String subject) {
        if (!subjectCache.containsKey(subject))
        {
            TriplePattern pattern = new TriplePattern(new Component(subject), new Component("?p"), new Component("?v"));
            List<TriplePattern> query = new ArrayList<>();
            query.add(pattern);
            int occurrences =  (int)executor.getQueryResultSize(query);
            subjectCache.put(subject, occurrences);
        }
        return subjectCache.get(subject);
    }

    Map<String, Integer> predicateCache = new HashMap<>();
    @Override
    public int PredicateOccurrences(String predicate) {
        if (!predicateCache.containsKey(predicate))
        {
            TriplePattern pattern = new TriplePattern(new Component("?s"), new Component(predicate), new Component("?v"));
            List<TriplePattern> query = new ArrayList<>();
            query.add(pattern);
            int occurrences = (int)executor.getQueryResultSize(query);
            predicateCache.put(predicate, occurrences);
        }
        return predicateCache.get(predicate);
    }

    int count = -1;
    @Override
    public int TriplesCount() {
        if (count == -1)
        {
            TriplePattern pattern = new TriplePattern(new Component("?s"), new Component("?p"), new Component("?v"));
            List<TriplePattern> query = new ArrayList<>();
            query.add(pattern);
            count = (int)executor.getQueryResultSize(query);
        }
        return count;
    }

    Map<String, Map<String, Float>> dependency = new HashMap<>();
    @Override
    public float DependencyRatio(String p1, String p2) {
        if (!dependency.containsKey(p1))
            dependency.put(p1, new HashMap<>());
        if (!dependency.get(p1).containsKey(p2))
        {
            TriplePattern t1 =new TriplePattern(new Component("?a"), new Component(p1), new Component("?b"));            
            TriplePattern t2 =new TriplePattern(new Component("?b"), new Component(p2), new Component("?c"));

            List<TriplePattern> query=  new ArrayList<>();
            query.add(t1);
            query.add(t2);
            int p2Afterp1 = (int)executor.getQueryResultSize(query);
            dependency.get(p1).put(p2, p2Afterp1/(float)PredicateOccurrences(p1));
        }
        return dependency.get(p1).get(p2);
    }

    Map<String, Map<String, Float>> valueDistribution = new HashMap<>();
    @Override
    public float GetDistribution(String p, String objValue) {
        if (!valueDistribution.containsKey(p))
            valueDistribution.put(p, new HashMap<>());
        if (!valueDistribution.get(p).containsKey(objValue))
        {
            TriplePattern t =new TriplePattern(new Component("?a"), new Component(p), new Component(objValue));            
            List<TriplePattern> query = new ArrayList<>();
            query.add(t);
            int pWithObjValue = (int)executor.getQueryResultSize(query);
         
            valueDistribution.get(p).put(objValue, pWithObjValue/(float)PredicateOccurrences(p));
        }
        return valueDistribution.get(p).get(objValue);
    }

    Map<String, Float> distinctMap = new HashMap<>();
    @Override
    public float DistinctObjects(String p) {
        if (!distinctMap.containsKey(p))
        {
            TriplePattern t =new TriplePattern(new Component("?a"), new Component(p), new Component("?o"));            

            List<TriplePattern> query =new  ArrayList<>();
            query.add(t);
            
            int distinctObjects = (int)executor.getQueryDistinctResultSize(query, "?o");
            distinctMap.put(p, (float)distinctObjects);
        }
        return distinctMap.get(p);
    }
    
    Map<String, Float> distinctSubMap = new HashMap<>();
    @Override
    public float DistinctSubjects(String p) {
        if (!distinctSubMap.containsKey(p))
        {
            TriplePattern t =new TriplePattern(new Component("?a"), new Component(p), new Component("?o"));            

            List<TriplePattern> query =new  ArrayList<>();
            query.add(t);
            
            int distinctObjects = (int)executor.getQueryDistinctResultSize(query, "?a");
            distinctSubMap.put(p, (float)distinctObjects);
        }
        return distinctSubMap.get(p);
    }
    
}

package graphdbsparqloptimization;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import sparqloptimization.estimators.DefaultStatisticsProvider;
import sparqloptimization.estimators.IntermediateResultEstimator;
import sparqloptimization.ExecutionPlanCostEstimator;
import sparqloptimization.QueryCardinalityEstimator;
import sparqloptimization.SparqlQueryExecutor;
import sparqloptimization.ReorderingJoinsOptimizer;
import sparqloptimization.TriplePattern;
import sparqloptimization.optimizers.ACOJoinsOptimizer;
import sparqloptimization.optimizers.BottomUpGreedyJoinsOptimizer;
import sparqloptimization.optimizers.DoNothingJoinsOptimizer;
import sparqloptimization.optimizers.PACOJoinsOptimizer;
import sparqloptimization.optimizers.RandomJoinsOptimizer;
import sparqloptimization.optimizers.TopDownGreedyJoinsOptimizer;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;

import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.AbstractList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import org.slf4j.LoggerFactory;

import sparqloptimization.RDFStatisticsProvider;
import sparqloptimization.estimators.DefaultCardinalityEstimator;
import sparqloptimization.estimators.PathBasedCardinalityEstimator;
import sparqloptimization.estimators.ShironoshitaCardinalityEstimator;
import sparqloptimization.optimizers.StochasticOptimizer;

public class Program {

    static String BindingToString(Map<String, String> map){
        return map.keySet().stream().reduce("", (a, k)-> a+" "+(k+":"+map.get(k)));
    }
    
    
    static void Test(String opName, ReorderingJoinsOptimizer optimizer, List<TriplePattern> query, ExecutionPlanCostEstimator evaluator, SparqlQueryExecutor executor){
        
        List<TriplePattern> ep = optimizer.OptimizeQuery(query);
        double intermediateResults = evaluator.evaluatePlan(ep);
        double finalResults = executor.getQueryResultSize(ep);
        
        System.out.println("Optimizer: "+opName+" gets: "+finalResults+" with: "+intermediateResults);
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
    
    public static void TestCardinalityEstimators(String outputFile, QueryCardinalityEstimator[] estimators){
        
        try {
            
            IntermediateResultEstimator[] costEstimators = new IntermediateResultEstimator[estimators.length];
            for (int i=0; i<estimators.length; i++)
                costEstimators[i] = new IntermediateResultEstimator(estimators[i]);
            
            OutputStreamWriter w = new FileWriter(outputFile);
            int i = 1 ;
            List<TriplePattern> query = null;
            Random r = new Random(1001);    
            //while ((query = QueriesBenchmark.getQueriesForTestingCardinalities(i)).size()>0)
            while ((query = QueriesBenchmark.getQuery(i)).size()>0)
            {
                for (int p=0; p<100; p++)
                {
                    w.write(p+",");
                    List<TriplePattern> toTest = getPermutation(r, query);
                    System.out.println("Testing query "+i+" perm "+p);
                    for (int j=0; j<estimators.length; j++)
                    {
                        System.out.print("Testing cardinality estimator "+j+": "+estimators[j]);

                        double cardinality = costEstimators[j].evaluatePlan(toTest);
                        
                        if (j == 0 && cardinality > 100000000)
                            break; // not analize big permutations...

                        System.out.println("> "+cardinality);

                        w.write(Double.toString(cardinality));
                        if (j != estimators.length-1)
                            w.write(", ");
                    }
                    w.write("\n");
                }
                w.write(">\n");

                i++;
            }
            w.close();
        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(Program.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Program.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }
    
    public static void TestCardinalityEstimators2(String outputFile, QueryCardinalityEstimator[] estimators){
        
        try {
            
            OutputStreamWriter w = new FileWriter(outputFile);
            int i = 1 ;
            List<TriplePattern> query = null;
            Random r = new Random(1001);    
            //while ((query = QueriesBenchmark.getQueriesForTestingCardinalities(i)).size()>0)
            while ((query = QueriesBenchmark.getQuery(i)).size()>0)
            {
                for (int p=0; p<40; p++)
                {
                    w.write(p+",");
                    List<TriplePattern> toTest = getPermutation(r, query);
                    System.out.println("Testing query "+i+" perm "+p);
                    for (int j=0; j<estimators.length; j++)
                    {
                        System.out.print("Testing cardinality estimator "+j+": "+estimators[j]);

                        double cardinality = estimators[j].estimateQuery(toTest);
                        
                        if (j == 0 && cardinality > 10000000)
                            break; // not analize big permutations...

                        System.out.println("> "+cardinality);

                        w.write(Double.toString(cardinality));
                        if (j != estimators.length-1)
                            w.write(", ");
                    }
                    w.write("\n");
                }
                w.write(">\n");

                i++;
            }
            w.close();
        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(Program.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Program.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }
    
    
    static  <T> boolean IsPermutation (List<T> a, List<T> b){
        return a.size() == b.size() && a.containsAll(b);
    }
    
    public static void TestOptimizers(String outputFile, SparqlQueryExecutor executor, ExecutionPlanCostEstimator epEvaluator, ReorderingJoinsOptimizer[] optimizers){
        try {
            OutputStreamWriter w = new FileWriter(outputFile);
            int i = 1 ;
            List<TriplePattern> query = null;
            while ((query = QueriesBenchmark.getQuery(i)).size()>0)
            {
                System.out.println("Testing query "+i);
                
                for (int j=0; j<optimizers.length; j++)
                {
                    System.out.print("Testing optimizer "+j+": "+optimizers[j]);

                    long time = System.currentTimeMillis();
                    List<TriplePattern> ep = optimizers[j].OptimizeQuery(query);
                    time = System.currentTimeMillis() - time;
                    System.out.println("Optimizer took "+time+" ms...");
                    
                    for (TriplePattern t : ep)
                        System.out.println(t);
                    
                    if (!IsPermutation(ep, query))
                        System.out.println("ERROR in optimizer!!!!!!");
                    
                    time = System.currentTimeMillis();
                    executor.executeQuery(ep);
                    time = System.currentTimeMillis()-time;
                    System.out.println("Query results in "+time+" ms...");
                   
                    double cost = epEvaluator.evaluatePlan(ep);
                    
                    System.out.println("> "+cost);
                    
                    w.write(Double.toString(cost));
                    
                    if (j != optimizers.length-1)
                        w.write(", ");
                }
                w.write("\n");
                i++;
            }
            w.close();
        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(Program.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Program.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }
    
    public static void TestOptimizer(String outputFile, int tests, SparqlQueryExecutor executor, ExecutionPlanCostEstimator epEvaluator, StochasticOptimizer optimizer){
        try {
            boolean printMessage = false;
            int i = 1 ;
            List<TriplePattern> query = null;
            while ((query = QueriesBenchmark.getQuery(i)).size()>0)
            {
                
                OutputStreamWriter wTests = new FileWriter(outputFile+"_Q"+i+"_tests.csv");
                OutputStreamWriter wBests = new FileWriter(outputFile+"_Q"+i+"_bests.csv");
                OutputStreamWriter wCosts = new FileWriter(outputFile+"_Q"+i+"_costs.csv");

                double ave = 0;
                for (int test = 0; test < tests; test++)
                {
                    optimizer.setSeed(test);

                    System.out.println("Testing query "+i+" with test "+test);

                    long time = System.currentTimeMillis();
                    List<TriplePattern> ep = optimizer.OptimizeQuery(query);
                    double[] evals = optimizer.getEvaluations();
                    // Write down evaluations
                    for (Double d : evals)
                        wTests.write(d.toString()+", ");
                    wTests.write("\n");
                    
                    double[] bestEval = optimizer.getBestEvaluations();
                    for (Double d : bestEval)
                        wBests.write(d.toString()+", ");
                    wBests.write("\n");
                    
                    time = System.currentTimeMillis() - time;
                    if (printMessage)
                        System.out.println("Optimizer took "+time+" ms...");

                    if (!IsPermutation(ep, query))
                        throw new Exception("Optimizer failed.");

                    time = System.currentTimeMillis();
                    executor.executeQuery(ep);
                    time = System.currentTimeMillis()-time;
                    if (printMessage)
                        System.out.println("Query results in "+time+" ms...");

                    double timeCost = time;
                    double intermediateCost = epEvaluator.evaluatePlan(ep);
                    double heuristicCost = bestEval[bestEval.length-1];

                    ave += heuristicCost;
                    
                    wCosts.write(Double.toString(timeCost)+", "+Double.toString(intermediateCost)+", "+Double.toString(heuristicCost)+"\n");

                    System.out.println(optimizer+":"+ Double.toString(timeCost)+", "+Double.toString(intermediateCost)+", "+Double.toString(heuristicCost)+"\n");

                }
                
                System.out.println("Average: "+(ave/tests));

                wCosts.close();
                wTests.close();
                wBests.close();
                i++;
            } // while query
        } catch (FileNotFoundException ex) {
            java.util.logging.Logger.getLogger(Program.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IOException ex) {
            java.util.logging.Logger.getLogger(Program.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Program.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
    }
    
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        // TODO code application logic here
        
        System.out.println("Hello world");
        
        //String prefixes = "";
         String prefixes= "PREFIX rdf: <http://www.w3.org/1999/02/22-rdf-syntax-ns#>\n" +
                         "PREFIX ub: <http://www.uh.cu/univ-bench.owl#>";

        // SparqlQueryExecutor graphDBExecutor = new GraphDBExecutor("Yago", prefixes);
        SparqlQueryExecutor graphDBExecutor;
        try {
            graphDBExecutor = new GraphDBExecutor("lubm", prefixes);
        } catch (Exception ex) {
            java.util.logging.Logger.getLogger(Program.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
            return;
        }
        
       // TriplePattern tp1 = new TriplePattern("?a <http://www.w3.org/2004/02/skos/core#narrower> ?o");
       // TriplePattern tp2 = new TriplePattern("?a <http://www.w3.org/2000/01/rdf-schema#label> \"free_time\"@eng");
          
        //Disabling logs...
        //Set<String> loggers = new HashSet<>(Arrays.asList("org.apache.http"));
        Set<String> loggers = new HashSet<>(Arrays.asList("org.apache.http", "o.a.h", "o.e.r.rio"));

        
        for(String log:loggers)
        {
            Logger logger = (Logger)LoggerFactory.getLogger(log);
            logger.setLevel(Level.INFO);
            logger.setAdditive(false);
        }
        
        RDFStatisticsProvider statistics = new DefaultStatisticsProvider(graphDBExecutor, "statistics.bin");
        
        DefaultCardinalityEstimator realCardinality = new DefaultCardinalityEstimator(graphDBExecutor);
        ShironoshitaCardinalityEstimator shironoshitaCardinalityEstimator = new ShironoshitaCardinalityEstimator(statistics);
        PathBasedCardinalityEstimator pathCardinalityEstimator = new PathBasedCardinalityEstimator(statistics);
        
        ExecutionPlanCostEstimator realEPEvaluator = new IntermediateResultEstimator(realCardinality);
        ExecutionPlanCostEstimator estimatedEPEvaluator = new IntermediateResultEstimator(pathCardinalityEstimator);
        
                // Real evaluation of the execution plan
        DoNothingJoinsOptimizer doNothingOptimizer = new DoNothingJoinsOptimizer();
        RandomJoinsOptimizer randomOptimizer = new RandomJoinsOptimizer(estimatedEPEvaluator);
        BottomUpGreedyJoinsOptimizer bottomUpGreedyOptimizer= new BottomUpGreedyJoinsOptimizer(pathCardinalityEstimator);          
        TopDownGreedyJoinsOptimizer topDownGreedyOptimizer= new TopDownGreedyJoinsOptimizer(pathCardinalityEstimator);
        ACOJoinsOptimizer acoOptimizer = new ACOJoinsOptimizer(statistics);
        PACOJoinsOptimizer pacoOptimizer = new PACOJoinsOptimizer(pathCardinalityEstimator);
        
        
        List<TriplePattern> bestEP = new ArrayList<TriplePattern>();
        bestEP.add(new TriplePattern("?e ub:teachingAssistantOf ?c"));
        bestEP.add(new TriplePattern("?e ub:memberOf ?d"));
        bestEP.add(new TriplePattern("?p ub:teacherOf ?c"));
        bestEP.add(new TriplePattern("?p ub:worksFor ?d"));
        bestEP.add(new TriplePattern("?c ub:name ?C"));
        bestEP.add(new TriplePattern("?e ub:advisor ?p"));
        bestEP.add(new TriplePattern("?e ub:name ?E"));
        bestEP.add(new TriplePattern("?e ub:undergraduateDegreeFrom ?D"));
        bestEP.add(new TriplePattern("?d ub:subOrganizationOf ?K"));
        bestEP.add(new TriplePattern("?a ub:publicationAuthor ?p"));
        bestEP.add(new TriplePattern("?a ub:publicationAuthor ?e"));
        bestEP.add(new TriplePattern("?a ub:name ?A"));

        System.out.print("Estimated int result for GraphDB best EP: "+estimatedEPEvaluator.evaluatePlan(bestEP));
        
        if (false) // Testing cardinality estimators
        {   
            QueryCardinalityEstimator[] cardinalityEstimators = new QueryCardinalityEstimator[] { 
                //shironoshitaCardinalityEstimator,
                pathCardinalityEstimator,
                realCardinality
            };                

            TestCardinalityEstimators("cardinalities.csv", cardinalityEstimators);
        }
        
        if (true) // Testing optimizers
        {
            ReorderingJoinsOptimizer[] optimizers= new ReorderingJoinsOptimizer[]{
                //doNothingOptimizer,
                //randomOptimizer,
              //  bottomUpGreedyOptimizer,
              //  topDownGreedyOptimizer,
                //acoOptimizer,
                pacoOptimizer
            };
            TestOptimizers("optimizers.csv", graphDBExecutor, realEPEvaluator, optimizers);
            //TestOptimizers("optimizers h.csv", graphDBExecutor, estimatedEPEvaluator, optimizers);
        }
        
        if (false){ // testing Gamma factor in paco
            
            for (int i=0; i<=5; i++)
            {
                PACOJoinsOptimizer p = new PACOJoinsOptimizer(pathCardinalityEstimator);
                p.GAMMA = 0.5 + i*0.1;
                TestOptimizer("paco_Gamma_"+i, 10, graphDBExecutor, realEPEvaluator, p);
            }
        }
        
        if (false){
            
            //TestOptimizer("random", 10, graphDBExecutor, realEPEvaluator, randomOptimizer);
            //TestOptimizer("paco", 10, graphDBExecutor, realEPEvaluator, pacoOptimizer);
            //TestOptimizer("aco", 10, graphDBExecutor, realEPEvaluator, acoOptimizer);
            TestOptimizer("aco", 10, graphDBExecutor, estimatedEPEvaluator, acoOptimizer);

        }
        
        statistics.dispose();
    }
    
}

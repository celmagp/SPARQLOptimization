package sparqloptimization.optimizers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import sparqloptimization.ReorderingJoinsOptimizer;
import sparqloptimization.RDFStatisticsProvider;
import sparqloptimization.TriplePattern;

/**
 *Ordering Joins-Based Optimizer using Ant Colony Optimization Metaheuristic method
 *
 */
public class ACOJoinsOptimizer extends StochasticOptimizer implements ReorderingJoinsOptimizer{

    RDFStatisticsProvider statistics;
    
    public ACOJoinsOptimizer(RDFStatisticsProvider statistics){
        this.statistics = statistics;
    }
    
    int EstimateTripleCardinality (TriplePattern pattern){
        // one single variable needs to be estimated with Algorithm 1 from ACO paper.
        // get the minimum occurrence of bound component as upper bound of the estimated value
        int subjectCardinality = Integer.MAX_VALUE;
        if (!pattern.Subject.IsVariable)
            subjectCardinality = statistics.SubjectOccurrences(pattern.Subject.Value);

        int predicateCardinality = Integer.MAX_VALUE;
        if (!pattern.Predicate.IsVariable)
            predicateCardinality = statistics.PredicateOccurrences(pattern.Predicate.Value);

        int objectCardinality = Integer.MAX_VALUE;
        if (!pattern.Object.IsVariable)
            objectCardinality = statistics.ObjectOccurrences(pattern.Object.Value);
        
        return Math.min(subjectCardinality, Math.min(predicateCardinality, objectCardinality));
    }
    
    static int[][][] ranks =  
        { { { 2, 3, 1 }, { 3, 3, 3 } , { 2, 3, 1 } }, // unbound ranks
        { { 4, 6, 2 }, { 6, 0, 6 } , { 4, 6, 2 }} }; // bound ranks
        
    int getRank (int ct1, int ct2, boolean unbounded)
    {
        if (unbounded)
            return ranks[0][ct1][ct2];
        return ranks[1][ct1][ct2];
    }
    
    double JoinFactor (TriplePattern p1, TriplePattern p2){
        int r = 0;
        if (p1.Subject.Value.equals(p2.Subject.Value))
            r += getRank(0, 0, p1.Subject.IsVariable);
        if (p1.Subject.Value.equals(p2.Predicate.Value))
            r += getRank(0, 1, p1.Subject.IsVariable);
        if (p1.Subject.Value.equals(p2.Object.Value))
            r += getRank(0, 2, p1.Subject.IsVariable);
        if (p1.Predicate.Value.equals(p2.Subject.Value))
            r += getRank(1, 0, p1.Predicate.IsVariable);
        if (p1.Predicate.Value.equals(p2.Predicate.Value))
            r += getRank(1, 1, p1.Predicate.IsVariable);
        if (p1.Predicate.Value.equals(p2.Object.Value))
            r += getRank(1, 2, p1.Predicate.IsVariable);
        if (p1.Object.Value.equals(p2.Subject.Value))
            r += getRank(2, 0, p1.Object.IsVariable);
        if (p1.Object.Value.equals(p2.Predicate.Value))
            r += getRank(2, 1, p1.Object.IsVariable);
        if (p1.Object.Value.equals(p2.Object.Value))
            r += getRank(2, 2, p1.Object.IsVariable);

        return (32 - r) / 32.0;
    }
    
    double GetWeight (TriplePattern p1, TriplePattern p2){
        int car1 = EstimateTripleCardinality(p1);
        int car2 = EstimateTripleCardinality(p2);
        double sel1 = car1 / (double)statistics.TriplesCount();
        double sel2 = car2 / (double)statistics.TriplesCount();
        
        double EstimatedJoinValue =0;
        if (p1.HasJoin(p2))
            EstimatedJoinValue = sel1 + JoinFactor(p1, p2) * sel1 * sel2;
        else
            EstimatedJoinValue = 1;
        return EstimatedJoinValue;
    }
    
    @Override
    public List<TriplePattern> OptimizeQuery(List<TriplePattern> joins) {
        double[][] W = new double[joins.size()][joins.size()];
        // matrix W initialization
        for (int i=0; i<joins.size(); i++)
            for (int j=0; j<joins.size(); j++)
                if (i != j)
                    W[i][j] = GetWeight(joins.get(i), joins.get(j));
        int[] perm = AntSolve(W);
        List<TriplePattern> sol = new ArrayList<>();
        for (int i=0; i<perm.length; i++)
            sol.add(joins.get(perm[i]));
        return sol;
    }
    
    private int[] AntSolve(double[][] W)
    {
        Random rnd = new Random(this.seed);

        double bestWalk = Double.MAX_VALUE;
        int[] solForBestWalk = null;

        double[][] ferormonas = new double[W.length][W.length];
        for (int i = 0; i < ferormonas.length; i++)
            for (int j = 0; j < ferormonas[i].length; j++)
                ferormonas[i][j] = 0.0001;

        double phi = 0.5;
        double Q = 0.01;

        evaluations = new double[1000];
        
        int A = 100;
        int N = 10;
        int e = 0;
        while (N > 0)
        {
            // Ejecutar un camino para cada hormiga
            int[][] sol = new int[A][W.length];
            double[] evals = new double[A];
            for (int i=0; i<A; i++)
            {
                sol[i] = ExecuteAnt(rnd, W, ferormonas);
                evals[i] = EvalSolution(W, sol[i]);
                
                evaluations[e++] = evals[i];

                if (bestWalk > evals[i]){
                    bestWalk = evals[i];
                    solForBestWalk = (int[])sol[i].clone();
                }
            }

            // Asignar las nuevas ferormonas a partir de los caminos
            for (int i = 0; i < W.length; i++)
                for (int j = 0; j < W.length; j++)
                {
                    double acc= 0;
                    for (int k=0; k<sol.length; k++) // for each ant
                        acc += DeltaT (sol[k], Q / evals[k], i, j);

                    ferormonas[i][j] = (1 - phi) * ferormonas[i][j] + acc;
                }

            N--;
        }

        return solForBestWalk;
    }

    private double DeltaT(int[] sol, double eval, int i, int j)
    {
        for (int m = 1; m < sol.length; m++)
            if (sol[m - 1] == i && sol[m] == j)
                return eval;
        return 0;
    }

    private double EvalSolution(double[][] W, int[] sol)
    {
        double total = 0;
        for (int i = 1; i < sol.length; i++)
            total += W[sol[i - 1]][sol[i]];
        return total;
    }

    private int RandomSelection (Random rnd, double[] probs)
    {
        double sum = 0;
        for (int i=0; i<probs.length; i++)
            sum += probs[i];

        if (sum == 0)
            return rnd.nextInt(probs.length);

        double sel = rnd.nextDouble() * sum;

        double total = 0;

        for (int i=0; i<probs.length; i++)
        {
            total += probs[i];
            if (sel < total)
                return i;
        }
        return probs.length - 1;
    }

    private int[] ExecuteAnt(Random rnd, double[][] W, double[][] ferormonas)
    {
        int startNode = rnd.nextInt(W.length);

        double alpha = 1;
        double beta  = 2;
        
        int[] sol = new int[W.length];
        boolean[] visited = new boolean[W.length];
        sol[0] = startNode;
        visited[startNode] = true;
        for (int i=1; i<W.length; i++)
        {
            double[] probs = new double[sol.length];

            double maxEdge = 0.001; // used for normalization
            for (int j=0; j < sol.length; j++)
                maxEdge = Math.max(maxEdge, W[sol[i-1]][j]);
            
            for (int j = 0; j < sol.length; j++)
                probs[j] = visited[j] ? 0 :
                        Math.pow(1.0 / (W[sol[i - 1]][j]), beta) * 
                        Math.pow(ferormonas[sol[i - 1]][j], alpha);

            sol[i] = RandomSelection(rnd, probs);
            visited[sol[i]] = true;
        }
        return sol;
    }

    double[] evaluations;
    @Override
    public double[] getEvaluations() {
        return evaluations;
    }
}

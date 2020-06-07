package sparqloptimization.optimizers;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import sparqloptimization.ExecutionPlanCostEstimator;
import sparqloptimization.QueryCardinalityEstimator;
import sparqloptimization.ReorderingJoinsOptimizer;
import sparqloptimization.TriplePattern;


/**
 *Ordering Joins-Based Optimizer using Ant Colony Optimization Metaheuristic method combined with an evaluation function based on the path of triple patterns already evaluated.
 *(Path based Ant Colony Optimization)
 *
 */
public class PACOJoinsOptimizer extends StochasticOptimizer implements ReorderingJoinsOptimizer{

    QueryCardinalityEstimator estimator;
    
    public double PHI = 0.4;
    public double GAMMA = 0.8;
    
    public PACOJoinsOptimizer (QueryCardinalityEstimator estimator){
        this.estimator = estimator;
    }

    @Override
    public String toString() {
        return "PACO (phi: "+PHI+", Gamma: "+GAMMA+")";
    }
    
    double[] evaluations  = new double[1000];

    @Override
    public double[] getEvaluations() {
        return evaluations;
    }
    
    @Override
    public List<TriplePattern> OptimizeQuery(List<TriplePattern> joins) {
        int[] perm = AntSolve(joins);
        List<TriplePattern> sol = new ArrayList<>();
        for (int i=0; i<perm.length; i++)
            sol.add(joins.get(perm[i]));
        return sol;
    }
    
    private int[] AntSolve(List<TriplePattern> joins)
    {
        Random rnd = new Random(seed);
        
        PathEvaluatorBase evaluator = PathEvaluatorBase.GetEvaluator(joins, estimator);

        double bestWalk = Double.MAX_VALUE;
        int[] solForBestWalk = null;

        double[][] ferormonas = new double[joins.size()+1][joins.size()+1];
        for (int i = 0; i < ferormonas.length; i++)
            for (int j = 0; j < ferormonas[i].length; j++)
                ferormonas[i][j] = 1;

        int fitnessEvalCount = 0;
        int A = 10;
        int N = 100;
        while (N > 0)
        {
            // Ejecutar un camino para cada hormiga
            int[][] sol = new int[A][joins.size()];
            double[] evals = new double[A];
            double normWalk = Double.MAX_VALUE;
            for (int i=0; i<A; i++)
            {
                evals[i] = ExecuteAnt(rnd, evaluator, ferormonas, sol[i]);
                
                double checkEval = EvalSolution(sol[i], evaluator);
                
                if (checkEval != evals[i])
                    System.out.println("EEEERRRROOOOORRRRR");
                
                evaluations[fitnessEvalCount] = evals[i];
                fitnessEvalCount++;
                
                normWalk = Math.min (normWalk, evals[i]);
                
                if (bestWalk > evals[i]){
                    bestWalk = evals[i];
                    //System.out.println("\n"+fitnessEvalCount+","+ bestWalk);
                    solForBestWalk = (int[])sol[i].clone();
                }
            }

            // Asignar las nuevas ferormonas a partir de los caminos
            for (int i = 0; i <= joins.size(); i++)
                for (int j = 0; j <= joins.size(); j++)
                    if (i != j)
                {
                    double acc= 0;
                    for (int k=0; k<sol.length; k++) // for each ant
                        acc += DeltaT (sol[k], G(bestWalk) / G(evals[k]), i - 1, j - 1) / sol.length;
                        //acc += DeltaT (sol[k], currentIterationBestWalk / evals[k], i - 1, j - 1);

                    ferormonas[i][j] = Math.min(32, Math.max (1.0/32, (1 - PHI) * ferormonas[i][j] + acc));
                }

            N--;
            
        }
        System.out.println();

        return solForBestWalk;
    }
        
    static double H(double x){
        return Math.pow(Math.log(x + 1), 4);
    }
    
    static double G(double x){
        return Math.pow(Math.log(x + 1), 2);
    }

    private double DeltaT(int[] sol, double eval, int i, int j)
    {
        if (i == -1)
            return sol[0] == j ? eval : 0;
        if (j == -1)
            return 0;
        
        for (int m = 1; m < sol.length; m++)
            if (sol[m - 1] == i && sol[m] == j)
                return eval * Math.pow(GAMMA, m);
        return 0;
    }

    private double EvalSolution(int[] sol, PathEvaluatorBase evaluator)
    {
        double cost = 0;
        long bits = 0;
        for (int i=0; i<sol.length; i++)
        {
            bits |= 1 << sol[i];
            cost += evaluator.evaluate(sol, i+1, bits);
        }
        return cost;
    }

    private int RandomSelection (Random rnd, double[] probs)
    {
        double sum = 0;
        for (int i=0; i<probs.length; i++)
            sum += probs[i];

        if (sum == 0)
            return rnd.nextInt(probs.length);

        double sel = rnd.nextDouble() * sum; //[0,1)*sum => [0,sum)

        double total = 0;

        for (int i=0; i<probs.length; i++)
        {
            total += probs[i];
            if (sel < total)
                return i;
        }
        return probs.length - 1;
    }

    private double ExecuteAnt(Random rnd, PathEvaluatorBase evaluator, double[][] ferormonas, int[] sol)
    {
        int N = sol.length;
        double[] probs = new double[sol.length];    
        long bits = 0;
        boolean[] visited = new boolean[N];
        double[] increments = new double[sol.length];

        double alpha = 2;
        double beta = 0.5;
        double eval = 0;
        
        for (int i=0; i<N; i++)
        {
            double minIncrement = Double.MAX_VALUE;
            for (int j=0; j<sol.length; j++)
                if (!visited[j])
            {
                // try to assume each posible next decision to know the increment in cost with it.
                long newBits = bits | (1 << j);
                sol[i] = j;
                increments[j] = evaluator.evaluate(sol, i+1, newBits);
                minIncrement = Math.min(minIncrement, increments[j]);
                // remove last again to continue testing.
                sol[i] = 0;
            }

            double min = H(minIncrement);
            
            for (int j = 0; j < sol.length; j++)
                probs[j] = visited[j] ? 0 : Math.pow(min / H(increments[j]), beta) * Math.pow(ferormonas[i == 0 ? 0 : sol[i - 1]+1][j+1], alpha);

            sol[i] = RandomSelection(rnd, probs);
            bits |= 1 << sol[i];
            visited[sol[i]] = true;
            eval += increments[sol[i]];
        }
        return eval;
    }
}

package sparqloptimization;

import java.util.Set;

/**
 *Represents the result of a SPARQL as a set of variable bindings
 */
public interface VariableBindingSet {
    
    Set<String> getVariables();
    
    String getValueOf(String variable);
    
}

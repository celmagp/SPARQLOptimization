package sparqloptimization;

/**
 * Represents a component (subject, predicate, object) of a RDF triple or pattern triple
 */
public class Component {
    public boolean IsVariable;
    public String Value;
    
    public Component (String value){
        this.Value = value;
        this.IsVariable = value.startsWith("?");
    }
    
    public boolean SameVariable (Component other){
        return this.IsVariable && other.IsVariable && this.Value.equals(other.Value);
    }
    
    public boolean IsPresentAsVariable (TriplePattern pattern){
        return SameVariable(pattern.Subject) || SameVariable(pattern.Predicate) || SameVariable(pattern.Object);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Component)) return false;
        return Value.equals(((Component)obj).Value); //To change body of generated methods, choose Tools | Templates.
    }
}

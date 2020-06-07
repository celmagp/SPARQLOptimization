package sparqloptimization;

/**
 * Represents a pattern triple of a query with the Subject, Predicate and Object components.
 */
public class TriplePattern {
    public Component Subject;
    public Component Predicate;
    public Component Object;

    public TriplePattern(Component subject, Component predicate, Component object){
        this.Subject = subject;
        this.Predicate= predicate;
        this.Object = object;
    }

    public TriplePattern(String pattern)
    {
        this(new Component(pattern.split(" ")[0]), new Component(pattern.split(" ")[1]), new Component(pattern.split(" ")[2]));
    }

    public int getVariableCount(){
        int count = 0;
        if (Subject.IsVariable) count++;
        if (Predicate.IsVariable) count++;
        if (Object.IsVariable) count++;
        return count;
    }
    
    @Override
    public String toString() {
        return Subject.Value+ " "+Predicate.Value+" "+Object.Value;
    }
    
    public boolean ShareVariable (TriplePattern other){
        return 
                Subject.IsVariable && Subject.IsPresentAsVariable(other) ||
                Predicate.IsVariable && Predicate.IsPresentAsVariable(other) ||
                Object.IsVariable && Object.IsPresentAsVariable(other);
    }
    
    public boolean HasJoin (TriplePattern other){
        return 
                Subject.equals(other.Subject) || Subject.equals(other.Predicate) || Subject.equals(other.Object) ||
                Predicate.equals(other.Subject) || Predicate.equals(other.Predicate) || Predicate.equals(other.Object) ||
                Object.equals(other.Subject) || Object.equals(other.Predicate) || Object.equals(other.Object);
    }
}

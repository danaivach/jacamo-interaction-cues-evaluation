package srm;

import java.util.Objects;

import jason.asSyntax.ListTerm;
import jason.asSyntax.Term;

public class Ability {
    ListTerm types;

    /* Constructor */
    public Ability(ListTerm types){
        this.types = types;
    }

    public ListTerm getTypes() {
        return types;
    }

    /* Equals */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof Ability)) {
            return false;
        }

        Ability other = (Ability) obj;

        return this.types.equals(other.types);
        // return name.equals(other.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(types);
    }

    @Override
    public String toString() {
        return types.toString();
    }
}
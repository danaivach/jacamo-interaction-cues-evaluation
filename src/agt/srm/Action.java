package srm;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import jason.asSyntax.ListTerm;
import jason.asSyntax.Term;

public class Action {
    private ListTerm types;
    private Term artifactName;
    private Term artifactId;

    /* Constructor */
    public Action(ListTerm types, Term artifactName, Term artifactId) {
        this.types = types;
        this.artifactName = artifactName;
        this.artifactId = artifactId;
    }

    /* Getters */
    public ListTerm getTypes() {
        return types;
    }

    public Term getArtifactName() {
        return artifactName;
    }

    public Term getArtifactId() {
        return artifactId;
    }

    // Helper methods for checking compatibility
    private boolean isArtifactIdCompatible(Term otherArtifactId) {
        if (artifactId == null || otherArtifactId == null) {
            return true; // Null values are considered compatible
        }
        return artifactId.equals(otherArtifactId);
    }

    private boolean isArtifactNameCompatible(Term otherArtifactName) {
        if (artifactName == null || otherArtifactName == null) {
            return true; // Null values are considered compatible
        }
        return artifactName.equals(otherArtifactName);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof Action)) return false;

        Action other = (Action) obj;

        // Check types compatibility
        boolean typesEqual = types.equals(other.types);

        // Check artifactId compatibility
        boolean artifactIdEqual = isArtifactIdCompatible(other.getArtifactId());

        // Check artifactName compatibility
        boolean artifactNameEqual = isArtifactNameCompatible(other.getArtifactName());

        return typesEqual && artifactIdEqual && artifactNameEqual;
    }

    @Override
    public int hashCode() {
        // Generate hash code considering null values correctly
        int result = types.hashCode();
        result = 31 * result + (artifactName != null ? artifactName.hashCode() : 0);
        result = 31 * result + (artifactId != null ? artifactId.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "Action{" +
                "types=" + types +
                ", artifactName=" + artifactName +
                ", artifactId=" + artifactId +
                '}';
    }

    public static boolean containsAllActions(Set<Action> exposedActions, Set<Action> planActions) {
        for (Action plan : planActions) {
            boolean found = false;
            for (Action exposed : exposedActions) {
                if (exposed.equals(plan)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }
}

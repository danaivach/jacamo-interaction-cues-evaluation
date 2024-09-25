package srm;

import java.util.*;
import jason.asSemantics.Agent;
import jason.asSemantics.Option;
import jason.asSyntax.*;
import jason.asSyntax.PlanBody.BodyType;

public class SignifierResolutionMechanism extends Agent {

    // Data structure to hold signifier actions and their abilities
    private class SignifierData {
        Set<Action> exposedActions = new HashSet<>();
        Map<Action, Ability> actionAbilities = new HashMap<>();
    }

    // Method to get annotated term and apply unification if needed
    private Term getAnnotatedTerm(Literal bodyTerm, String annotationKey, Option option) {
        Term annotatedTerm = bodyTerm.getAnnot(annotationKey);
        if (annotatedTerm != null) {
            Term termValue = bodyTerm.getAnnot(annotationKey).getTerm(0);
            if (option != null & !termValue.isGround()) {
                return getUnifiedTerm(termValue, option);
            }
            return termValue;
        }
        return null;
    }

    // Retrieve and unify terms using the option's unifier
    private Term getUnifiedTerm(Term term, Option option) {
        Term unifiedTerm = option.getUnifier().get(term.toString());
        return (unifiedTerm != null) ? unifiedTerm : term;
    }

    // Retrieve signifier data (both actions and abilities) in one pass
    private SignifierData getSignifiedActions() {
        SignifierData signifierData = new SignifierData();
        PredicateIndicator predicateIndicator = new PredicateIndicator("signifier", 3);
        Iterator<Literal> candidateBeliefs = getBB().getCandidateBeliefs(predicateIndicator);

        if (candidateBeliefs == null) return signifierData;

        while (candidateBeliefs.hasNext()) {
            Literal candidateBelief = candidateBeliefs.next();
            Term signifierTerm = candidateBelief.getAsListOfTerms().get(2);

            if (!signifierTerm.isList()) continue;

            Term actionTypesTerm = ((ListTerm) signifierTerm).get(0);
            ListTerm actionTypesList;

            if (actionTypesTerm.isList()) {
                actionTypesList = (ListTerm) actionTypesTerm;
            } else if (actionTypesTerm.isString()) {
                actionTypesList = new ListTermImpl().append(actionTypesTerm);
            } else {
                continue;
            }

            Term artifactName = getAnnotatedTerm(candidateBelief, "artifact_name", null);
            Term artifactId = getAnnotatedTerm(candidateBelief, "artifact_id", null);

            // Create the action
            Action action = new Action(actionTypesList, artifactName, artifactId);
            signifierData.exposedActions.add(action);

            // Extract abilities
            Term abilityTypesTerm = ((ListTerm) signifierTerm).get(1);
            ListTerm abilityTypesList;

            if (abilityTypesTerm.isList()) {
                abilityTypesList = (ListTerm) abilityTypesTerm;
            } else if (abilityTypesTerm.isString()) {
                abilityTypesList = new ListTermImpl().append(abilityTypesTerm);
            } else {
                continue;
            }

            // Create the Ability
            Ability ability = new Ability(abilityTypesList);

            // Associate the ability with the action
            signifierData.actionAbilities.put(action, ability);
        }

        return signifierData;
    }

    // Get agent abilities from beliefs
    public Set<Ability> getAgentAbilities() {
        PredicateIndicator predicateIndicator = new PredicateIndicator("ability", 1);
        Iterator<Literal> candidateBeliefs = getBB().getCandidateBeliefs(predicateIndicator);
        Set<Ability> abilities = new HashSet<>();

        if (candidateBeliefs == null) return abilities;

        while (candidateBeliefs.hasNext()) {
            Literal candidateBelief = candidateBeliefs.next();
            Term abilityName = candidateBelief.getTerm(0);
            if (abilityName.isList()) {
                abilities.add(new Ability((ListTerm) abilityName));
            } else {
                ListTermImpl abilityList = new ListTermImpl();
                abilityList.add(abilityName);
                abilities.add(new Ability(abilityList));
            }
        }
        return abilities;
    }

    // Get actions from the plan body, applying unification
    public Set<Action> getPlanActions(Option option) {
        Plan plan = option.getPlan();
        Set<Action> planActionsList = new HashSet<>();
        PlanBody planBody = plan.getBody();

        while (planBody != null) {
            if (planBody.getBodyType() == BodyType.action) {
                Literal bodyTerm = (Literal) planBody.getBodyTerm();
                if (bodyTerm.getFunctor().equals("invokeAction")) {
                    Term actionTypesTerm = bodyTerm.getAsListOfTerms().get(2);
                    ListTerm actionTypesList;

                    if (actionTypesTerm.isList()) {
                        actionTypesList = (ListTerm) actionTypesTerm;
                    } else if (actionTypesTerm.isString()) {
                        actionTypesList = new ListTermImpl().append(actionTypesTerm);
                    } else {
                        continue;
                    }

                    Term artifactName = getAnnotatedTerm(bodyTerm, "artifact_name", option);
                    Term artifactId = getAnnotatedTerm(bodyTerm, "artifact_id", option);

                    planActionsList.add(new Action(actionTypesList, artifactName, artifactId));
                }
            }
            planBody = planBody.getBodyNext();
        }
        return planActionsList;
    }

    // Optimized selectOption: using cached signifier data to avoid redundant belief loops
    @Override
    public Option selectOption(List<Option> options) {
        if (options == null || options.isEmpty()) return null;

        // Get signifier data (actions and abilities) in one pass
        SignifierData signifierData = getSignifiedActions();
        Set<Action> exposedActions = signifierData.exposedActions;
        Map<Action, Ability> actionAbilities = signifierData.actionAbilities;

        // Get agent abilities once
        Set<Ability> agentAbilities = getAgentAbilities();

        // Check each option for applicability
        for (Option option : options) {
            Set<Action> planActions = getPlanActions(option);

            // Check if all plan actions are satisfied by exposed actions
            if (!Action.containsAllActions(exposedActions, planActions)) {
                continue; // Skip this option if actions don't match
            }

            // Check abilities for each plan action
            boolean isApplicable = true;
            for (Action action : planActions) {
                Action exposedAction = findCompatibleKey(actionAbilities, action);

                if (exposedAction == null ) { continue; }

                Ability recommendedAbility = actionAbilities.get(exposedAction);
                if (recommendedAbility != null && !recommendedAbility.getTypes().isEmpty()) {
                    //System.out.println("Recommended " + recommendedAbility);
                    //System.out.println("Agent " + agentAbilities);
                    if (!agentAbilities.contains(recommendedAbility)) {
                        isApplicable = false;
                        break; // Exit loop early if any ability is missing
                    }
                }
            }

            if (isApplicable) {
                return option; // Return the first applicable option
            }
        }
        return null; // No applicable option found
    }

    public static Action findCompatibleKey(Map<Action, ?> map, Action action) {

        for (Action key : map.keySet()) {
            // Check if action types are contained in the key's types
            boolean typesMatch = key.getTypes().containsAll(action.getTypes());

            // Check artifact ID compatibility
            boolean artifactIdMatch = action.getArtifactId() == null
                    || (key.getArtifactId() != null && key.getArtifactId().equals(action.getArtifactId()));
            // Check artifact name compatibility
            boolean artifactNameMatch = action.getArtifactName() == null
                    || (key.getArtifactName() != null && key.getArtifactName().equals(action.getArtifactName()));

            // Combine all checks
            if (typesMatch && artifactIdMatch && artifactNameMatch) {
                return key; // Return the key that matches the criteria
            }
        }

        System.out.println("No compatible key found for action: " + action);
        return null; // Return null if no compatible key is found
    }

}

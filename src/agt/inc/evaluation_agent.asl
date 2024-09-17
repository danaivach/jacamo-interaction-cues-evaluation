@scalability_evaluation
+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & evaluation(scalability, DynamicResolution, false, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum) <-
   !set_up_scalability_eval(EnvUrl, EnvName, DynamicResolution, false, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum).

@scalability_evaluation_bas
+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & basEvaluation(scalability, RecContext, RecAbilities, DynamicResolution, false) <-
   !set_up_scalability_eval(EnvUrl, EnvName, RecContext, RecAbilities, DynamicResolution, false).

@scalability_evaluation_sem
+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & evaluation(scalability, false, true, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum) <-
   !set_up_scalability_eval(EnvUrl, EnvName, false, true, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum);
   .print("Created a Signifier Exposure Artifact");
   makeArtifact("sem", "eval.TimedSignifierExposureArtifact", [], SemId);
   !registerNamespaces(SemId);
   +sem(SemId).

@scalability_evaluation_bas_sem
+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & basEvaluation(scalability, RecContext, true, false, true) <-
   !set_up_scalability_eval(EnvUrl, EnvName, RecContext, true, false, true);
   .wait(4000);
   .print("Created a Signifier Exposure Artifact");
   makeArtifact("sem", "eval.TimedSignifierExposureArtifact", [], SemId);
  .findall(Ability, ability([Ability]), Abilities);
   setAssumedAbilities(Abilities)[artifact_id(SemId)];
   !registerNamespaces(SemId);
   +sem(SemId).

@scalability_setup
+!set_up_scalability_eval(EnvUrl, EnvName, DynamicResolution, DynamicExposure, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum): true <-
   .print("Evaluation-scalability - SRM:", DynamicResolution, ", SEM:", DynamicExposure, ", DynamicPlanLib:", DynamicPlanLib, ", DynamicAbilities:", DynamicAbilities, ", MinSigNum:", MinSigNum, ", MaxSigNum:", MaxSigNum);

    !setNamespace("ex", "https://example.org/");

    ?fileName("scalability", DynamicResolution, DynamicExposure, FileName);
    makeArtifact("logger", "eval.TimeLogger", [MinSigNum, FileName], LoggerId);

    ?vocabulary(Vocabulary);
    makeArtifact("conf", "eval.ScalabilityConf", [EnvUrl, EnvName, "test", Vocabulary, DynamicResolution, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum, true], ConfId);
    linkArtifacts(ConfId, "conf-out", LoggerId);
    focus(ConfId).

@scalability_bas_setup
+!set_up_scalability_eval(EnvUrl, EnvName, RecContext, RecAbilities, DynamicResolution, DynamicExposure): true <-
   .print("Building Automation System - Scalability Evaluation");
   .print("SRM:", DynamicResolution, ", SEM:", DynamicExposure);
   .print("RecommendedContext:", RecContext, ", RecommendedAbilities:", RecAbilities);

    !setNamespace("ex", "https://example.org/");

    ?fileName("scalability", RecContext, RecAbilities, DynamicResolution, DynamicExposure, FileName);
    makeArtifact("logger", "eval.TimeLogger", [0, FileName], LoggerId);

    ?vocabulary(Vocabulary);
    makeArtifact("conf", "eval.ScalabilityConfSmallScaleBAS", [EnvUrl, EnvName, Vocabulary, RecAbilities, DynamicResolution], ConfId);
    linkArtifacts(ConfId, "bas-conf-out", LoggerId);
    focus(ConfId).

@agent_metadata_update[atomic]
+agent_metadata([KnownPlan], AbilityType, true) : true <-
    -+ability(AbilityType);
    .relevant_plans({+!test_goal}, _, LL);
    .remove_plan(LL);
    .add_plan(KnownPlan).

@agent_metadata_ability_only_update[atomic]
+agent_metadata([], AbilityType, false) : true <-
    -+ability(AbilityType).

@agent_metadata_many_plans_update[atomic]
+agent_metadata(KnownPlans, AbilityType, false) : true <-
     -+ability(AbilityType);
    .relevant_plans({+!test_goal}, _, LL);
    .remove_plan(LL);
    .add_plan(KnownPlans).

@artifact_namespace_registration_end
+!registerNamespaces([], ArtId).

@artifact_namespaces_registration_ongoing
+!registerNamespaces([Prefix | Prefixes], ArtId) : namespace(Prefix, Namespace) <-
  setNamespace(Prefix, Namespace)[artifact_id(ArtId)];
  !registerNamespaces(Prefixes, ArtId).

@agent_metadata_preferred_artifact_many_plans[atomic]
+agent_metadata(PreferredArtifact, [], KnownPlans) : true <-
    .term2string(PreferredArtifactTerm, PreferredArtifact);
    -+preferred_artifact(PreferredArtifactTerm);
    .relevant_plans({+!test_goal}, _, LL);
    .remove_plan(LL);
    .add_plan(KnownPlans).

@agent_metadata_preferred_artifact_many_abilities[atomic]
+agent_metadata(PreferredArtifact, Abilities, KnownPlans) : true <-
    !assume_abilities(Abilities);
    .term2string(PreferredArtifactTerm, PreferredArtifact);
    -+preferred_artifact(PreferredArtifactTerm);
    .relevant_plans({+!test_goal}, _, LL);
    .remove_plan(LL);
    .add_plan(KnownPlans).

+!assume_abilities([]).

+!assume_abilities([Ability|Abilities]) : true <-
    +ability([Ability]);
    !assume_abilities(Abilities).

@hypermedia_artifact_instantiation_hmas_dryrun_sem
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : sem(SemId) & vocabulary("https://purl.org/hmas/") <-
    makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI, true, true], ArtId)[wid(WkspId)];
    !registerArtifactNamespace(ArtIRI, ArtName);
    ?web_id(WebId);
    addRecommendationContext(ArtIRI, WebId, RecommendationFilter)[artifact_id(SemId)];
    !registerNamespaces(ArtId);
    focus(ArtId, RecommendationFilter);
    .print("Focused on artifact ", ArtName, " with recommendation filter.").

@hypermedia_artifact_instantiation_bas_hmas_dryrun
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : basEvaluation(_,_,RecAbilities,_,_) & vocabulary("https://purl.org/hmas/") <-
    makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI, RecAbilities, true], ArtId)[wid(WkspId)];
    !registerArtifactNamespace(ArtIRI, ArtName);
    !registerNamespaces(ArtId);
    focus(ArtId).

@hypermedia_artifact_instantiation_hmas_dryrun
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : vocabulary("https://purl.org/hmas/") <-
    makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI, true, true], ArtId)[wid(WkspId)];
    !registerNamespaces(ArtId);
    focus(ArtId).

@hypermedia_artifact_instantiation_wot_dryrun_full_exposure
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : vocabulary("https://www.w3.org/2019/wot/td#") <-
    makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.wot.WebSubThingArtifact", [ArtIRI, true, true], ArtId)[wid(WkspId)];
    !registerNamespaces(ArtId);
    focus(ArtId).

@log_filename_hmas_baseline_setup
+?fileName(EvalType, false, false, FileName) : vocabulary("https://purl.org/hmas/") <-
    .concat(EvalType, "_hmas_00", FileName).

@log_filename_hmas_sem_setup
+?fileName(EvalType, false, true, FileName) : vocabulary("https://purl.org/hmas/") <-
    .concat(EvalType, "_hmas_01", FileName).

@log_filename_hmas_srm_setup
+?fileName(EvalType, true, false, FileName) : vocabulary("https://purl.org/hmas/") <-
    .concat(EvalType, "_hmas_10", FileName).

@log_filename_td_baseline_setup
+?fileName(EvalType, false, false, FileName) : vocabulary("https://www.w3.org/2019/wot/td#") <-
    .concat(EvalType, "_td_00", FileName).

@log_filename_bas_hmas_baseline_setup
+?fileName(EvalType, false, false, false, false, FileName) : vocabulary("https://purl.org/hmas/") <-
    .concat(EvalType, "_hmas_0000", FileName).

@log_filename_bas_hmas_baseline_abilities_setup
+?fileName(EvalType, false, true, false, false, FileName) : vocabulary("https://purl.org/hmas/") <-
    .concat(EvalType, "_hmas_0100", FileName).

@log_filename_bas_hmas_sem_setup
+?fileName(EvalType, false, true, false, true, FileName) : vocabulary("https://purl.org/hmas/") <-
    .concat(EvalType, "_hmas_0101", FileName).

@log_filename_bas_hmas_srm_setup
+?fileName(EvalType, false, true, true, false, FileName) : vocabulary("https://purl.org/hmas/") <-
    .concat(EvalType, "_hmas_0110", FileName).

@log_filename_bas_td_baseline_setup
+?fileName(EvalType, false, false, false, false, FileName) : vocabulary("https://www.w3.org/2019/wot/td#") <-
    .concat(EvalType, "_td_0000", FileName).

+!registerArtifactNamespace(ArtIRI, ArtName) : true <-
    .delete("/#artifact",ArtIRI,BaseIRI);
    .concat(BaseIRI, "/", Namespace);
    !setNamespace(ArtName, Namespace).



@scalability_evaluation
+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & evaluation(scalability, DynamicResolution, false, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum) <-
   !set_up_scalability_eval(EnvUrl, EnvName, DynamicResolution, false, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum).

@scalability_evaluation_bas
+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & evaluation(scalability, DynamicResolution, false) <-
   !set_up_scalability_eval(EnvUrl, EnvName, DynamicResolution, false).

@scalability_evaluation_sem
+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & evaluation(scalability, false, true, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum) <-
   !set_up_scalability_eval(EnvUrl, EnvName, false, true, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum);
   .print("Created a Signifier Exposure Artifact");
   makeArtifact("sem", "org.hyperagents.jacamo.artifacts.eval.TimedSignifierExposureArtifact", [], SemId);
   !registerNamespaces(SemId);
   +sem(SemId).

@scalability_evaluation_bas_sem
+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & evaluation(scalability, false, true) <-
   !set_up_scalability_eval(EnvUrl, EnvName, false, true);
   .print("Created a Signifier Exposure Artifact");
   makeArtifact("sem", "org.hyperagents.jacamo.artifacts.eval.TimedSignifierExposureArtifact", [], SemId);
   !registerNamespaces(SemId);
   +sem(SemId).

@scalability_setup
+!set_up_scalability_eval(EnvUrl, EnvName, DynamicResolution, DynamicExposure, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum): true <-
   .print("Evaluation-scalability - SRM:", DynamicResolution, ", SEM:", DynamicExposure, ", DynamicPlanLib:", DynamicPlanLib, ", DynamicAbilities:", DynamicAbilities, ", MinSigNum:", MinSigNum, ", MaxSigNum:", MaxSigNum);

    !setNamespace("ex", "https://example.org/");

    ?fileName("scalability", DynamicResolution, DynamicExposure, FileName);
    makeArtifact("logger", "org.hyperagents.jacamo.artifacts.eval.TimeLogger", [MinSigNum, FileName], LoggerId);

    ?vocabulary(Vocabulary);
    makeArtifact("conf", "org.hyperagents.jacamo.artifacts.eval.ScalabilityConf", [EnvUrl, EnvName, "test", Vocabulary, DynamicResolution, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum, true], ConfId);
    linkArtifacts(ConfId, "conf-out", LoggerId);
    focus(ConfId).

@scalability_bas_setup
+!set_up_scalability_eval(EnvUrl, EnvName, DynamicResolution, DynamicExposure): true <-
   .print("Building Automation System - Scalability Evaluation - SRM:", DynamicResolution, ", SEM:", DynamicExposure);

    !setNamespace("ex", "https://example.org/");

    ?fileName("scalability", DynamicResolution, DynamicExposure, FileName);
    makeArtifact("logger", "org.hyperagents.jacamo.artifacts.eval.TimeLogger", [0, FileName], LoggerId);

    ?vocabulary(Vocabulary);
    makeArtifact("conf", "org.hyperagents.jacamo.artifacts.eval.ScalabilityConfBASUseCase", [EnvUrl, EnvName, Vocabulary], ConfId);
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

@agent_metadata_preferred_artifact_many_plans[atomic]
+agent_metadata(PreferredArtifact, KnownPlans) : true <-
    .term2string(PreferredArtifactTerm, PreferredArtifact);
    .print(PreferredArtifact);
    .print(PreferredArtifactTerm);
    -+preferred_artifact(PreferredArtifactTerm);
    .relevant_plans({+!test_goal}, _, LL);
    .remove_plan(LL);
    .add_plan(KnownPlans).

@hypermedia_artifact_instantiation_hmas_dryrun_sem
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : sem(SemId) & vocabulary("https://purl.org/hmas/") <-
    makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI, true], ArtId)[wid(WkspId)];
    !registerNamespaces(ArtId);
    ?web_id(WebId);
    addRecommendationContext(ArtIRI, WebId, RecommendationFilter)[artifact_id(SemId)];
    focus(ArtId, RecommendationFilter);
    .print("Focused on artifact ", ArtName, " with recommendation filter.").

@hypermedia_artifact_instantiation_hmas_dryrun
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : vocabulary("https://purl.org/hmas/") <-
    makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI, true], ArtId)[wid(WkspId)];
    !registerNamespaces(ArtId);
    focus(ArtId).

@hypermedia_artifact_instantiation_wot_dryrun_full_exposure
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : vocabulary("https://www.w3.org/2019/wot/td#") <-
    makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.wot.WebSubThingArtifact", [ArtIRI, true, true], ArtId)[wid(WkspId)];
    !registerNamespaces(ArtId);
    focus(ArtId).

@log_file_name_hmas_baseline_setup
+?fileName(EvalType, false, false, FileName) : vocabulary("https://purl.org/hmas/") <-
    .concat(EvalType, "_hmas_00", FileName);
    +fileName(DynamicPlanLib, DynamicAbilities, FileName).

@log_file_name_hmas_sem_setup
+?fileName(EvalType, false, true, FileName) : vocabulary("https://purl.org/hmas/") <-
    .concat(EvalType, "_hmas_01", FileName);
    +fileName(DynamicPlanLib, DynamicAbilities, FileName).

@log_file_name_hmas_srm_setup
+?fileName(EvalType, true, false, FileName) : vocabulary("https://purl.org/hmas/") <-
    .concat(EvalType, "_hmas_10", FileName);
    +fileName(DynamicPlanLib, DynamicAbilities, FileName).

@log_file_name_td_baseline_setup
+?fileName(EvalType, false, false, FileName) : vocabulary("https://www.w3.org/2019/wot/td#") <-
    .concat(EvalType, "_td_00", FileName);
    +fileName(DynamicPlanLib, DynamicAbilities, FileName).


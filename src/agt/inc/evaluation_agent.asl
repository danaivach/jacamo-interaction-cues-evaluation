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
+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & basEvaluation(scalability, RecContext, true, DynamicResolution, true) <-
   !set_up_scalability_eval(EnvUrl, EnvName, RecContext, true, DynamicResolution, true);
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
+!set_up_scalability_eval(EnvUrl, EnvName, RecAbilities, RecContext, DynamicResolution, DynamicExposure): true <-
   .print("Building Automation System - Scalability Evaluation");
   .print("SRM:", DynamicResolution, ", SEM:", DynamicExposure);
   .print("RecommendedAbilities:", RecAbilities, ", RecommendedContext:", RecContext);

    !setNamespace("ex", "https://example.org/");
    !setNamespace("saref", "https://saref.etsi.org/core/");
    !setNamespace("heatingGroupBoard", "http://172.27.52.55:8080/workspaces/61/artifacts/heatingGroupBoard/");

    ?fileName("scalability", RecAbilities, RecContext, DynamicResolution, DynamicExposure, FileName);
    makeArtifact("logger", "eval.TimeLogger", [0, FileName], LoggerId);

    ?vocabulary(Vocabulary);
    makeArtifact("conf", "eval.ScalabilityConfLargeScaleBAS", [EnvUrl, EnvName, Vocabulary, RecAbilities, RecContext, DynamicResolution], ConfId);
    linkArtifacts(ConfId, "bas-conf-out", LoggerId);
    focus(ConfId).

@agent_metadata_many_artifacts_many_plans[atomic]
+agent_metadata([ArtName0, ArtName1, ArtName2, ArtName3, ArtName4], Abilities, KnownPlans) : true <-
    !assume_abilities(Abilities);
    .term2string(ArtName0Term, ArtName0);
    .term2string(ArtName1Term, ArtName1);
    .term2string(ArtName2Term, ArtName2);
    .term2string(ArtName3Term, ArtName3);
    .term2string(ArtName4Term, ArtName4);
    -+preferred_artifacts(ArtName0Term, ArtName1Term, ArtName2Term, ArtName3Term, ArtName4Term);
    .relevant_plans({+!test_goal}, _, LL);
    .remove_plan(LL);
    .add_plan(KnownPlans).

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
+agent_metadata(referredArtifact, [], KnownPlans) : true <-
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
    .wait(5000);
    +ability([Ability]);
    !assume_abilities(Abilities).

@artifact_discovery_custom[atomic]
+artifact(ArtIRI, ArtName, ArtTypes)[workspace(WkspName,_)] : true <-
  //.print("Discovered artifact (name: ", ArtName ,") with types ", ArtTypes, " in workspace ", WkspName, ": ", ArtIRI);
  ?joinedWsp(WkspId, WkspNameTerm, WkspName);

  // Create a hypermedia ResourceArtifact for this artifact.
  !makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId);

  // Set WebId
  ?web_id(WebId);
  setOperatorWebId(WebId)[artifact_id(ArtId)];

  // Register to the ResourceArtifact for notifications
  !registerForWebSub(ArtName, ArtId);

  .term2string(WkspNameTerm, WkspNameStr);
  ?workspace(WkspIRI, WkspNameStr);

  // Focus on the ResourceArtifact to observe its properties and events
  //registerArtifactForFocus(WkspIRI, ArtIRI, ArtId, ArtName);

  .print("Created artifact ", ArtName).

@hypermedia_artifact_instantiation_hmas_dryrun_sem
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : sem(SemId) & vocabulary("https://purl.org/hmas/") <-
    makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI, true, true], ArtId)[wid(WkspId)];
    !registerArtifactNamespace(ArtIRI, ArtName);
    !registerNamespaces(ArtId);

    // Observe artifact state
    .delete("/#artifact", ArtIRI, BaseIRI);
    registerArtifactForWebSub(BaseIRI, SemId, "http://172.27.52.55:5000/observe");
    registerArtifactForWebSub(BaseIRI, ArtId, "http://172.27.52.55:5000/observe");
    .wait(1000);

    // Retrieve recommendation filter
    ?web_id(WebId);
    addRecommendationContext(ArtIRI, WebId, RecommendationFilter)[artifact_id(SemId)];

    // Focus on artifact with recommendation filter
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
    .delete("/#artifact", ArtIRI, BaseIRI);
    registerArtifactForWebSub(BaseIRI, ArtId, "http://172.27.52.55:5000/observe");
    focus(ArtId).

+!registerArtifactNamespace(ArtIRI, ArtName) : true <-
    .delete("/#artifact",ArtIRI,BaseIRI);
    .concat(BaseIRI, "/", Namespace);
    !setNamespace(ArtName, Namespace).

@websub_registration_custom_component_hub
+!registerForWebSub(ArtName, ArtId) : true <-
  ?websub(HubIRI, TopicIRI)[artifact_id(ArtId)];
  registerArtifactForWebSub(TopicIRI, ArtId, HubIRI).

{ include("results_handler_agent.asl") }



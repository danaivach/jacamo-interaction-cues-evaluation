+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & evaluation(scalability, false, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum) <-
   !set_up_scalability_eval(EnvUrl, EnvName, false, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum).

@scalability_evaluation_sem
+!start_evaluation(EnvUrl, EnvName): web_id(WebId) & evaluation(scalability, true, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum) <-
   !set_up_scalability_eval(EnvUrl, EnvName, true, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum);
   +sem.

@scalability_setup
+!set_up_scalability_eval(EnvUrl, EnvName, DynamicExposure, DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum): true <-
   .print("Evaluation-scalability, SEM:", DynamicExposure, " DynamicPlanLib:", DynamicPlanLib, ", DynamicAbilities:", DynamicAbilities, ", MinSigNum:", MinSigNum, ", MaxSigNum:", MaxSigNum);

    !setNamespace("ex", "https://example.org/");

    ?fileName("scalability", DynamicExposure, DynamicPlanLib, DynamicAbilities, FileName);
    makeArtifact("logger", "eval.TimeLogger", [MinSigNum, FileName], LoggerId);
    makeArtifact("conf", "eval.ScalabilityConf", [EnvUrl, EnvName, "test", DynamicPlanLib, DynamicAbilities, MinSigNum, MaxSigNum, true], ConfId);
    linkArtifacts(ConfId, "conf-out", LoggerId);
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

@workspace_discovery_custom[atomic]
+workspace(WkspIRI, WkspName) : true <-
    .print("Discovered workspace (name: ", WkspName ,"): ", WkspIRI);

    // Create a CArtAgO Workspace that will contain the hypermedia WorkspaceArtifact and its contained artifacts.
    //createWorkspace(WkspName);

    // Join the CArtAgO Workspace
    //!joinWorkspace(WkspName, WkspId);

    // Create a hypermedia WorkspaceArtifact for this workspace.
    // Used for some operations (e.g., to create an artifact).
    .term2string(WkspNameTerm, WkspName);
    ?joinedWsp(WkspId, WkspNameTerm,_);
    !makeMirroringWorkspace(WkspIRI, WkspName, WkspId);

    // Join the hypermedia WorkspaceArtifact
    !joinHypermediaWorkspace(WkspName);

    .print("Created workspace artifact ", WkspName, ", joined, and registered for notifications").

@artifact_discovery_custom[atomic]
+artifact(ArtIRI, ArtName, ArtTypes)[workspace(WkspName,_)] : true <-
    .print("Discovered artifact (name: ", ArtName ,") with types ", ArtTypes, " in workspace ", WkspName, ": ", ArtIRI);
    ?joinedWsp(WkspId, WkspNameTerm, WkspName);

    // Create a hypermedia ResourceArtifact for this artifact.
    !makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId);

    // Register to the ResourceArtifact for notifications
    ?websub(HubIRI, TopicIRI)[artifact_id(ArtId)];
    registerArtifactForWebSub(TopicIRI, ArtId, HubIRI);
    .print("Created artifact ", ArtName, ", and registered for notifications").

@hypermedia_artifact_instantiation_hmas_dryrun_sem
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : sem & vocabulary("https://purl.org/hmas/") & web_id(WebId) <-
    makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI, true], ArtId)[wid(WkspId)];
    !registerNamespaces(ArtId);
    setOperatorWebId(WebId)[artifact_id(ArtId)];
    .print("making SEM");
    makeArtifact("sem", "eval.TimedSignifierExposureArtifact", [], SemId);
    !registerNamespaces(SemId);
    .print("adjusting for agent");
    addRecommendationContext(ArtIRI, "agent-profile.ttl", RecommendationFilter);
    focus(ArtId, RecommendationFilter).

@hypermedia_artifact_instantiation_hmas_dryrun
+!makeMirroringArtifact(ArtIRI, ArtName, ArtId, WkspId) : vocabulary("https://purl.org/hmas/") & web_id(WebId) <-
    makeArtifact(ArtName, "org.hyperagents.jacamo.artifacts.hmas.WebSubResourceArtifact", [ArtIRI, true], ArtId)[wid(WkspId)];
    !registerNamespaces(ArtId);
    setOperatorWebId(WebId)[artifact_id(ArtId)];
    focus(ArtId).

+?fileName(EvalType, DynamicExposure, DynamicPlanLib, DynamicAbilities, FileName) : DynamicExposure <-
    .concat(EvalType, "_100", FileName);
    +fileName(DynamicPlanLib, DynamicAbilities, FileName).

+?fileName(EvalType, DynamicExposure, DynamicPlanLib, DynamicAbilities, FileName) : DynamicPlanLib & DynamicAbilities <-
    .concat(EvalType, "_011", FileName);
    +fileName(DynamicPlanLib, DynamicAbilities, FileName).

+?fileName(EvalType, DynamicExposure, DynamicPlanLib, DynamicAbilities, FileName) : not DynamicPlanLib & DynamicAbilities <-
    .concat(EvalType, "_001", FileName);
    +fileName(DynamicPlanLib, DynamicAbilities, FileName).

+?fileName(EvalType, DynamicExposure, DynamicPlanLib, DynamicAbilities, FileName) : not DynamicPlanLib & not DynamicAbilities <-
    .concat(EvalType, "_000", FileName);
    +fileName(DynamicPlanLib, DynamicAbilities, FileName).

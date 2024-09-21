// Agent sample_agent in project main

/* Initial beliefs and rules */

entry_url("http://172.27.52.55:8080/workspaces/61").

env_name("61").

fullRooms(AvailableArtifacts) :-
    .length(AvailableArtifacts, ArtifactsNum) &
    (ArtifactsNum mod 12 == 0 |
        (
            .member(61,AvailableArtifacts) &
            (ArtifactsNum - 1) mod 12 == 0
        )
    ).

/* Initial goals */

!start.

/* Plans */

+!start : entry_url(EnvUrl) & env_name(EnvName) <-
    .print("hello world");

    // Create local workspace
    createWorkspace(EnvName);
    !joinWorkspace(EnvName,_);

    // Set WebId
    !set_up_web_id(WebId);

    // Start evaluation
    !start_evaluation(EnvUrl, EnvName);

    // Load environment 61
    makeArtifact("notification-server", "org.hyperagents.jacamo.artifacts.yggdrasil.NotificationServerArtifact", ["localhost", 8081], NotifServerId);
    setOperatorWebId(WebId)[artifact_id(NotifServerId)];
    start;
    !load_environment(EnvUrl, "61").

+preferred_artifact(DeviceId) : true <-
    .print("Target device set: ", DeviceId).

+preferred_artifacts(ArtName0, ArtName1, ArtName2, ArtName3, ArtName4) : true <-
    .print("Target components set: ", ArtName0, ", ", ArtName1, ", ", ArtName2, ", ", ArtName3, ", ", ArtName4).

@testing_many_artifacts_setup[atomic]
+exposureState("done") : true <-
    .findall(ArtName, exposureState("done")[artifact_name(ArtName)], AvailableArtifacts);
    !select_test_goal(AvailableArtifacts).

@testing_many_artifacts[atomic]
+!select_test_goal(AvailableArtifacts) : fullRooms(AvailableArtifacts)  <-
    ?preferred_artifacts(ArtName0, ArtName1, ArtName2, ArtName3, ArtName4);
    startTimer;
    !test_goal(ArtName0, ArtName1, ArtName2, ArtName3, ArtName4);
    .drop_all_events;
    deployNewDevice.

@testing_many_artifacts_denied[atomic]
+!select_test_goal(ArtifactsNum) : true .

@testing_one_artifact[atomic]
+exposureState("done") : true <-
    startTimer;
    !test_goal(DeviceId);
    deployNewDevice.

@test_goal
+!test_goal: true <- .print("Initial plan").

@web_id_initialization
+!set_up_web_id(WebId) : .my_name(AgentName) <-
    .concat("https://wiser-solid-xi.interactions.ics.unisg.ch/", AgentName, "/profile/card#me", WebId);
    +web_id(WebId).

{ include("inc/evaluation_agent.asl") }
{ include("inc/hypermedia.asl") }

// Agent sample_agent in project main

/* Initial beliefs and rules */

vocabulary("https://purl.org/hmas/").

entry_url("http://172.27.52.55:8080/workspaces/61").

env_name("61").

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

+setupDone : true <- .print("increasing sigs"); increaseSignifiers.

@testing[atomic]
+exposureState("done")[artifact_name(test)] : true <-
    startTimer;
    !test_goal;
    stopTimerAndLog;
    increaseSignifiers.

@test_goal
+!test_goal : true <- .print("Initial plan").

@web_id_initialization
+!set_up_web_id(WebId) : .my_name(AgentName) <-
    .concat("https://wiser-solid-xi.interactions.ics.unisg.ch/", AgentName, "/profile/card#me", WebId);
    +web_id(WebId).

{ include("inc/evaluation_agent.asl") }
{ include("inc/hypermedia.asl") }

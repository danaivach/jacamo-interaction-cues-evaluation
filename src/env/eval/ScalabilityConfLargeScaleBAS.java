package eval;

import cartago.*;
import ch.unisg.ics.interactions.hmas.core.vocabularies.CORE;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.*;
import ch.unisg.ics.interactions.hmas.interaction.vocabularies.INTERACTION;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;

@ARTIFACT_INFO(
  outports = {
    @OUTPORT(name = "bas-conf-out")
  }
)
public class ScalabilityConfLargeScaleBAS extends Artifact {

  protected static int COMPONENT_ACTION_NUM = 3;
  protected static int ROOM_COMPONENTS_NUM = 12;
  protected static int ROOM_NUM = 400;
  protected static int PLAN_NUM = 3000;
  protected static final String WEB_ID = "https://example.org/env-manager";

  protected static final String[] GENERIC_ACTIONS = {
    "Ignite", "Drench", "Gust", "Stabilize", "Charge", "Freeze", "Forge", "Enshroud", "Illuminate", "Disperse"
  };

  protected static final String[] GENERIC_ABILITIES = {
    "Pyromancy", "Hydromancy", "Aeromancy", "Geomancy", "Electromancy",
    "Cryomancy", "Metallurgy", "Shadowcraft", "Lumomancy", "Voidweaving"
  };

  private static final Map<String, Map<String, String>> planActionToArtifactMap = new HashMap<>();

  static {
    // Plan 1 Action to Artifact mappings
    Map<String, String> plan1ActionMap = new HashMap<>();
    plan1ActionMap.put("Action0A", "ArtName0");
    plan1ActionMap.put("Action0C", "ArtName0");
    plan1ActionMap.put("Action3B", "ArtName1");
    plan1ActionMap.put("Action3C", "ArtName1");
    plan1ActionMap.put("Action7A", "ArtName2");
    plan1ActionMap.put("Action7B", "ArtName2");
    plan1ActionMap.put("Action5C", "ArtName3");
    plan1ActionMap.put("Action5A", "ArtName3");
    plan1ActionMap.put("Action9A", "ArtName4");
    plan1ActionMap.put("Action9C", "ArtName4");
    planActionToArtifactMap.put("Plan1", plan1ActionMap);

    // Plan 2 Action to Artifact mappings
    Map<String, String> plan2ActionMap = new HashMap<>();
    plan2ActionMap.put("Action1B", "ArtName0");
    plan2ActionMap.put("Action1A", "ArtName0");
    plan2ActionMap.put("Action4A", "ArtName1");
    plan2ActionMap.put("Action4C", "ArtName1");
    plan2ActionMap.put("Action10A", "ArtName2");
    plan2ActionMap.put("Action10B", "ArtName2");
    plan2ActionMap.put("Action8C", "ArtName3");
    plan2ActionMap.put("Action8A", "ArtName3");
    plan2ActionMap.put("Action2B", "ArtName4");
    plan2ActionMap.put("Action2C", "ArtName4");
    planActionToArtifactMap.put("Plan2", plan2ActionMap);

    // Plan 3 Action to Artifact mappings
    Map<String, String> plan3ActionMap = new HashMap<>();
    plan3ActionMap.put("Action6A", "ArtName0");
    plan3ActionMap.put("Action6C", "ArtName0");
    plan3ActionMap.put("Action11B", "ArtName1");
    plan3ActionMap.put("Action11C", "ArtName1");
    plan3ActionMap.put("Action0B", "ArtName2");
    plan3ActionMap.put("Action0A", "ArtName2");
    plan3ActionMap.put("Action3A", "ArtName3");
    plan3ActionMap.put("Action3C", "ArtName3");
    plan3ActionMap.put("Action9B", "ArtName4");
    plan3ActionMap.put("Action9C", "ArtName4");
    planActionToArtifactMap.put("Plan3", plan3ActionMap);
  }

  protected static final Namespace JACAMO_NS = new SimpleNamespace("jacamo", "https://purl.org/hmas/jacamo/");
  protected static final Namespace EX_NS = new SimpleNamespace("ex", "https://example.org/");
  protected static String getCurie(String str, Namespace ns) {
    return str.replace(ns.getName(), ns.getPrefix() + ":");
  }

  protected final Random random = new Random();

  protected List<String> plans;

  protected String envURL;
  protected String workspaceName;
  protected String vocabulary;
  protected boolean adjustedExposure;
  protected boolean concisePlans;
  protected int registeredRoomNum;
  protected int registeredDeviceNum;
  protected List<String> agentAbilities;
  protected HashMap<Integer, List<ThingDescription.Builder>> roomTDBuilders;
  protected HashMap<Integer, List<ResourceProfile.Builder>> roomProfileBuilders;



  public void init(String url, String workspaceName, String vocabulary, boolean adjustedExposure, boolean concisePlans) {
    this.envURL = url;
    this.workspaceName = workspaceName;
    this.adjustedExposure = adjustedExposure;
    this.concisePlans = concisePlans;
    this.registeredRoomNum = 0;
    this.registeredDeviceNum = 0;
    this.agentAbilities = new ArrayList<>();

    if ("hmas".equals(vocabulary) || "https://purl.org/hmas/".equals(vocabulary)) {
      this.vocabulary = "hmas";
      this.roomProfileBuilders = new HashMap<>();
      this.createResourceProfiles();
    } else if ("td".equals(vocabulary) || "https://www.w3.org/2019/wot/td#".equals(vocabulary)) {
      this.vocabulary = "td";
      this.roomTDBuilders = new HashMap<>();
      this.createTDs();
    } else {
      failed("Unknown vocabulary. Please, select either \"hmas\" for testing with hMAS Resource Profiles," +
        "or \"td\" for testing with W3C Web of Things Thing Descriptions.");
    }

    this.initKnownPlans(this.registeredRoomNum);
    this.registerRoom(this.registeredRoomNum);
  }

  @OPERATION
  private void deployNewDevice() {
    if (this.registeredRoomNum < ROOM_NUM) {
      this.registeredRoomNum++;
      this.registerRoom(this.registeredRoomNum);
      System.out.println("room num:" + this.registeredRoomNum);

      try {
        execLinkedOp("bas-conf-out", "setRound", this.registeredRoomNum);
      } catch (OperationException e) {
        throw new RuntimeException(e);
      }
    }
  }


  private void initKnownPlans(Integer artifactIndex) {
    this.plans = new ArrayList<>();

    List<String> targetPlans = this.getTargetPlans();
    this.plans.addAll(targetPlans);

    for (int level = 0; level < PLAN_NUM/GENERIC_ACTIONS.length ; level++) {
      for (int j = 0; j < GENERIC_ACTIONS.length; j++) {
        String action = GENERIC_ACTIONS[j];
        this.plans.add(this.getGenericPlan(action + level));
      }
    }

    Collections.shuffle(this.plans);

    this.agentAbilities.add("heatingGroupBoard:#Room0OperatorAbility");

    List<String> preferredArtifacts = Arrays.asList("component1", "component4", "component10", "component8", "component2");
    this.defineObsProperty("agent_metadata", preferredArtifacts.toArray(), this.agentAbilities.toArray(), this.plans.toArray());
  }

  private void registerRoom(int roomIndex) {

    String profileStr = null;
    String slug = null;

    if ("hmas".equals(this.vocabulary)) {
      List<ResourceProfile.Builder> profileBuilders = roomProfileBuilders.get(roomIndex);
      for (ResourceProfile.Builder builder : profileBuilders) {
        ResourceProfile profile = builder.build();
        String profileUrl = profile.getIRIAsString().get();
        int startIndex = profileUrl.indexOf("artifacts/") + "artifacts/".length();
        slug = profileUrl.substring(startIndex);
        profileStr = new ResourceProfileGraphWriter(profile).write();
        registerArtifact(profileStr, slug);
      }
    } else if ("td".equals(this.vocabulary)) {
      List<ThingDescription.Builder> TDBuilders = roomTDBuilders.get(roomIndex);
      for (ThingDescription.Builder builder : TDBuilders) {
        ThingDescription td = builder.build();
        slug = td.getTitle();
        profileStr = new TDGraphWriter(td).write();
        registerArtifact(profileStr, slug);
      }
    }
  }

  private void registerArtifact(String profileStr, String slug) {
    if (profileStr != null) {
      HttpClient client = new HttpClient();
      try {
        client.start();
        ContentResponse response = client.POST(this.envURL + "/artifacts/")
                .content(new StringContentProvider(profileStr), "text/turtle")
                .header("X-Agent-WebID", WEB_ID)
                .header("Slug", slug).send();
        client.stop();

        if (response.getStatus() != HttpStatus.SC_CREATED) {
          log("Request failed: " + response.getStatus());
        } else {
          this.registeredDeviceNum++;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void createTDs() {
    int componentIndex = 0;
    for (int room = 0; room < ROOM_NUM; room++) {
      List<ThingDescription.Builder> TDBuilders = new ArrayList<>();
      for (int roomComponent = 0; roomComponent < ROOM_COMPONENTS_NUM; roomComponent ++) {
        String profileURIStr = this.envURL + "/artifacts/component" + componentIndex;
        SimpleValueFactory rdf = SimpleValueFactory.getInstance();
        IRI profileURI = rdf.createIRI(profileURIStr);
        IRI thingURI = rdf.createIRI(profileURIStr + "/#artifact");
        ThingDescription.Builder builder = new ThingDescription.Builder("component" + componentIndex)
                .addThingURI(profileURIStr + "/#artifact")
                .addTriple(profileURI, CORE.IS_PROFILE_OF, thingURI)
                .addTriple(profileURI, RDF.TYPE, CORE.RESOURCE_PROFILE)
                .addTriple(thingURI, CORE.HAS_PROFILE, profileURI)
                .addSemanticType(EX_NS.getName() + "Component" + roomComponent);

        for (int action = 0; action < COMPONENT_ACTION_NUM; action++) {
          char actionLetter = (char) (action + 1 + 64);
          ActionAffordance aff = new ActionAffordance.Builder("action" + action,
                  new ch.unisg.ics.interactions.wot.td.affordances.Form
                          .Builder("http://localhost:8000/" + "component" + componentIndex + "/action" + action)
                          .setMethodName("POST").build())
                  .addSemanticType(EX_NS.getName() + "Action" + roomComponent + actionLetter)
                  .build();

          builder.addAction(aff);
        }
        List<ActionAffordance> defaultAffs = getDefaultAffordances("component" + componentIndex);
        builder.addActions(defaultAffs);
        TDBuilders.add(builder);
        componentIndex++;
      }
      this.roomTDBuilders.put(room, TDBuilders);
    }
  }

  private void createResourceProfiles() {
    int componentIndex = 0;
    for (int room = 0; room < ROOM_NUM; room++) {
      List<ResourceProfile.Builder> profileBuilders = new ArrayList<>();
      for (int roomComponent = 0; roomComponent < ROOM_COMPONENTS_NUM; roomComponent ++) {
        String profileURIStr = this.envURL + "/artifacts/component" + componentIndex;
        SimpleValueFactory rdf = SimpleValueFactory.getInstance();
        BNode creatorNode = rdf.createBNode();
        BNode subscriberNode = rdf.createBNode();
        BNode observerNode = rdf.createBNode();

        ResourceProfile.Builder builder = new ResourceProfile.Builder(new ch.unisg.ics.interactions.hmas.core.hostables.Artifact
                .Builder()
                .setIRIAsString(profileURIStr + "/#artifact")
                .addSemanticType(EX_NS.getName() + "Component" + roomComponent)
                .build())
                .setIRIAsString(profileURIStr)
                .addTriple(rdf.createIRI(profileURIStr + "/#deleteArtifact"), INTERACTION.RECOMMENDS_ABILITY, creatorNode)
                .addTriple(rdf.createIRI(profileURIStr + "/#updateArtifact"), INTERACTION.RECOMMENDS_ABILITY, creatorNode)
                .addTriple(rdf.createIRI(profileURIStr + "/#subscribeToArtifact"), INTERACTION.RECOMMENDS_ABILITY, subscriberNode)
                .addTriple(rdf.createIRI(profileURIStr + "/#unsubscribeFromArtifact"), INTERACTION.RECOMMENDS_ABILITY, subscriberNode)
                .addTriple(creatorNode, RDF.TYPE, INTERACTION.ABILITY)
                .addTriple(subscriberNode, RDF.TYPE, INTERACTION.ABILITY)
                .addTriple(observerNode, RDF.TYPE, INTERACTION.ABILITY)
                .addTriple(creatorNode, RDF.TYPE, rdf.createIRI(profileURIStr + "/#CreateArtifactAbility"))
                .addTriple(subscriberNode, RDF.TYPE, rdf.createIRI(profileURIStr + "/#SubscribeAbility"))
                .addTriple(observerNode, RDF.TYPE, rdf.createIRI(profileURIStr + "/#ObserverAbility"));

        for (int action = 0; action < COMPONENT_ACTION_NUM; action++) {
          char actionLetter = (char) (action + 1 + 64);
          ActionSpecification spec = new ActionSpecification.Builder(new Form
                  .Builder("http://localhost:8000/" + "component" + componentIndex + "/action" + action)
                  .setMethodName("POST")
                  .build())
                  .addRequiredSemanticType(EX_NS.getName() + "Action" + roomComponent + actionLetter)
                  .build();

          Signifier sig = new Signifier.Builder(spec)
                  .addRecommendedAbility(new Ability
                          .Builder()
                          .addSemanticType(this.envURL + "/artifacts/heatingGroupBoard/#Room" + room + "OperatorAbility")
                          .build())
                  .setIRIAsString(profileURIStr + "/#" + roomComponent + actionLetter)
                  .build();

          builder.exposeSignifier(sig);
        }
        profileBuilders.add(builder);
        componentIndex++;
      }
      this.roomProfileBuilders.put(room, profileBuilders);
    }
  }

  private String getGenericPlan(String action) {
    String applicationContext = "true";
    String planAnnot = "[artifact_name(test), wsp("
      + this.workspaceName + ")]";

    String actionType = EX_NS.getPrefix() + ":" + action;

    if ("hmas".equals(this.vocabulary)) {
      applicationContext = "signifier([\"" + actionType + "\"], [Ability], _)";
    } else if ("td".equals(this.vocabulary)) {
      applicationContext = "affordance([\"" + actionType + "\"])";
    }

    return "@test_goal_" + action.toLowerCase() + " +!test_goal_" + action + " : " + applicationContext +
      " <- invokeAction(\"" + actionType + "\")" + planAnnot + ". ";
  }

  private String generateApplicationContext(String[] actions, Map<String, String> actionToArtifactMap) {
    StringBuilder applicationContext = new StringBuilder();

    for (int i = 0; i < actions.length; i++) {
      String action = actions[i];
      String artifactName = actionToArtifactMap.get(action);

      // Generate the context for the current action
      String contextForAction = generateContextForAction(action, artifactName, i);

      // Append the generated context
      if (!applicationContext.isEmpty() && !applicationContext.toString().equals("true")) {
        applicationContext.append(" & "); // Add delimiter if not the first context
      }
      applicationContext.append(contextForAction);
      if (this.concisePlans) {
        break;
      }
    }

    return applicationContext.toString().trim();
  }

  private String generateContextForAction(String action, String artifactName, int abilitySuffix) {
    StringBuilder context = new StringBuilder();

    String prefixedAction = EX_NS.getPrefix() + ":" + action;

    if ("hmas".equals(this.vocabulary)) {
      if (concisePlans) {
        return "true";
      } else {
        if (adjustedExposure) {
          context.append(String.format("ability(Abilities%s) & signifier([\"%s\"], Abilities%s, _)[artifact_name(%s)] ",
                  abilitySuffix, prefixedAction, abilitySuffix, artifactName));
        } else {
          context.append(String.format("signifier([\"%s\"][artifact_name(%s)] ", prefixedAction, artifactName));
        }
      }
    } else if ("td".equals(this.vocabulary)) {
      context.append(String.format("affordance([\"%s\"])[artifact_name(%s)] ", prefixedAction, artifactName));
    }

    return context.toString();
  }

  private String generateActionStatements(String[] actions, Map<String, String> actionToArtifactMap) {
    StringBuilder actionStatements = new StringBuilder();

    for (String action : actions) {
      String prefixedAction = EX_NS.getPrefix() + ":" + action;
      String artifactName = actionToArtifactMap.get(action);
      actionStatements.append("invokeAction(\"").append(prefixedAction).append("\")[artifact_name(").append(artifactName).append("), wsp(")
              .append(workspaceName).append(")]; ");
    }

    return actionStatements.toString().trim();
  }

  private String generatePlan(String planName, String[] actions) {
    Map<String, String> actionToArtifactMap = planActionToArtifactMap.get(planName);
    if (actionToArtifactMap == null) {
      throw new IllegalArgumentException("Invalid planName: " + planName);
    }

    String applicationContext = generateApplicationContext(actions, actionToArtifactMap);
    String actionStatements = generateActionStatements(actions, actionToArtifactMap);

    return "@test_goal_" + planName + " +!test_goal(ArtName0, ArtName1, ArtName2, ArtName3, ArtName4) : " + applicationContext +
            "<- stopTimerAndLog; " + actionStatements + ".";
  }

  public List<String> getTargetPlans() {
    List<String> plans = new ArrayList<>();

    String[] plan1Actions = {
            "Action0A", "Action0C",
            "Action3B", "Action3C",
            "Action7A", "Action7B",
            "Action5C", "Action5A",
            "Action9A", "Action9C"
    };

    String[] plan2Actions = {
            "Action1B", "Action1A",
            "Action4A", "Action4C",
            "Action10A", "Action10B",
            "Action8C", "Action8A",
            "Action2B", "Action2C"
    };

    String[] plan3Actions = {
            "Action6A", "Action6C",
            "Action11B", "Action11C",
            "Action0B", "Action0A",
            "Action3A", "Action3C",
            "Action9B", "Action9C"
    };

    plans.add(generatePlan("Plan1", plan1Actions));
    plans.add(generatePlan("Plan2", plan2Actions));
    plans.add(generatePlan("Plan3", plan3Actions));

    return plans;
  }

  private static List<ActionAffordance> getDefaultAffordances(String artifactName) {
    List<ActionAffordance> defaultAffordances = new ArrayList<>();

    ActionAffordance deleteAff = new ActionAffordance.Builder("deleteArtifact",
            new ch.unisg.ics.interactions.wot.td.affordances.Form
                    .Builder("http://localhost:8000/" + artifactName + "/deleteArtifact")
                    .setMethodName("DELETE").build())
            .addSemanticType(JACAMO_NS.getName() + "DeleteArtifact")
            .build();

    ActionAffordance updateAff = new ActionAffordance.Builder("updateArtifact",
            new ch.unisg.ics.interactions.wot.td.affordances.Form
                    .Builder("http://localhost:8000/" + artifactName + "/updateArtifact")
                    .setMethodName("PUT").build())
            .addSemanticType(JACAMO_NS.getName() + "UpdateArtifact")
            .build();

    ActionAffordance perceiveAff = new ActionAffordance.Builder("perceiveArtifact",
            new ch.unisg.ics.interactions.wot.td.affordances.Form
                    .Builder("http://localhost:8000/" + artifactName + "/perceiveArtifact")
                    .setMethodName("GET").build())
            .addSemanticType(JACAMO_NS.getName() + "PerceiveArtifact")
            .build();

    defaultAffordances.addAll(Arrays.asList(deleteAff, updateAff, perceiveAff));
    return defaultAffordances;
  }
}


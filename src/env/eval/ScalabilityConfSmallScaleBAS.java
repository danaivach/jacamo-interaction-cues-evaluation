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
public class ScalabilityConfSmallScaleBAS extends Artifact {

  protected static int HC_DEVICES_NUM = 500;
  protected static int CC_DEVICES_NUM = 500;
  protected static int HC_SIGNIFIERS_NUM = 3;
  protected static int CC_SIGNIFIERS_NUM = 4;
  protected static int PLAN_NUM = 3000;
  protected static final String WEB_ID = "https://example.org/env-manager";

  protected static final String[] GENERIC_ACTIONS = {
    "Ignite", "Drench", "Gust", "Stabilize", "Charge", "Freeze", "Forge", "Enshroud", "Illuminate", "Disperse"
  };

  protected static final String[] GENERIC_ABILITIES = {
    "Pyromancy", "Hydromancy", "Aeromancy", "Geomancy", "Electromancy",
    "Cryomancy", "Metallurgy", "Shadowcraft", "Lumomancy", "Voidweaving"
  };

  protected static final Namespace JACAMO_NS = new SimpleNamespace("jacamo", "https://purl.org/hmas/jacamo/");
  protected static final Namespace EX_NS = new SimpleNamespace("ex", "https://example.org/");
  protected static String getCurie(String str, Namespace ns) {
    return str.replace(ns.getName(), ns.getPrefix() + ":");
  }

  protected final Random random = new Random();

  protected List<ResourceProfile.Builder> HMASProfileBuilders;
  protected List<ThingDescription.Builder> TDBuilders;
  protected List<String> plans;

  protected String envURL;
  protected String workspaceName;
  protected String vocabulary;
  protected boolean adjustedExposure;
  protected boolean concisePlans;
  protected int registeredDevicesNum;
  protected List<String> agentAbilities;

  public void init(String url, String workspaceName, String vocabulary, boolean adjustedExposure, boolean concisePlans) {
    this.envURL = url;
    this.workspaceName = workspaceName;
    this.adjustedExposure = adjustedExposure;
    this.concisePlans = concisePlans;
    this.registeredDevicesNum = 0;
    this.agentAbilities = new ArrayList<>();

    Integer artifactIndex = null;
    if ("hmas".equals(vocabulary) || "https://purl.org/hmas/".equals(vocabulary)) {
      this.vocabulary = "hmas";
      this.HMASProfileBuilders = new ArrayList<>();
      this.initAllResourceProfiles();
      artifactIndex = random.nextInt(HMASProfileBuilders.size());
    } else if ("td".equals(vocabulary) || "https://www.w3.org/2019/wot/td#".equals(vocabulary)) {
      this.vocabulary = "td";
      this.TDBuilders = new ArrayList<>();
      this.initAllTDs();
      artifactIndex = random.nextInt(TDBuilders.size());
    } else {
      failed("Unknown vocabulary. Please, select either \"hmas\" for testing with hMAS Resource Profiles," +
        "or \"td\" for testing with W3C Web of Things Thing Descriptions.");
    }

    this.initKnownPlans(artifactIndex);
    this.registerArtifact(artifactIndex);
    System.out.println(this.registeredDevicesNum);
  }

  @OPERATION
  private void deployNewDevice() {
    if (this.registeredDevicesNum < HC_DEVICES_NUM + CC_DEVICES_NUM -1) {
      Integer artifactIndex = null;
      if ("hmas".equals(this.vocabulary)) {
        artifactIndex = random.nextInt(HMASProfileBuilders.size());
      } else if ("td".equals(this.vocabulary)) {
        artifactIndex = random.nextInt(TDBuilders.size());
      }

      this.registerArtifact(artifactIndex);
      System.out.println(this.registeredDevicesNum);


      try {
        execLinkedOp("bas-conf-out", "setRound", this.registeredDevicesNum);
      } catch (OperationException e) {
        throw new RuntimeException(e);
      }
    }
  }

  private void initAllResourceProfiles() {
    createResourceProfiles(HC_DEVICES_NUM, HC_SIGNIFIERS_NUM,  0, "HeatingController", "HeatingActionType" );
    createResourceProfiles(CC_DEVICES_NUM, CC_SIGNIFIERS_NUM, HC_DEVICES_NUM,"CoolingController", "CoolingActionType");
  }

  private void initAllTDs() {
    createTDs(HC_DEVICES_NUM, HC_SIGNIFIERS_NUM,  0, "HeatingController", "HeatingActionType" );
    createTDs(CC_DEVICES_NUM, CC_SIGNIFIERS_NUM,  HC_DEVICES_NUM, "CoolingController", "CoolingActionType" );

  }

  private void initKnownPlans(Integer artifactIndex) {
    this.plans = new ArrayList<>();

    String targetPlan = this.getTargetPlan(artifactIndex);
    this.plans.add(targetPlan);

    for (int level = 0; level < PLAN_NUM/GENERIC_ACTIONS.length ; level++) {
      for (int j = 0; j < GENERIC_ACTIONS.length; j++) {
        String action = GENERIC_ACTIONS[j];
        this.plans.add(this.getGenericPlan(action + level));
      }
    }

    Collections.shuffle(this.plans);
    String preferredArtifact = "controller" + artifactIndex;
    this.defineObsProperty("agent_metadata", preferredArtifact, this.agentAbilities.toArray(), this.plans.toArray());
  }

  private void registerArtifact(int artifactIndex) {

    String profileStr = null;
    String slug = null;

    if ("hmas".equals(this.vocabulary)) {
      ResourceProfile.Builder randomProfileBuilder = HMASProfileBuilders.get(artifactIndex);
      ResourceProfile profile = randomProfileBuilder.build();
      String profileUrl = profile.getIRIAsString().get();
      int startIndex = profileUrl.indexOf("artifacts/") + "artifacts/".length();
      slug = profileUrl.substring(startIndex);
      profileStr = new ResourceProfileGraphWriter(profile).write();
      HMASProfileBuilders.remove(HMASProfileBuilders.get(artifactIndex));
    } else if ("td".equals(this.vocabulary)) {
      ThingDescription.Builder randomTDBuilder = TDBuilders.get(artifactIndex);
      ThingDescription td = randomTDBuilder.build();
      slug = td.getTitle();
      profileStr = new TDGraphWriter(td).write();
      TDBuilders.remove(TDBuilders.get(artifactIndex));
    }

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
          this.registeredDevicesNum++;
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void createTDs(int deviceCount, int signifierCount, int startIndex, String controllerType, String actionTypePrefix) {
    for (int dNum = 0; dNum < deviceCount; dNum++) {
      String profileURIStr = this.envURL + "/artifacts/controller" + (startIndex + dNum);
      SimpleValueFactory rdf = SimpleValueFactory.getInstance();
      IRI profileURI = rdf.createIRI(profileURIStr);
      IRI thingURI = rdf.createIRI(profileURIStr + "/#artifact");
      ThingDescription.Builder builder = new ThingDescription.Builder("controller" + (startIndex + dNum))
        .addThingURI(profileURIStr + "/#artifact")
              .addTriple(profileURI, CORE.IS_PROFILE_OF, thingURI)
              .addTriple(profileURI, RDF.TYPE, CORE.RESOURCE_PROFILE)
              .addTriple(thingURI, CORE.HAS_PROFILE, profileURI)
        .addSemanticType(EX_NS.getName() + controllerType);

      for (int sNum = 0; sNum < signifierCount; sNum++) {
        ActionAffordance aff = new ActionAffordance.Builder("action" + sNum,
          new ch.unisg.ics.interactions.wot.td.affordances.Form
            .Builder("http://localhost:8000/" + controllerType + dNum + "/action" + sNum)
            .setMethodName("POST").build())
          .addSemanticType(EX_NS.getName() + actionTypePrefix + sNum)
          .build();

        builder.addAction(aff);
      }
      List<ActionAffordance> defaultAffs = getDefaultAffordances(controllerType + dNum);
      builder.addActions(defaultAffs);
      TDBuilders.add(builder);
    }
  }

  private void createResourceProfiles(int deviceCount, int signifierCount, int startIndex, String controllerType, String actionTypePrefix) {
    for (int dNum = 0; dNum < deviceCount; dNum++) {
      String profileURIStr = this.envURL + "/artifacts/controller" + (startIndex + dNum);
      SimpleValueFactory rdf = SimpleValueFactory.getInstance();
      IRI profileURI = rdf.createIRI(profileURIStr);
      //"http://172.27.52.55:8080/workspaces/61/artifacts/controller1436/#deleteArtifact"

      BNode creatorNode = rdf.createBNode();
      BNode subscriberNode = rdf.createBNode();
      BNode observerNode = rdf.createBNode();
      ResourceProfile.Builder builder = new ResourceProfile.Builder(new ch.unisg.ics.interactions.hmas.core.hostables.Artifact
        .Builder()
        .setIRIAsString(profileURIStr + "/#artifact")
        .addSemanticType(EX_NS.getName() + controllerType)
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

      for (int sNum = 0; sNum < signifierCount; sNum++) {
        ActionSpecification spec = new ActionSpecification.Builder(new Form
          .Builder("http://localhost:8000/" + controllerType + dNum + "/action" + sNum)
          .setMethodName("POST")
          .build())
          .addRequiredSemanticType(EX_NS.getName() + actionTypePrefix + sNum)
          .build();

        Signifier sig = new Signifier.Builder(spec)
          .addRecommendedAbility(new Ability
            .Builder()
            .addSemanticType(profileURIStr + "/#" + controllerType + "OperatorAbility")
            .build())
          .setIRIAsString(profileURIStr + "/#action" + sNum)
          .build();

        builder.exposeSignifier(sig);
      }
      HMASProfileBuilders.add(builder);
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

  private String getTargetPlan(int preferredControllerIndex) {

    String applicationContext = "true";
    String planAnnot = "[artifact_name(ArtName), wsp(" + this.workspaceName + ")]";

    String actionTypePrefix;
    String planLabel;
    String profileURI = this.envURL + "/artifacts/controller" + preferredControllerIndex;
    agentAbilities.add("controller" + preferredControllerIndex + ":#" + "ObserverAbility");

    if (preferredControllerIndex < HC_DEVICES_NUM) {
      actionTypePrefix = EX_NS.getPrefix() + ":HeatingActionType";
      planLabel = "heating";
      if (this.adjustedExposure) {
        agentAbilities.add("controller" + preferredControllerIndex + ":#" + "HeatingControllerOperatorAbility");
      }
    } else {
      actionTypePrefix = EX_NS.getPrefix() + ":CoolingActionType";
      planLabel = "air_flow_modulation";
      if (this.adjustedExposure) {
        agentAbilities.add("controller" + preferredControllerIndex + ":#" + "CoolingControllerOperatorAbility");
      }
    }

    if ("hmas".equals(this.vocabulary)) {
      if (concisePlans) {
        applicationContext = "true";
      } else if (adjustedExposure) {
        applicationContext = "ability(Abilities0) & signifier([\"" + actionTypePrefix + "0\"],Abilities0,_)[artifact_name(ArtName)] " +
                "& ability(Abilities1) & signifier([\"" + actionTypePrefix + "1\"],Abilities1,_)[artifact_name(ArtName)] " +
                "& ability(Abilities2) & signifier([\"" + actionTypePrefix + "2\"],Abilities2,_)[artifact_name(ArtName)] ";
      } else {
        applicationContext = "signifier([\"" + actionTypePrefix + "0\"])[artifact_name(ArtName)] " +
                "& signifier([\"" + actionTypePrefix + "1\"])[artifact_name(ArtName)] " +
                "& signifier([\"" + actionTypePrefix + "2\"])[artifact_name(ArtName)] ";
      }
    } else if ("td".equals(this.vocabulary)) {
      applicationContext = "affordance([\"" + actionTypePrefix + "0\"])[artifact_name(ArtName)] " +
        "& affordance([\"" + actionTypePrefix + "1\"])[artifact_name(ArtName)] " +
        "& affordance([\"" + actionTypePrefix + "2\"])[artifact_name(ArtName)] ";
    }

    return "@test_goal_" + planLabel + " +!test_goal(ArtName) : " + applicationContext +
            " <- stopTimerAndLog; " +
            "invokeAction(\"" + actionTypePrefix + "0\")" + planAnnot + "; " +
            "invokeAction(\"" + actionTypePrefix + "1\")" + planAnnot + "; " +
            "invokeAction(\"" + actionTypePrefix + "2\")" + planAnnot + ".";
  }

  private static List<ActionAffordance> getDefaultAffordances(String artifactName) {
    List<ActionAffordance> defaultAffordances = new ArrayList<>();

    ActionAffordance registerAff = new ActionAffordance.Builder("register",
            new ch.unisg.ics.interactions.wot.td.affordances.Form
                    .Builder("http://localhost:8000/" + artifactName + "/register")
                    .setMethodName("POST").build())
            .addSemanticType(JACAMO_NS.getName() + "LogIn")
            .build();

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


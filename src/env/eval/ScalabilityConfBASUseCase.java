package eval;

import cartago.*;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphWriter;
import ch.unisg.ics.interactions.hmas.interaction.signifiers.*;
import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.ActionAffordance;
import ch.unisg.ics.interactions.wot.td.io.TDGraphWriter;
import org.apache.hc.core5.http.HttpStatus;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@ARTIFACT_INFO(
  outports = {
    @OUTPORT(name = "bas-conf-out")
  }
)
public class ScalabilityConfBASUseCase extends Artifact {

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
  protected int registeredDevicesNum;

  public void init(String url, String workspaceName, String vocabulary) {
    this.envURL = url;
    this.workspaceName = workspaceName;
    this.registeredDevicesNum = 0;

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
    if (this.registeredDevicesNum <= HC_DEVICES_NUM + CC_DEVICES_NUM) {
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
    createResourceProfiles(HC_DEVICES_NUM, HC_SIGNIFIERS_NUM,  "heatingController", "HeatingController", "HeatingActionType", "HeatingControllerOperator");
    createResourceProfiles(CC_DEVICES_NUM, CC_SIGNIFIERS_NUM, "coolingController","CoolingController", "CoolingActionType", "CoolingControllerOperator");
  }

  private void initAllTDs() {
    createTDs(HC_DEVICES_NUM, HC_SIGNIFIERS_NUM,  "heatingController", "HeatingController", "HeatingActionType", "HeatingControllerOperator");
    createTDs(CC_DEVICES_NUM, CC_SIGNIFIERS_NUM,  "coolingController", "CoolingController", "CoolingActionType", "CoolingControllerOperator");

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
    String preferredArtifact;
    if (artifactIndex < HC_DEVICES_NUM) {
      preferredArtifact = "heatingController" + artifactIndex;
    } else {
      preferredArtifact = "coolingController" + (artifactIndex - HC_DEVICES_NUM);
    }
    Collections.shuffle(this.plans);
    this.defineObsProperty("agent_metadata", preferredArtifact, this.plans.toArray());
  }

  private void registerArtifact(int artifactIndex) {

    String profileStr = null;

    if ("hmas".equals(this.vocabulary)) {
      ResourceProfile.Builder randomProfileBuilder = HMASProfileBuilders.get(artifactIndex);
      profileStr = new ResourceProfileGraphWriter(randomProfileBuilder.build()).write();
      HMASProfileBuilders.remove(HMASProfileBuilders.get(artifactIndex));
    } else if ("td".equals(this.vocabulary)) {
      ThingDescription.Builder randomTDBuilder = TDBuilders.get(artifactIndex);
      profileStr = new TDGraphWriter(randomTDBuilder.build()).write();
      TDBuilders.remove(TDBuilders.get(artifactIndex));
    }

    if (profileStr != null) {
      HttpClient client = new HttpClient();

      try {
        client.start();
        ContentResponse response = client.POST(this.envURL + "/artifacts/")
          .content(new StringContentProvider(profileStr), "text/turtle")
          .header("X-Agent-WebID", WEB_ID)
          .header("Slug", "controller" + artifactIndex).send();
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

  private void createTDs(int deviceCount, int signifierCount, String controllerNamePrefix, String controllerType, String actionTypePrefix, String operatorSemanticType) {
    for (int dNum = 0; dNum < deviceCount; dNum++) {
      String profileURI = this.envURL + "/artifacts/" + controllerType + dNum;
      ThingDescription.Builder builder = new ThingDescription.Builder(controllerNamePrefix + dNum)
        .addThingURI(profileURI)
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
      TDBuilders.add(builder);
    }
  }

  private void createResourceProfiles(int deviceCount, int signifierCount, String controllerNamePrefix, String controllerType, String actionTypePrefix, String operatorSemanticType) {
    for (int dNum = 0; dNum < deviceCount; dNum++) {
      String profileURI = this.envURL + "/artifacts/" + controllerType + dNum;
      ResourceProfile.Builder builder = new ResourceProfile.Builder(new ch.unisg.ics.interactions.hmas.core.hostables.Artifact
        .Builder()
        .setIRIAsString(profileURI + "/#artifact")
        .addSemanticType(EX_NS.getName() + controllerType)
        .build())
        .setIRIAsString(profileURI + "/");

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
            .addSemanticType(EX_NS.getName() + operatorSemanticType)
            .build())
          .setIRIAsString(profileURI + "/#action" + sNum)
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
      applicationContext = "ability(Ability) & signifier([\"" + actionType + "\"], [Ability], _)";
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

    if (preferredControllerIndex < HC_DEVICES_NUM) {
      actionTypePrefix = EX_NS.getPrefix() + ":HeatingActionType";
      planLabel = "heating";
    } else {
      actionTypePrefix = EX_NS.getPrefix() + ":CoolingActionType";
      planLabel = "air_flow_modulation";
    }

    if ("hmas".equals(this.vocabulary)) {
      applicationContext = null;
      //"ability(Ability) & signifier([\"" + actionTypePrefix + "0\"], [Ability], _)";
    } else if ("td".equals(this.vocabulary)) {
      applicationContext = "affordance([\"" + actionTypePrefix + "0\"])[artifact_name(ArtName)] " +
        "& affordance([\"" + actionTypePrefix + "1\"])[artifact_name(ArtName)] " +
        "& affordance([\"" + actionTypePrefix + "2\"])[artifact_name(ArtName)] ";
    }

    return "@test_goal_" + planLabel + " +!test_goal(ArtName) : " + applicationContext +
      " <- invokeAction(\"" + actionTypePrefix + "0\")" + planAnnot + "; " +
      "invokeAction(\"" + actionTypePrefix + "1\")" + planAnnot + "; " +
      "invokeAction(\"" + actionTypePrefix + "2\")" + planAnnot + ".";
  }
}


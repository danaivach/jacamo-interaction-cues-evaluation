package eval;

import cartago.*;
import ch.unisg.ics.interactions.hmas.interaction.io.ResourceProfileGraphReader;
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
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.model.impl.SimpleNamespace;

import java.util.*;
import java.util.stream.Collectors;

@ARTIFACT_INFO(
        outports = {
                @OUTPORT(name = "conf-out")
        }
)
public class ScalabilityConf extends Artifact {

  protected static final String[] ACTIONS = {
          "Ignite", "Drench", "Gust", "Stabilize", "Charge", "Freeze", "Forge", "Enshroud", "Illuminate", "Disperse"
  };

  protected static final String[] BASE_ABILITIES = {
          "Pyromancy", "Hydromancy", "Aeromancy", "Geomancy", "Electromancy",
          "Cryomancy", "Metallurgy", "Shadowcraft", "Lumomancy", "Voidweaving"
  };

  protected static final String[] SUPPORTING_ITEMS = {
          "Glyph", "Rune", "Mark", "Sigil", "Emblem", "Seal", "Insignia", "Crest", "Symbol", "Badge"
  };

  protected static final Namespace EX_NS = new SimpleNamespace("ex", "https://example.org/");
  protected static final String WEB_ID = "https://example.org/env-manager";

  protected ResourceProfile.Builder testHMASProfileBuilder;
  protected ThingDescription.Builder testTDBuilder;
  protected List<Signifier> allSignifiers;
  protected List<ActionAffordance> allAffordances;
  protected HashMap<Signifier,String> allHMASPlans;
  protected HashMap<ActionAffordance, String> allTDPlans;

  protected String envURL;
  protected String workspaceName;
  protected String artifactName;
  protected String vocabulary;
  protected boolean dynamicResolution;
  protected boolean dynamicPlanLib;
  protected boolean dynamicAbilities;
  protected int signifierNum;
  protected int maxSignifierNum;
  protected boolean logTime;

  protected final Random random = new Random();

  protected static String getCurie(String str, Namespace ns) {
    return str.replace(ns.getName(), ns.getPrefix() + ":");
  }

  public void init(String url, String workspaceName, String artifactName, String vocabulary, boolean dynamicResolution,
                   boolean dynamicPlanLib, boolean dynamicAbilities, int signifierNum, int maxSignifierNum, boolean logTime) {
    this.envURL = url;
    this.workspaceName = workspaceName;
    this.artifactName = artifactName;
    this.dynamicResolution = dynamicResolution;
    this.dynamicPlanLib = dynamicPlanLib;
    this.dynamicAbilities = dynamicAbilities;
    this.signifierNum = signifierNum;
    this.maxSignifierNum = maxSignifierNum;
    this.logTime = logTime;

    this.testHMASProfileBuilder = this.getHMASProfileBaseBuilder();
    this.testTDBuilder = this.getTDBaseBuilder();

    if ("hmas".equals(vocabulary) || "https://purl.org/hmas/".equals(vocabulary)) {
      this.vocabulary = "hmas";
      this.initAllSignifiers();
    } else if ("td".equals(vocabulary) || "https://www.w3.org/2019/wot/td#".equals(vocabulary)) {
      this.vocabulary = "td";
      this.initAllAffordances();
    } else {
      failed("Unknown vocabulary. Please, select either \"hmas\" for testing with hMAS Resource Profiles," +
              "or \"td\" for testing with W3C Web of Things Thing Descriptions.");
    }

    this.updateAgentSituation();
    this.publishEmptyProfile();
    this.updatePublishedProfile(this.getUpdatedProfile());
  }

  public void init(String url, String workspaceName, String artifactName, boolean dynamicResolution, int signifierNum, int maxSignifierNum, boolean logTime) {
    this.init(url, workspaceName, artifactName, "hmas", dynamicResolution, false, false, signifierNum, maxSignifierNum, logTime);
  }

  @OPERATION
  private void increaseSignifiers() {
    if (this.signifierNum < this.maxSignifierNum) {
      this.signifierNum++;
      this.updateAgentSituation();
      try {
        if (logTime) {
          execLinkedOp("conf-out", "setRound", signifierNum);
        }
      } catch (OperationException e) {
        throw new RuntimeException(e);
      }
      this.updatePublishedProfile(this.getUpdatedProfile());
    }
  }

  protected ResourceProfile.Builder getHMASProfileBaseBuilder() {
    String artifactURI = this.envURL + "/artifacts/" + this.artifactName ;
    return new ResourceProfile.Builder(new ch.unisg.ics.interactions.hmas.core.hostables.Artifact
            .Builder()
            .setIRIAsString(artifactURI + "/#artifact")
            .addSemanticType(EX_NS.getName() + "SpellBook")
            .build())
            .setIRIAsString(this.envURL + "/artifacts/" + this.artifactName + "/");
  }

  protected ThingDescription.Builder getTDBaseBuilder() {
    String artifactURI = this.envURL + "/artifacts/" + this.artifactName ;
    return new ThingDescription.Builder(this.artifactName)
            .addThingURI(artifactURI)
            .addSemanticType(EX_NS.getName() + "SpellBook");
  }

  private void publishEmptyProfile() {
    String profileStr = null;

    if ("hmas".equals(this.vocabulary)) {
      profileStr = new ResourceProfileGraphWriter(testHMASProfileBuilder.build()).write();
    } else if ("td".equals(this.vocabulary)) {
      profileStr = new TDGraphWriter(testTDBuilder.build()).write();
    }

    if (profileStr != null) {
      HttpClient client = new HttpClient();

      try {
        client.start();

        ContentResponse response = client.POST(this.envURL + "/artifacts/")
                .content(new StringContentProvider(profileStr), "text/turtle")
                .header("X-Agent-WebID", WEB_ID)
                .header("Slug", this.artifactName).send();

        if (response.getStatus() != HttpStatus.SC_CREATED) {
          log("Request failed: " + response.getStatus());
        }

        ResourceProfile profile = ResourceProfileGraphReader.readFromURL(this.envURL + "/artifacts/" + this.artifactName);
        this.testHMASProfileBuilder.exposeSignifiers(profile.getExposedSignifiers());

        client.stop();
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  private void updatePublishedProfile(String profile) {

    HttpClient client = new HttpClient();
    try {
      client.start();

      ContentResponse response = client.newRequest(this.envURL + "/artifacts/" + this.artifactName)
              .method(HttpMethod.PUT)
              .content(new StringContentProvider(profile), "text/turtle")
              .header("X-Agent-WebID", WEB_ID)
              .send();

      if (response.getStatus() != HttpStatus.SC_OK) {
        log("Request failed: " + response.getStatus());
      }

      client.stop();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private String getUpdatedProfile() {
    // Check if signifierNum is greater than 0
    if (signifierNum > 0) {
      if ("hmas".equals(this.vocabulary)) {
        ResourceProfile profile = this.testHMASProfileBuilder
                .exposeSignifier(this.allSignifiers.get(signifierNum - 1))
                .build();
        return new ResourceProfileGraphWriter(profile).write();
      } else if ("td".equals(this.vocabulary)) {
        ThingDescription td = this.testTDBuilder
                .addAction(this.allAffordances.get(signifierNum - 1))
                .build();
        return new TDGraphWriter(td).write();
      }
    } else {
      // Handle the case when signifierNum is 0 or less
      if ("hmas".equals(this.vocabulary)) {
        return new ResourceProfileGraphWriter(this.testHMASProfileBuilder.build()).write();
      } else if ("td".equals(this.vocabulary)) {
        return new TDGraphWriter(this.testTDBuilder.build()).write();
      }
    }
    // Return null if vocabulary is neither "hmas" nor "td"
    return null;
  }


  @OPERATION
  public void updateAgentSituation() {
    System.out.println(signifierNum);
    if (signifierNum > 0) {
      Object[] prefixedAbilityTypes = {"ex:Pyromancy0"};
      List<String> knownPlans = new ArrayList<>();

      if (dynamicAbilities && "hmas".equals(this.vocabulary)) {
        List<Signifier> exposedSignifiers = this.allSignifiers.subList(0, signifierNum);
        int index = random.nextInt(exposedSignifiers.size());

        // Return the signifier at the random index
        Signifier randomSignifier = exposedSignifiers.get(index);

        Iterator<Ability> abilitiesIt = randomSignifier.getRecommendedAbilities().iterator();
        Ability ability = abilitiesIt.hasNext() ? abilitiesIt.next() : null;

        if (ability != null) {
          prefixedAbilityTypes = ability.getSemanticTypes()
                  .stream()
                  .filter(type -> !type.equals(INTERACTION.TERM.ABILITY.toString()))
                  .map(type -> getCurie(type, EX_NS))
                  .collect(Collectors.toSet()).toArray();
        }

        if (dynamicPlanLib) {
          knownPlans.add(this.allHMASPlans.get(randomSignifier));
        }
      }

      if (!dynamicPlanLib && signifierNum == 1) {
        if ("hmas".equals(this.vocabulary)) {
          knownPlans = this.allHMASPlans.values().stream().toList();
        } else if ("td".equals(this.vocabulary)) {
          knownPlans = this.allTDPlans.values().stream().toList();
        }
      }

      if (this.getObsProperty("agent_metadata") == null) {
        this.defineObsProperty("agent_metadata", knownPlans.toArray(), prefixedAbilityTypes[0], dynamicPlanLib);
      } else {
        this.getObsProperty("agent_metadata").updateValues(knownPlans.toArray(), prefixedAbilityTypes[0], dynamicPlanLib);
      }
    }
  }


  private String getPlan(String actionType) {

    String applicationContext = "true";
    String planAnnot = "[artifact_name("
            + this.artifactName + "), wsp("
            + this.workspaceName + ")]";

    String planActionType = getCurie(actionType, EX_NS);
    String planLabel = planActionType.substring(planActionType.indexOf(":") + 1).toLowerCase();

    if (!this.dynamicResolution && "hmas".equals(this.vocabulary)) {
      applicationContext = "ability(Ability) & signifier([\"" + planActionType + "\"], [Ability], _)";
    } else if ("td".equals(this.vocabulary)) {
      applicationContext = "affordance([\"" + planActionType + "\"])";
    }

    return "@test_goal_" + planLabel + " +!test_goal : " + applicationContext +
            " <- invokeAction(\"" + planActionType + "\")" + planAnnot + ". ";
  }

  private void initAllAffordances() {
    this.allAffordances = new ArrayList<>();
    this.allTDPlans = new HashMap<ActionAffordance, String>();

    for (int level = 0; level < this.maxSignifierNum/10 ; level++) {
      for (int j = 0; j < BASE_ABILITIES.length; j++) {
        String action = ACTIONS[j];
        String ability = BASE_ABILITIES[j];

        String actionType = EX_NS.getName() + action + level;
        String abilityType = EX_NS.getName() + ability + level;

        ActionAffordance aff = new ActionAffordance.Builder(action.toLowerCase() + level,
                new ch.unisg.ics.interactions.wot.td.affordances.Form
                        .Builder("http://localhost:8000/" + action.toLowerCase() + level)
                        .setMethodName("POST").build())
                .addSemanticType(actionType)
                .build();

        this.allAffordances.add(aff);
        this.allTDPlans.put(aff, this.getPlan(actionType));
      }
    }
  }

  private void initAllSignifiers() {
    this.allSignifiers = new ArrayList<>();
    this.allHMASPlans = new HashMap<>();

    for (int level = 0; level < this.maxSignifierNum/10 ; level++) {
      for (int j = 0; j < BASE_ABILITIES.length; j++) {
        String action = ACTIONS[j];
        String ability = BASE_ABILITIES[j];

        String actionType = EX_NS.getName() + action + level;
        String abilityType = EX_NS.getName() + ability + level;

        ActionSpecification spec = new ActionSpecification.Builder(new Form
                .Builder("http://localhost:8000/" + action.toLowerCase() + level)
                .setMethodName("POST")
                .build())
                .addRequiredSemanticType(actionType)
                .build();

        Signifier sig = new Signifier.Builder(spec)
                .addRecommendedAbility(new Ability
                        .Builder()
                        .addSemanticType(abilityType)
                        .build())
                .setIRIAsString(this.envURL + "/artifacts/" + this.artifactName + "/#"
                        + ACTIONS[j].toLowerCase()
                        + level)
                .build();

        this.allSignifiers.add(sig);
        this.allHMASPlans.put(sig, this.getPlan(actionType));
      }
    }
    System.out.println("Total number of signifiers to be loaded: " + this.allSignifiers.size());
  }
}

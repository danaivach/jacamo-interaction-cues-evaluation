# Evaluation of interaction guidance in Hypermedia Multi-Agent Systems 
This project is built using JaCaMo to evaluate the interaction guidance provided by the environment in Hypermedia Multi-Agent Systems (MAS).

## Objective
The goal of this project is to analyze how the environment's interaction cues affect the decision-making process of agents. Specifically, we examine the scalability by measuring how quickly a Jason agent selects a plan as the number of available interaction cues increases.

## Scalability Evaluation
We evaluate the time in which a Jason agent decides which plan to execute while increasing the number of interaction cues in the environment.

### Experiment 1: Full signifier exposure
In this experiment, the environment contains an artifact that progressively offers more action possibilities. Each possible action is advertised via an interaction cue in the environment. The agent must reason over all available cues and decide which action to take, for example, based on the type of action being advertised.

#### Running the Experiment
To evaluate the agent's behavior, execute the following command:
````
./gradlew runEvalScalability -Psem=0 -PdynamicPlans=0 -PdynamicAbilities=0
````

### Experiment 2: Adjusted signifier exposure
In this experiment, the environment contains an artifact that progressively offers more action possibilities, and an artifact that implements a Signifier Exposure Mechanism (SEM). The SEM artifact dynamically adjusts which cues to expose to the agent, based on the available cues and the agent's resource profile. The agent then reasons about the filtered cues to decide how to act.
the cues that complement it. 

#### Running the Experiment
To evaluate the agent's behavior under the SEM, execute the following command:
````
./gradlew runEvalScalability -Psem=1 -PdynamicPlans=0 -PdynamicAbilities=0
````



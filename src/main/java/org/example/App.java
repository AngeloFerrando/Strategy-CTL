package org.example;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import utils.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Main class
 */
public class App 
{
    public static void main( String[] args ) throws Exception {

      // String s = AbstractionUtils.modelCheck("./tmp/subModel.ispl");


        // read json file
        String jsonModel = Files.readString(Paths.get("./roverEx1.json"), StandardCharsets.UTF_8);
        // load json file to ATL Model Java representation
        AtlModel atlModel = JsonObject.load(jsonModel, AtlModel.class);
        // validate the model
        AbstractionUtils.validateAtlModel(atlModel);
        // add default transitions to the model
        AbstractionUtils.processDefaultTransitions(atlModel);

//        String jsonSubModel = Files.readString(Paths.get("./tmp/ir/subModel0.json"), StandardCharsets.UTF_8);
//        AtlModel atlSubModel = JsonObject.load(jsonSubModel, AtlModel.class);
//        AbstractionUtils.validateAtlModel(atlSubModel);
//        AbstractionUtils.processDefaultTransitions(atlSubModel);
//
//        createMonitor(atlModel, atlSubModel);

        List<Pair<AtlModel,Monitor>> subModelsIR = allSubICGSWithPerfectInformation(atlModel);
        System.out.println("PerfectInformationSubModels: " + subModelsIR.size() + "\n\n");
        FileUtils.cleanDirectory(new File("./tmp/IR/"));
        int i = 0;
        for(Pair<AtlModel,Monitor> m : subModelsIR) {
            FileWriter writer = new FileWriter("./tmp/IR/subModel" + i++ + ".json");
            writer.append(m.getLeft().toString()).append("\n\n");
            writer.close();
        }

//        List<Pair<AtlModel,Monitor>> subModelsir = allSubICGSWithImperfectRecall(atlModel);
//        System.out.println("ImperfectRecallSubModels: " + subModelsir.size() + "\n\n");
//        FileUtils.cleanDirectory(new File("./tmp/ir/"));
//        i = 0;
//        for(Pair<AtlModel,Monitor> m : subModelsir) {
//            FileWriter writer = new FileWriter("./tmp/ir/subModel" + i++ + ".json");
//            writer.append(m.getLeft().toString()).append("\n\n");
//            writer.close();
//        }
        Set<Monitor> monitors = new HashSet<>();
        monitors.addAll(subModelsIR.stream().map(Pair::getRight).collect(Collectors.toList()));
//        monitors.addAll(subModelsir.stream().map(Pair::getRight).collect(Collectors.toList()));
        List<String> trace = new ArrayList<>();
        trace.add("s0");
        trace.add("s2");
        trace.add("o");
        trace.add("s0");
        execRV(monitors, trace);
    }

    public static List<Pair<AtlModel,Monitor>> allSubICGSWithImperfectRecall(AtlModel model) throws Exception {
        List<AtlModel> candidates = AbstractionUtils.allModels(model);
        return AbstractionUtils.validateSubModels(model, candidates, true);
    }

    public static List<Pair<AtlModel,Monitor>> allSubICGSWithPerfectInformation(AtlModel model) throws Exception {
        List<AtlModel> candidates = new LinkedList<>();
        candidates.add(model);
        List<AtlModel> candidatesPP = new LinkedList<>();
        while(!candidates.isEmpty()) {
            AtlModel candidate = candidates.remove(0);
            boolean valid = true;
            for(Agent agent : candidate.getAgents()){
                if(!agent.getIndistinguishableStates().isEmpty()) {
                    for(List<String> indistinguishableStates : agent.getIndistinguishableStates()) {
                        for (String ind : indistinguishableStates) {
                            AtlModel aux = candidate.clone();
                            State s = new State();
                            s.setName(ind);
                            aux.removeState(s);
                            candidates.add(aux);
                        }
                    }
                    valid = false;
                    break;
                }
            }
            if(valid) {
                if(candidatesPP.stream().noneMatch((m) -> new HashSet<>(m.getStates()).equals(new HashSet<>(candidate.getStates())))) {
                    candidatesPP.add(candidate);
                }
            }
        }
        return AbstractionUtils.validateSubModels(model, candidatesPP, false);
    }

    public static void execRV(Set<Monitor> monitors, List<String> trace) throws IOException {
        for(String event : trace) {
            Set<Monitor> monitorsAux = new HashSet<>();
            Set<Pair<String, String>> satisfiedFormulas = new HashSet<>();
            for(Monitor monitor : monitors) {
                Monitor.Verdict output = monitor.next(event);
                if(output == Monitor.Verdict.Unknown) {
                    monitorsAux.add(monitor);
                } else if(output == Monitor.Verdict.True) {
                    satisfiedFormulas.add(new ImmutablePair<>(monitor.getLtl(), monitor.getAtl()));
                }
            }
            monitors = monitorsAux;
            for(Pair<String, String> p : satisfiedFormulas) {
                System.out.println("- Monitor concluded satisfaction of LTL property: " + p.getLeft());
                System.out.println("and reached a sub-model where the ATL property: " + p.getRight() + " is satisfied.");
            }
            if(!satisfiedFormulas.isEmpty()) {
                System.out.println("Do you want to continue monitoring the system? [y/n]");
                Scanner scanner = new Scanner(System.in);
                String choice = scanner.next();
                if(choice.equals("n")) {
                    return;
                }
            }
        }
    }
}

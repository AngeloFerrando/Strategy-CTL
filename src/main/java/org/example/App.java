package org.example;

import org.apache.commons.io.FileUtils;
import utils.*;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

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

        //createMonitor(atlModel, atlSubModel);

        List<AtlModel> subModels = allSubICGSWithPerfectInformation(atlModel);
        System.out.println("PerfectInformationSubModels: " + subModels.size() + "\n\n");
        FileUtils.cleanDirectory(new File("./tmp/IR/"));
        int i = 0;
        for(AtlModel m : subModels) {
            FileWriter writer = new FileWriter("./tmp/IR/subModel" + i++ + ".json");
            writer.append(m.toString()).append("\n\n");
            writer.close();
        }

        subModels = allSubICGSWithImperfectRecall(atlModel);
        System.out.println("ImperfectRecallSubModels: " + subModels.size() + "\n\n");
        FileUtils.cleanDirectory(new File("./tmp/ir/"));
        i = 0;
        for(AtlModel m : subModels) {
            FileWriter writer = new FileWriter("./tmp/ir/subModel" + i++ + ".json");
            writer.append(m.toString()).append("\n\n");
            writer.close();
        }
    }

    public static List<AtlModel> allSubICGSWithImperfectRecall(AtlModel model) throws Exception {
        List<AtlModel> candidates = AbstractionUtils.allModels(model);
        return AbstractionUtils.validateSubModels(model.getFormula(), candidates, true);
    }

    public static List<AtlModel> allSubICGSWithPerfectInformation(AtlModel model) throws Exception {
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
        return AbstractionUtils.validateSubModels(model.getFormula(), candidatesPP, false);
    }

    public static void createMonitor(AtlModel model, AtlModel subModel) {
        AtlModel copy = model.clone();
        Optional<? extends State> initialState = subModel.getStates().stream().filter(State::isInitial).findFirst();
        if(initialState.isPresent()) {
            Optional<String> atom = initialState.get().getLabels().stream().filter(l -> l.startsWith("atom")).findFirst();
            if(atom.isPresent()) {
                String ltl = copy.getFormula().extractLTL(subModel.getFormula(), atom.get());
                copy.getState(initialState.get().getName()).getLabels().add(atom.get());
            }
        }
        String pippo = "";
    }
}

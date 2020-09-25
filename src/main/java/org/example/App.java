package org.example;

import org.apache.commons.io.FileUtils;
import utils.*;

import java.io.File;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

/**
 * Main class
 */
public class App 
{
    public static void main( String[] args ) throws Exception {

      // String s = AbstractionUtils.modelCheck("./tmp/subModel.ispl");


        // read json file
        String jsonModel = Files.readString(Paths.get("./roverEx2.json"), StandardCharsets.UTF_8);
        // load json file to ATL Model Java representation
        AtlModel atlModel = JsonObject.load(jsonModel, AtlModel.class);
        // validate the model
        AbstractionUtils.validateAtlModel(atlModel);
        // add default transitions to the model
        AbstractionUtils.processDefaultTransitions(atlModel);

        List<AtlModel> subModels = allSubICGSWithImperfectRecall(atlModel);
        System.out.println("ImperfectRecallSubModels: " + subModels.size() + "\n\n");
        FileUtils.cleanDirectory(new File("./tmp/ir/"));
        int i = 0;
        for(AtlModel m : subModels) {
            FileWriter writer = new FileWriter("./tmp/ir/subModel" + i++ + ".json");
            writer.append(m.toString()).append("\n\n");
            writer.close();
        }

        subModels = allSubICGSWithPerfectInformation(atlModel);
        System.out.println("PerfectInformationSubModels: " + subModels.size() + "\n\n");
        FileUtils.cleanDirectory(new File("./tmp/IR/"));
        i = 0;
        for(AtlModel m : subModels) {
            FileWriter writer = new FileWriter("./tmp/IR/subModel" + i++ + ".json");
            writer.append(m.toString()).append("\n\n");
            writer.close();
        }
    }

    public static List<AtlModel> allSubICGSWithImperfectRecall(AtlModel model) throws Exception {
        List<AtlModel> candidates = AbstractionUtils.allModels(model);
        return AbstractionUtils.validateSubModels(model.getFormula(), candidates);
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
                    List<String> indistinguishableStates = agent.getIndistinguishableStates().get(0);
                    for (String ind : indistinguishableStates) {
                        AtlModel aux = candidate.clone();
                        State s = new State();
                        s.setName(ind);
                        aux.removeState(s);
                        candidates.add(aux);
                    }
                    valid = false;
                    break;
                }
            }
            if(valid) {
                candidatesPP.add(candidate);
            }
        }
        return AbstractionUtils.validateSubModels(model.getFormula(), candidatesPP);
    }
}

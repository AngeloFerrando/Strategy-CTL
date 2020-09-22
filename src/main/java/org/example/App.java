package org.example;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import utils.*;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Main class
 */
public class App 
{
    public static void main( String[] args ) throws Exception {
        // read json file
        String jsonModel = Files.readString(Paths.get("./roverEx1.json"), StandardCharsets.UTF_8);
        // load json file to ATL Model Java representation
        AtlModel atlModel = JsonObject.load(jsonModel, AtlModel.class);
        // validate the model
        AbstractionUtils.validateAtlModel(atlModel);
        // add default transitions to the model
        AbstractionUtils.processDefaultTransitions(atlModel);

        FileWriter writer = new FileWriter("./tmp/outputir.txt");
        List<AtlModel> subModels = maxSubICGSWithImperfectRecall(atlModel);
        writer.write("SubModels: " + subModels.size() + "\n\n");
        for(AtlModel m : subModels) {
            writer.append(m.toString() + "\n\n");
        }
        writer.close();
        writer = new FileWriter("./tmp/outputIR.txt");
        subModels = maxSubICGSWithPerfectInformation(atlModel);
        writer.write("SubModels: " + subModels.size() + "\n\n");
        for(AtlModel m : subModels) {
            writer.append(m.toString() + "\n\n");
        }
        writer.close();
    }

    private static List<AtlModel> extractSubModels(Formula formula, List<AtlModel> candidates) throws IOException {
        int j = 1;
        int i = 0;
        List<AtlModel> results = new ArrayList<>();
        for(AtlModel candidate : candidates) {
            System.out.println("Checking candidate " + j++ + " of " + candidates.size());
            Formula formula1 = null, formulaAux = formula.clone();
            boolean satisfied;
            do {
                formula1 = formulaAux.innermostFormula();
                // compile candidate sub-model to ispl
                candidate.setFormula(formula1);
                String mcmasProgram = AbstractionUtils.generateMCMASProgram(candidate);
                // write temporary ispl file
                String fileName = "./tmp/subModel.ispl";
                Files.write(Paths.get(fileName), mcmasProgram.getBytes());
                // model check the ispl model
                satisfied = AbstractionUtils.getMcmasResult(AbstractionUtils.modelCheck(fileName));
                if(satisfied) {
                    if(formulaAux != formula1) {
                        formulaAux.updateInnermostFormula("a" + i);
                        candidate.updateModel("a" + i);
                    }
                    results.add(candidate);
                    i++;
                }
            } while(formulaAux != formula1 && satisfied);
        }
        return results;
    }

    public static List<AtlModel> maxSubICGSWithImperfectRecall(AtlModel model) throws IOException {
        List<AtlModel> candidates = AbstractionUtils.allModels(model);
        return extractSubModels(model.getFormula(), candidates);
    }

    public static List<AtlModel> maxSubICGSWithPerfectInformation(AtlModel model) throws IOException {
        List<AtlModel> candidates = new LinkedList<>();
        candidates.add(model);
        List<AtlModel> candidatesPP = new LinkedList<>();
        while(!candidates.isEmpty()) {
            AtlModel candidate = candidates.remove(0);
            for(Agent agent : candidate.getAgents()){
                if(!agent.getIndistinguishableStates().isEmpty()) {
                    List<String> indistinguishableStates = agent.getIndistinguishableStates().get(0);
                    for(String ind : indistinguishableStates) {
                        AtlModel aux = candidate.clone();
                        State s = new State();
                        s.setName(ind);
                        aux.removeState(s);
                        candidates.add(aux);
                    }
                } else {
                    candidatesPP.add(candidate);
                }
            }
        }
        return extractSubModels(model.getFormula(), candidatesPP);
    }
}

package org.example;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import utils.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Main class
 */
public class App 
{
    public static void main( String[] args ) throws Exception {
        // read json file
        String jsonModel = Files.readString(Paths.get("./test.json"), StandardCharsets.UTF_8);
        // load json file to ATL Model Java representation
        AtlModel atlModel = JsonObject.load(jsonModel, AtlModel.class);
        // validate the model
        AbstractionUtils.validateAtlModel(atlModel);
        // add default transitions to the model
        AbstractionUtils.processDefaultTransitions(atlModel);

//        String mcmasProgram = AbstractionUtils.generateMCMASProgram(atlModel);
//        // write temporary ispl file
//        String fileName = "./tmp/subModel.ispl";
//        Files.write(Paths.get(fileName), mcmasProgram.getBytes());
//        // model check the ispl model
//        System.out.println(AbstractionUtils.modelCheck(fileName));


        List<AtlModel> subModels = maxSubICGSWithImperfectRecall(atlModel);
        System.out.println("SubModels: " + subModels.size());
        for(AtlModel m : subModels) {
            System.out.println(m);
        }
    }

    public static List<AtlModel> maxSubICGSWithImperfectRecall(AtlModel model) throws IOException, CloneNotSupportedException {
        List<AtlModel> results = new ArrayList<>();
        Formula formula = model.getFormula();
        int i = 0, j = 0;
        List<AtlModel> candidates = AbstractionUtils.allModels(model);
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
                    formulaAux.updateInnermostFormula("a" + i);
                    candidate.updateModel("a" + i);
                    results.add(candidate);
                    i++;
                }
            } while(formula != formula1 && satisfied);
            formulaAux = formula;
        }
        return results;
    }
}

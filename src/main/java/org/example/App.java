package org.example;

import utils.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

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
        // compile ATL model to ispl representation
        String mcmasProgram = AbstractionUtils.generateMCMASProgram(atlModel);
        // write temporary ispl file
        String fileName = "./tmp/model" + System.currentTimeMillis()+".ispl";
        while (Files.exists(Paths.get(fileName))) {
            fileName = "/tmp/model" + System.currentTimeMillis() + ".ispl";
        }
        Files.write(Paths.get(fileName), mcmasProgram.getBytes());
        // model check the ispl model
        String mcmasOutputMustAtlModel = AbstractionUtils.modelCheck(fileName);
        // print the result
        System.out.println(mcmasOutputMustAtlModel);
    }
}

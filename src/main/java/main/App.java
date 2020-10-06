package main;

import com.google.common.base.Stopwatch;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import utils.*;

import javax.annotation.processing.Filer;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Main class
 */
public class App 
{
    public static void main( String[] args ) throws Exception {

        Options options = new Options();

        Option impRecall = new Option("r", "imperfect-recall", false, "find sub-models with imperfect recall");
        impRecall.setRequired(false);
        options.addOption(impRecall);
        Option perfectInfo = new Option("I", "perfect-information", false, "find sub-models with perfect information");
        perfectInfo.setRequired(false);
        options.addOption(perfectInfo);
        Option modelOpt = new Option("m", "model", true, "the ATL model");
        modelOpt.setRequired(true);
        options.addOption(modelOpt);
        Option subModelsFolder = new Option("sb", "sub-models", true, "folder containing the sub-models to use for generating the monitors");
        subModelsFolder.setRequired(false);
        options.addOption(subModelsFolder);
        Option trace = new Option("t", "trace", true, "the trace to be analysed by the monitors");
        trace.setRequired(false);
        options.addOption(trace);
        Option outputFolder = new Option("o", "output", true, "folder where sub-models will be saved");
        outputFolder.setRequired(false);
        options.addOption(outputFolder);
        Option verbose = new Option("s", "silent", false, "disable prints");
        verbose.setRequired(false);
        options.addOption(verbose);
        Option mcmas = new Option("mcmas", "mcmas", true, "installation folder of mcmas");
        mcmas.setRequired(true);
        options.addOption(mcmas);
        Option rv = new Option("rv", "rv", true, "installation folder of lamaconv");
        rv.setRequired(true);
        options.addOption(rv);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd;

        try {
            cmd = parser.parse(options, args);
            AbstractionUtils.mcmas = cmd.getOptionValue("mcmas");
            Monitor.rv = cmd.getOptionValue("rv");
            System.out.println("Parsing the model..");
            // read json file
            String jsonModel = Files.readString(Paths.get(cmd.getOptionValue("model")), StandardCharsets.UTF_8);
            // load json file to ATL Model Java representation
            AtlModel atlModel = JsonObject.load(jsonModel, AtlModel.class);
            // validate the model
            AbstractionUtils.validateAtlModel(atlModel);
            // add default transitions to the model
            AbstractionUtils.processDefaultTransitions(atlModel);
            System.out.println("Model successfully parsed");
            boolean silent = cmd.hasOption("silent");
            if(cmd.hasOption("imperfect-recall") && cmd.hasOption("perfect-information")) {
                throw new IllegalArgumentException("-r and -I cannot be selected at the same time");
            }
            if(cmd.hasOption("imperfect-recall")) {
                System.out.println("Start extracting sub-models with imperfect recall..");
                Stopwatch stopwatch = Stopwatch.createStarted();
                List<AtlModel> subModelsir = allSubICGSWithImperfectRecall(atlModel, silent);
                stopwatch.stop();
                System.out.println("The algorithm found " + subModelsir.size() + " sub-models with imperfect recall in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " [ms]");
                FileUtils.cleanDirectory(new File(cmd.getOptionValue("output","./tmp/ir")));
                int i = 0;
                for(AtlModel m : subModelsir) {
                    FileWriter writer = new FileWriter(cmd.getOptionValue("output","./tmp/ir/") + "/subModel" + i++ + ".json");
                    writer.append(m.toString()).append("\n\n");
                    writer.close();
                }
                FileWriter writer = new FileWriter(cmd.getOptionValue("output","./tmp/ir/") + "map");
                for(Map.Entry<String, String> item : Formula.getMapAtomToFormula().entrySet()) {
                    writer.append(item.getKey()).append(":").append(item.getValue()).append("\n");
                }
                writer.close();
                return;
            }
            if(cmd.hasOption("perfect-information")) {
                System.out.println("Start extracting sub-models with perfect information..");
                Stopwatch stopwatch = Stopwatch.createStarted();
                List<AtlModel> subModelsIR = allSubICGSWithPerfectInformation(atlModel, silent);
                stopwatch.stop();
                System.out.println("The algorithm found " + subModelsIR.size() + " sub-models with perfect information in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " [ms]");
                FileUtils.cleanDirectory(new File(cmd.getOptionValue("output","./tmp/IR")));
                int i = 0;
                for(AtlModel m : subModelsIR) {
                    FileWriter writer = new FileWriter(cmd.getOptionValue("output","./tmp/IR/") + "/subModel" + i++ + ".json");
                    writer.append(m.toString()).append("\n\n");
                    writer.close();
                }
                FileWriter writer = new FileWriter(cmd.getOptionValue("output","./tmp/IR/") + "map");
                for(Map.Entry<String, String> item : Formula.getMapAtomToFormula().entrySet()) {
                    writer.append(item.getKey()).append(":").append(item.getValue()).append("\n");
                }
                writer.close();
                return;
            }
            if(!cmd.hasOption("sub-models") || !cmd.hasOption("trace")) {
                throw new IllegalArgumentException("-sb and -t must be passed");
            }
            System.out.println("Start creating monitors..");
            Stopwatch stopwatch = Stopwatch.createStarted();
            Set<Monitor> monitors = AbstractionUtils.createMonitors(atlModel, cmd.getOptionValue("sub-models"), silent);
            stopwatch.stop();
            System.out.println("All monitors created in " + stopwatch.elapsed(TimeUnit.MILLISECONDS) + " [ms]");
            System.out.println("Run all monitors over the trace..");
            execRV(monitors, Arrays.asList(cmd.getOptionValue("trace").split(",")));
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("Strategy RV", options);
            System.exit(1);
        }



        // String s = AbstractionUtils.modelCheck("./tmp/subModel.ispl");




//        String jsonSubModel = Files.readString(Paths.get("./tmp/ir/subModel0.json"), StandardCharsets.UTF_8);
//        AtlModel atlSubModel = JsonObject.load(jsonSubModel, AtlModel.class);
//        AbstractionUtils.validateAtlModel(atlSubModel);
//        AbstractionUtils.processDefaultTransitions(atlSubModel);
//
//        createMonitor(atlModel, atlSubModel);

//        List<Pair<AtlModel,Monitor>> subModelsIR = allSubICGSWithPerfectInformation(atlModel);
//        System.out.println("PerfectInformationSubModels: " + subModelsIR.size() + "\n\n");
//        FileUtils.cleanDirectory(new File("./tmp/IR/"));
//        int i = 0;
//        for(Pair<AtlModel,Monitor> m : subModelsIR) {
//            FileWriter writer = new FileWriter("./tmp/IR/subModel" + i++ + ".json");
//            writer.append(m.getLeft().toString()).append("\n\n");
//            writer.close();
//        }
//
//
//        Set<Monitor> monitors = new HashSet<>();
//        monitors.addAll(subModelsIR.stream().map(Pair::getRight).collect(Collectors.toList()));
////        monitors.addAll(subModelsir.stream().map(Pair::getRight).collect(Collectors.toList()));
//        List<String> trace = new ArrayList<>();
//        trace.add("s0");
//        trace.add("s2");
//        trace.add("o");
//        trace.add("s0");
//        execRV(monitors, trace);
    }

    public static List<AtlModel> allSubICGSWithImperfectRecall(AtlModel model, boolean silent) throws Exception {
        System.out.println("Generating sub-models..");
        List<AtlModel> candidates = AbstractionUtils.allModels(model);
        System.out.println("Sub-models generated: " + candidates.size());
        return AbstractionUtils.validateSubModels(model, candidates, true, silent);
    }

    public static List<AtlModel> allSubICGSWithPerfectInformation(AtlModel model, boolean silent) throws Exception {
        System.out.println("Generating sub-models..");
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
        System.out.println("Sub-models generated: " + candidatesPP.size());
        return AbstractionUtils.validateSubModels(model, candidatesPP, false, silent);
    }

    public static void execRV(Set<Monitor> monitors, Collection<String> trace) throws IOException {
        for(String event : trace) {
            System.out.println("Analyse event: " + event);
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

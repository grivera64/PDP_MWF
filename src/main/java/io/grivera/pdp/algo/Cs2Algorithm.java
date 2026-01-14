package io.grivera.pdp.algo;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

import io.grivera.pdp.network.Network;
import io.grivera.pdp.network.node.SensorNode;
import io.grivera.pdp.util.Tuple;

public class Cs2Algorithm extends NetworkAlgorithm {

    private final String cs2Location;
    private long totalProfit;
    private List<Tuple<SensorNode, SensorNode, Integer>> flows;

    public Cs2Algorithm(Network network) {
        this(network, ".");
    }

    public Cs2Algorithm(Network network, String cs2Location) {
        super(network);
        this.cs2Location = cs2Location;
        this.verifyCs2();
    }

    private void verifyCs2() {
        File currDir = new File(this.cs2Location);
        File[] files = currDir.listFiles(f -> f.getName().matches("^cs2(.exe)?$"));
        if (files == null || files.length < 1) {
            throw new IllegalArgumentException(
                    String.format("Couldn't find CS2 program [Searched Dir: \"%s\"]", currDir.getAbsoluteFile()));
        }
    }

    public void silentRun() {
        this.run();
    }

    public RunDetails run() {
        super.run();

        String baseFileName = String.format("cs2_tmp_%s", this.getDateString());
        String tmpInpName = String.format("%s.inp", baseFileName);

        Network network = this.getNetwork();
        network.saveAsCsInp(tmpInpName);

        String cs2FullPath = new File(this.cs2Location).getAbsolutePath();
        Path tmpTxt = null;
        String tmpTxtName;

        long runTime;
        long parseTime;
        try {
            tmpTxt = Files.createTempFile(Path.of("."), baseFileName, ".txt");
            tmpTxtName = tmpTxt.toString();
            String osName = System.getProperty("os.name");
            String mainCommand = String.format("(\"%s/cs2\" < \"%s\") > \"%s\"", cs2FullPath, tmpInpName, tmpTxtName);

            List<String> osCommand;
            if (osName.startsWith("Windows")) {
                osCommand = List.of("cmd", "/C", mainCommand.replace("/", "\\"));
            } else if (osName.startsWith("Mac OS")) {
                osCommand = List.of("/bin/zsh", "-c", mainCommand);
            } else {
                osCommand = List.of("/bin/bash", "-c", mainCommand);
            }

            runTime = -System.nanoTime();
            new ProcessBuilder(osCommand)
                    .directory(new File("."))
                    .start()
                    .waitFor();
            runTime += System.nanoTime();

            parseTime = -System.nanoTime();
            this.parseCs2(tmpTxt.toFile());
            parseTime += System.nanoTime();

            /* Clear the .inp and .txt files after no longer needed */
            tmpTxt.toFile().delete();
        } catch (IOException | InterruptedException e) {
            System.err.printf("ERROR: Terminal not supported for '%s'!\n", System.getProperty("os.name"));
            runTime = 0;
            parseTime = 0;
        } catch (IllegalArgumentException e) {
            System.err.printf("ERROR: Unable to parse CS2 Results: '%s'\n", e.getMessage());
            tmpTxt.toFile().delete();
            runTime = 0;
            parseTime = 0;
        }
        new File(tmpInpName).delete();

        return new RunDetails(runTime, parseTime);
    }

    private void parseCs2(File file) {
        if (!file.exists()) {
            throw new RuntimeException("Running CS2 Failed!");
        }

        String[] lineSplit;
        SensorNode tmpSrc;
        SensorNode tmpDst;
        int srcId;
        int dstId;
        int tmpFlow;

        Network network = this.getNetwork();

        this.flows = new ArrayList<>();
        try (Scanner fileScanner = new Scanner(file)) {
            if (!fileScanner.hasNext()) {
                System.err.println("WARNING: EMPTY FILE!");
            }
            while (fileScanner.hasNext()) {
                lineSplit = fileScanner.nextLine().split("\\s+");

                switch (lineSplit[0].charAt(0)) {
                    case 's':
                        this.totalProfit = -Integer.parseInt(lineSplit[1]);
                        break;
                    case 'f':
                        srcId = Integer.parseInt(lineSplit[1]);
                        dstId = Integer.parseInt(lineSplit[2]);

                        if (srcId < 1 || dstId > network.getDataNodeCount() + network.getStorageNodeCount()) {
                            break;
                        }

                        tmpSrc = network.getSensorNodeByUuid(srcId);
                        tmpDst = network.getSensorNodeByUuid(dstId);

                        tmpFlow = Integer.parseInt(lineSplit[3]);
                        this.flows.add(Tuple.of(tmpSrc, tmpDst, tmpFlow));
                        break;
                    case 'c':
                        break;
                    default:
                        System.err.printf("WARNING: Invalid command '%s' found! Skipping...\n", lineSplit[0]);
                        break;
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Cannot open file!");
        }
    }

    @Override
    public long getTotalProfit() {
        super.getTotalProfit();
        return this.totalProfit;
    }

    @Override
    public long getTotalValue() {
        super.getTotalValue();

        long totalValue = 0;
        Network network = this.getNetwork();
        for (Tuple<SensorNode, SensorNode, Integer> tuple : this.flows) {
            totalValue += network.getDataNodeById(tuple.first().getId()).getOverflowPacketValue() * tuple.third();
        }
        return totalValue;
    }

    @Override
    public long getTotalCost() {
        super.getTotalCost();

        long totalCost = 0;
        Network network = this.getNetwork();
        long minCost;
        for (Tuple<SensorNode, SensorNode, Integer> tuple : this.flows) {
            minCost = network.calculateMinCost(tuple.first(), tuple.second());
            totalCost += minCost * tuple.third();
        }
        return totalCost;
    }

    @Override
    public long getTotalPackets() {
        super.getTotalPackets();

        int totalPackets = 0;
        for (Tuple<SensorNode, SensorNode, Integer> tuple : this.flows) {
            totalPackets += tuple.third();
        }
        return totalPackets;
    }

    private String getDateString() {
        String pattern = "yyyyMMddHHmmss";
        DateFormat df = new SimpleDateFormat(pattern);
        Date today = Calendar.getInstance().getTime();
        return df.format(today);
    }
}

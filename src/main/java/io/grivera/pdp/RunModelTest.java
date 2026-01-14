package io.grivera.pdp;

import java.time.Duration;
import java.util.Scanner;

import io.grivera.pdp.algo.*;
import io.grivera.pdp.network.HashMapSensorNetwork;
import io.grivera.pdp.network.Network;
import io.grivera.pdp.util.NetworkUtil;

public class RunModelTest {
    public static void main(String[] args) {
        // // WARNING: This network configuration takes a LONG time to run
        // // (ETC: 2 days)
        // System.out.println("Generating Network...");
        //
        // Network network = HashMapSensorNetwork.of(
        // 20_000, 20_000,
        // 5_000, 500,
        // 2_500, 100,
        // 2_500, 100,
        // 500_000,
        // 1, 100
        // );

        Scanner scanner = new Scanner(System.in);

        System.out.print("Enter the area dimensions (width in m):\n> ");
        int width = scanner.nextInt();
        System.out.print("Enter the area dimensions (height in m):\n> ");
        int height = scanner.nextInt();

        System.out.print("Enter the number of nodes:\n> ");
        int nodes = scanner.nextInt();
        System.out.print("Enter the transmission range (in m):\n> ");
        int transmissionRange = scanner.nextInt();

        System.out.print("Enter the number of DGs:\n> ");
        int dnCount = scanner.nextInt();
        System.out.print("Enter the packets generated per DG:\n> ");
        int dnPackets = scanner.nextInt();

        System.out.print("Enter the number of SNs:\n> ");
        int snCount = scanner.nextInt();
        System.out.print("Enter the storage spaces per SN:\n> ");
        int snStorage = scanner.nextInt();

        System.out.print("Enter the battery capacity (in microJ):\n> ");
        int batteryCapacity = scanner.nextInt();

        System.out.print("Enter the value range (min):\n> ");
        int valueMin = scanner.nextInt();
        System.out.print("Enter the value range (max):\n> ");
        int valueMax = scanner.nextInt();
        System.out.println();

        System.out.println("Generating Network...");
        Network network = HashMapSensorNetwork.of(
            width, height,
            nodes, transmissionRange,
            dnCount, dnPackets,
            snCount, snStorage,
            batteryCapacity,
            valueMin, valueMax
        );
        System.out.println("Generated Network!");

        System.out.println("Finding MFE...");
        network.setOverflowPackets(100);
        long mfe = NetworkUtil.findMinEnergy(network);
        if (mfe == -1) {
            System.err.println("Find min energy failed!");
            System.exit(1);
        }
        network.setBatteryCapacity(mfe);
        System.out.println("Found MFE!");

        System.out.println(network);
        network.save("generated_network.sn");

        Algorithm algo;
        RunDetails details;
        Duration runTimeDuration;
        Duration parseTimeDuration;
        for (int overflowPackets = 50; overflowPackets <= 100; overflowPackets += 10) {
            System.out.printf("======== d = %3d ========\n", overflowPackets);
            network.setOverflowPackets(overflowPackets);
            algo = new GreedyAlgorithm(network);
            System.out.println("Running Greedy...");
            details = algo.run();
            System.out.println("Ran Greedy!");

            runTimeDuration = Duration.ofNanos(details.runTimeNS());
            parseTimeDuration = Duration.ofNanos(details.parseTimeNS());

            System.out.println("Greedy Details");
            System.out.printf("\tRun Time: %s\n", runTimeDuration);
            System.out.printf("\tParse Time: %s\n", parseTimeDuration);
            System.out.printf("\t\tTotal Packets = %d\n", algo.getTotalPackets());
            System.out.printf("\t\tTotal Value = %d\n", algo.getTotalValue());
            System.out.printf("\t\tTotal Cost = %d\n", algo.getTotalCost());
            System.out.printf("\t\tTotal Profit = %d\n", algo.getTotalProfit());

            algo = new MFAlgorithm(network);
            System.out.println("Running MF...");
            details = algo.run();
            System.out.println("Ran MF!");

            runTimeDuration = Duration.ofNanos(details.runTimeNS());
            parseTimeDuration = Duration.ofNanos(details.parseTimeNS());

            System.out.println("MF Details");
            System.out.printf("\tRun Time: %s\n", runTimeDuration);
            System.out.printf("\tParse Time: %s\n", parseTimeDuration);
            System.out.printf("\t\tTotal Packets = %d\n", algo.getTotalPackets());
            System.out.printf("\t\tTotal Value = %d\n", algo.getTotalValue());
            System.out.printf("\t\tTotal Cost = %d\n", algo.getTotalCost());
            System.out.printf("\t\tTotal Profit = %d\n", algo.getTotalProfit());

            algo = new MWFAlgorithm(network);
            System.out.println("Running MWF...");
            details = algo.run();
            System.out.println("Ran MWF!");

            runTimeDuration = Duration.ofNanos(details.runTimeNS());
            parseTimeDuration = Duration.ofNanos(details.parseTimeNS());

            System.out.println("MWF Details");
            System.out.printf("\tRun Time: %s\n", runTimeDuration);
            System.out.printf("\tParse Time: %s\n", parseTimeDuration);
            System.out.printf("\t\tTotal Packets = %d\n", algo.getTotalPackets());
            System.out.printf("\t\tTotal Value = %d\n", algo.getTotalValue());
            System.out.printf("\t\tTotal Cost = %d\n", algo.getTotalCost());
            System.out.printf("\t\tTotal Profit = %d\n", algo.getTotalProfit());

            System.out.println("=========================");
            System.out.println();
        }
    }
}

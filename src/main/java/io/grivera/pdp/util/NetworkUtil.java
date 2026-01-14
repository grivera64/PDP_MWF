package io.grivera.pdp.util;

import java.util.Iterator;

import io.grivera.pdp.cli.ProgressBar;
import io.grivera.pdp.cli.ProgressBars;

import io.grivera.pdp.network.Network;
import io.grivera.pdp.network.node.DataNode;

public class NetworkUtil {
    public static long findMinEnergy(final Network network) {
        long left = 1_000_000;
        long right = Integer.MAX_VALUE;
        long maxRange = Integer.SIZE;
        final ProgressBar<Long> bar = ProgressBars.range(0, maxRange + 1, 1);
        final Iterator<Long> iter = bar.iterator();
        long mid;
        long solution = -1;

        iter.next();
        while (left <= right && left >= 0 && right >= 0) {
            mid = left + ((right - left) / 2);
            if (mid < 0) {
                System.err.printf("mid = %d because of left = %d, right = %d\n", mid, left, right);
                System.exit(1);
            }
            network.setBatteryCapacity(mid);
            if (network.isMaxFeasible()) {
                solution = mid;
                right = mid - 1;
            } else {
                left = mid + 1;
            }

            iter.next();
        }

        network.setBatteryCapacity(solution);
        return solution;
    }
}

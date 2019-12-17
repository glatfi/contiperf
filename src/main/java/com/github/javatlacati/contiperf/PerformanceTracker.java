/*
 * (c) Copyright 2009-2013 by Volker Bergmann. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, is permitted under the terms of the
 * GNU Lesser General Public License (LGPL), Eclipse Public License (EPL)
 * and the BSD License.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * WITHOUT A WARRANTY OF ANY KIND. ALL EXPRESS OR IMPLIED CONDITIONS,
 * REPRESENTATIONS AND WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE
 * HEREBY EXCLUDED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package com.github.javatlacati.contiperf;

import com.github.javatlacati.contiperf.clock.SystemClock;
import com.github.javatlacati.contiperf.report.ReportContext;
import com.github.javatlacati.contiperf.report.ReportModule;
import com.github.javatlacati.contiperf.util.InvokerProxy;
import com.github.javatlacati.stat.LatencyCounter;

import java.io.PrintWriter;

/**
 * {@link InvokerProxy} that provides performance tracking features.<br>
 * <br>
 * Created: 22.10.2009 16:36:43
 *
 * @author Volker Bergmann
 * @since 1.0
 */
public class PerformanceTracker extends InvokerProxy {

    private final ExecutionConfig executionConfig;
    private final PerformanceRequirement requirement;

    private ReportContext context;

    private Clock[] clocks;
    private LatencyCounter[] counters;
    private boolean trackingStarted;
    private long warmUpFinishedTime;

    public PerformanceTracker(Invoker target,
                              PerformanceRequirement requirement, ReportContext context) {
        this(target, null, requirement, context,
                new Clock[]{new SystemClock()});
    }

    public PerformanceTracker(Invoker target, ExecutionConfig executionConfig,
                              PerformanceRequirement requirement, ReportContext context,
                              Clock[] clocks) {
        super(target);
        this.executionConfig = executionConfig != null ? executionConfig
                : new ExecutionConfig(0);
        this.requirement = requirement;
        this.setContext(context);
        this.clocks = clocks;
        this.counters = null;
        this.trackingStarted = false;
        this.warmUpFinishedTime = -1;
    }

    public void setContext(ReportContext context) {
        this.context = context;
    }

    public LatencyCounter[] getCounters() {
        return counters;
    }

    public void startTracking() {
        reportStart();
        int max = requirement != null ? requirement.getMax() : -1;
        int length = clocks.length;
        this.counters = new LatencyCounter[length];
        for (int i = 0; i < length; i++) {
            LatencyCounter counter = new LatencyCounter(target.toString(),
                    clocks[i].getName(), max >= 0 ? max : 1000);
            this.counters[i] = counter;
            counter.start();
        }
        trackingStarted = true;
    }

    @Override
    public Object invoke(Object[] args) throws Exception {
        long clock0StartTime = clocks[0].getTime();
        long realStartMillis = System.nanoTime() / 1000000;
        if (warmUpFinishedTime == -1) {
            warmUpFinishedTime = realStartMillis + executionConfig.getWarmUp();
        }
        checkState(realStartMillis);
        PerfTestExecutionError perfTestExecutionError = null;
        Object result = null;
        try {
            result = super.invoke(args);
        } catch (PerfTestExecutionError ptee) {
            perfTestExecutionError = ptee;
        }
        int latency = (int) (clocks[0].getTime() - clock0StartTime);
        if (isTrackingStarted()) {
            for (LatencyCounter counter : counters) {
                counter.addSample(latency, perfTestExecutionError);
            }
        }
        reportInvocation(latency, realStartMillis);
        if (null != perfTestExecutionError) {
            if (isAllowedErrors(requirement)) {
                reportError();
            } else {
                throw perfTestExecutionError;
            }
        }
        if (requirement != null && requirement.getMax() >= 0
                && latency > requirement.getMax()
                && executionConfig.isCancelOnViolation()) {
            context.fail("Method " + getId() + " exceeded time limit of "
                    + requirement.getMax() + " ms running " + latency + " ms");
        }
        return result;
    }

    private synchronized void checkState(long callStart) {
        if (callStart >= warmUpFinishedTime && !trackingStarted) {
            startTracking();
        }
    }

    public boolean isTrackingStarted() {
        return trackingStarted;
    }

    public void stopTracking() {
        if (!isTrackingStarted()) {
            throw new RuntimeException(
                    "Trying to stop counter before it was started");
        }
        for (LatencyCounter counter : counters) {
            counter.stop();
        }
        LatencyCounter mainCounter = counters[0];
        mainCounter.printSummary(new PrintWriter(System.out));
        reportCompletion();
        if (!isAllowedErrors(requirement)
                && mainCounter.getAssertionErrors().size() > 0) {
            Throwable p = mainCounter.getAssertionErrors().get(0);
            while (p.getCause() != null && !(p instanceof AssertionError)) {
                p = p.getCause();
            }
            if (p instanceof AssertionError) {
                throw (AssertionError) p;
            } else {
                throw mainCounter.getAssertionErrors().get(0);
            }
        }
        if (requirement != null) {
            checkRequirements(mainCounter);
        }
        this.trackingStarted = false;
    }

    public void clear() {
        counters = null;
    }

    // helper methods
    // --------------------------------------------------------------------------------------------------

    private boolean isAllowedErrors(PerformanceRequirement requirement) {

        return requirement != null && requirement.isAllowedError();
    }

    private void reportStart() {
        for (ReportModule module : context.getReportModules()) {
            module.starting(getId());
        }
    }

    private void reportInvocation(int latency, long callStart) {
        for (ReportModule module : context.getReportModules()) {
            module.invoked(getId(), latency, callStart);
        }
    }

    private void reportCompletion() {
        for (ReportModule module : context.getReportModules()) {
            module.completed(getId(), counters, executionConfig, requirement);
        }
    }

    private void reportError() {
        for (ReportModule module : context.getReportModules()) {
            module.error(getId());
        }
    }

    private void checkRequirements(LatencyCounter mainCounter) {
        long elapsedMillis = mainCounter.duration();
        long requiredMax = requirement.getMax();
        if (requiredMax >= 0 && mainCounter.maxLatency() > requiredMax) {
            context.fail("The maximum latency of " + requiredMax
                    + " ms was exceeded, Measured: "
                    + mainCounter.maxLatency() + " ms");
        }
        long requiredTotalTime = requirement.getTotalTime();
        if (requiredTotalTime >= 0 && elapsedMillis > requiredTotalTime) {
            context.fail("Test run " + getId() + " exceeded timeout of "
                    + requiredTotalTime + " ms running " + elapsedMillis
                    + " ms");
        }
        int requiredThroughput = requirement.getThroughput();
        if (requiredThroughput > 0 && elapsedMillis > 0) {
            long actualThroughput = mainCounter.sampleCount() * 1000
                    / elapsedMillis;
            if (actualThroughput < requiredThroughput) {
                context.fail("Test " + getId() + " had a throughput of only "
                        + actualThroughput + " calls per second, required: "
                        + requiredThroughput + " calls per second");
            }
        }
        int requiredAverage = requirement.getAverage();
        if (requiredAverage >= 0
                && mainCounter.averageLatency() > requiredAverage) {
            context.fail("Average execution time of " + getId()
                    + " exceeded the requirement of " + requiredAverage
                    + " ms, measured " + mainCounter.averageLatency() + " ms");
        }
        for (PercentileRequirement percentile : requirement
                .getPercentileRequirements()) {
            long measuredLatency = mainCounter.percentileLatency(percentile
                    .getPercentage());
            if (measuredLatency > percentile.getMillis()) {
                context.fail(percentile.getPercentage() + "-percentile of "
                        + getId() + " exceeded the requirement of "
                        + percentile.getMillis() + " ms, measured "
                        + measuredLatency + " ms");
            }
        }

        double percentAllowedErrors = requirement.getAllowedErrorsRate();
        if (percentAllowedErrors > 0
                && mainCounter.errorsRate() > percentAllowedErrors) {
            context.fail("The maximum percentage of errors "
                    + (percentAllowedErrors * 100)
                    + "% was exceeded, Measured: "
                    + (mainCounter.errorsRate() * 100) + "%");
        }
    }
}

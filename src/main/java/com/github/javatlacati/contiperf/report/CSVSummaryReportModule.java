/*
 * (c) Copyright 2011 by Volker Bergmann. All rights reserved.
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

package com.github.javatlacati.contiperf.report;

import com.github.javatlacati.contiperf.ExecutionConfig;
import com.github.javatlacati.contiperf.PerformanceRequirement;
import com.github.javatlacati.contiperf.util.ContiPerfUtil;
import com.github.javatlacati.stat.LatencyCounter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

/**
 * Writes summary information of the ContiPerf to a CSV file.<br>
 * <br>
 * Created: 16.01.2011 11:03:46
 *
 * @author Volker Bergmann
 * @since 2.0.0
 */
public class CSVSummaryReportModule extends AbstractReportModule {

    private static final String LINE_SEPARATOR = System
            .getProperty("line.separator");
    public static final char SEPARATOR = ',';

    private static Collection<File> usedFiles = new HashSet<>();

    private File file;

    public CSVSummaryReportModule() {
        this.file = null;
    }

    // ReportModule interface implementation
    // ---------------------------------------------------------------------------

    @Override
    public String getReportReferenceLabel(String serviceId) {
        return serviceId == null ? "CSV Summary" : null;
    }

    @Override
    public String getReportReference(String serviceId) {
        return serviceId == null ? filename() : null;
    }

    @Override
    public void starting(String serviceId) {
        synchronized (usedFiles) {
            file = new File(context.getReportFolder(), filename());
            if (!usedFiles.contains(file) && file.exists()) {
                if (!file.delete()) {
                    throw new RuntimeException(
                            "Previous file version could not be deleted: "
                                    + file);
                }
                usedFiles.add(file);
            }
            if (!file.exists()) {
                writeHeader(); //TODO: perhaps add serviceId
            }
        }
    }

    @Override
    public void completed(String serviceId, LatencyCounter[] counters,
                          ExecutionConfig executionConfig, PerformanceRequirement requirement) {
        writeStats(serviceId, counters);
    }

    // helper methods
    // --------------------------------------------------------------------------------------------------

    private void writeHeader() {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file, true);
            String line = "serviceId,startTime,duration,invocations,min,average,median,90%,95%,99%,max"
                    + LINE_SEPARATOR;
            out.write(line.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ContiPerfUtil.close(out);
        }
    }

    private void writeStats(String serviceId, LatencyCounter[] counters) {
        OutputStream out = null;
        try {
            out = new FileOutputStream(file, true);
            DecimalFormat decForm = new DecimalFormat("0.#",
                    DecimalFormatSymbols.getInstance(Locale.US));
            decForm.setGroupingUsed(false);
            LatencyCounter mainCounter = counters[0];
            String avg = decForm.format(mainCounter.averageLatency());
            String message = new StringBuilder()
                    .append(serviceId).append(SEPARATOR)
                    .append(mainCounter.getStartTime()).append(SEPARATOR)
                    .append(mainCounter.duration()).append(SEPARATOR)
                    .append(mainCounter.sampleCount()).append(SEPARATOR)
                    .append(mainCounter.minLatency()).append(SEPARATOR)
                    .append(avg).append(SEPARATOR)
                    .append(mainCounter.percentileLatency(50)).append(SEPARATOR)
                    .append(mainCounter.percentileLatency(90)).append(SEPARATOR)
                    .append(mainCounter.percentileLatency(95)).append(SEPARATOR)
                    .append(mainCounter.percentileLatency(99)).append(SEPARATOR)
                    .append(mainCounter.maxLatency()).append(LINE_SEPARATOR)
                    .toString();
            out.write(message.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            ContiPerfUtil.close(out);
        }
    }

    private String filename() {
        return "summary.csv";
    }

}

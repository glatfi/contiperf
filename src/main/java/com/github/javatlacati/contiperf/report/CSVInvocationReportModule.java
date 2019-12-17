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
import com.github.javatlacati.stat.LatencyCounter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;

/**
 * {@link ReportModule} that creates a CSV file with one line per invocation,
 * which reports the measured latency in the first column and the start time in
 * the second one.<br>
 * <br>
 * Created: 16.01.2011 17:05:11
 *
 * @author Volker Bergmann
 * @since 2.0.0
 */
public class CSVInvocationReportModule extends AbstractReportModule {

    private static final String FILE_SUFFIX = ".inv.csv";
    public static final char DELIMITER = ',';

    private PrintWriter out;

    @Override
    public String getReportReferenceLabel(String serviceId) {
        return "Invocations as CSV";
    }

    @Override
    public String getReportReference(String serviceId) {
        return serviceId != null ? filename(serviceId) + FILE_SUFFIX : null;
    }

    @Override
    public void starting(String serviceId) {
        createFile(serviceId);
    }

    @Override
    public synchronized void invoked(String serviceId, int latency, long startTime) {
        out.print(latency);
        out.print(DELIMITER);
        out.println(startTime);
    }

    @Override
    public void completed(String serviceId, LatencyCounter[] counters,
                          ExecutionConfig executionConfig, PerformanceRequirement requirement) {
        if (out != null) {
            out.close();
        }
    }

    private void createFile(String serviceId) {
        try {
            String filename = filename(serviceId);
            out = new PrintWriter(new BufferedWriter(new FileWriter(filename)));
            out.println("latency,startTimeNanos");
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }

    private String filename(String serviceId) {
        return context.getReportFolder() + File.separator + serviceId
                + FILE_SUFFIX;
    }

}

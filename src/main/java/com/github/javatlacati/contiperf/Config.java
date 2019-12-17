/*
 * (c) Copyright 2009-2011 by Volker Bergmann. All rights reserved.
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

import com.github.javatlacati.contiperf.report.HtmlReportModule;
import com.github.javatlacati.contiperf.report.ReportContext;

import java.io.File;
import java.util.Locale;

/**
 * Parses and provides the ContiPerf configuration.<br>
 * <br>
 * Created: 18.10.2009 06:46:31
 *
 * @author Volker Bergmann
 * @since 1.0
 */
public class Config {

    private static final String DEFAULT_REPORT_FOLDER_NAME = "contiperf-report";
    public static final String SYSPROP_ACTIVE = "contiperf.active";
    public static final String SYSPROP_CONFIG_FILENAME = "contiperf.config";
    public static final String DEFAULT_CONFIG_FILENAME = "contiperf.config.xml";

    public boolean active() {
        String sysprop = System.getProperty(SYSPROP_ACTIVE);
        return sysprop == null || !"false"
                .equals(sysprop.trim().toLowerCase(Locale.US));
    }

    // helpers
    // ---------------------------------------------------------------------------------------------------------

    public static String getConfigFileName() {
        String filename = System.getProperty(SYSPROP_CONFIG_FILENAME);
        if (filename == null || filename.trim().length() == 0) {
            filename = DEFAULT_CONFIG_FILENAME;
        }
        return filename;
    }

    private static Config instance;

    public static Config instance() {
        if (instance == null) {
            instance = new Config();
        }
        return instance;
    }

    public int getInvocationCount(String testId) {
        // TODO v2.x read config file and support override of annotation
        // settings
        return -1;
    }

    public ReportContext createDefaultReportContext(
            Class<? extends AssertionError> failureClass) {
        File reportFolder = getReportFolder();
        ReportContext context = new ReportContext(reportFolder, failureClass);
        context.addReportModule(new HtmlReportModule());
        return context;
    }

    public File getReportFolder() {
        File targetDir = new File("target");

        String dirName = System.getProperty(DEFAULT_REPORT_FOLDER_NAME);
        if (dirName == null || dirName.trim().length() == 0) {
            dirName = DEFAULT_CONFIG_FILENAME;
        }

        File reportFolder;
        if (targetDir.exists()) {
            reportFolder = new File(targetDir,
                    dirName);
        } else {
            reportFolder = new File(
                    dirName);
        }

        // TODO v2.x determine from config file
        return reportFolder;
    }

}

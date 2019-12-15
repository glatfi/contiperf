/*
 * (c) Copyright 2010 by Volker Bergmann. All rights reserved.
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

package com.github.javatlacati.contiperf.junit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.text.ParseException;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.javatlacati.contiperf.PerfTest;
import com.github.javatlacati.contiperf.Required;
import com.github.javatlacati.contiperf.report.ListReportModule;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

/**
 * Tests the {@link ContiPerfRule}.<br>
 * <br>
 * Created: 29.03.2010 12:35:53
 *
 * @author Volker Bergmann
 * @since 1.0
 */
public class ContiPerfRuleTest {

    @Test
    public void testDefault() throws Throwable {
        TestBean target = check("plain");
        assertEquals(1, target.plainCount.get());
    }

    @Test
    public void testInvocationCount() throws Throwable {
        TestBean target = check("five");
        assertEquals(5, target.fiveCount.get());
    }

    @Test
    public void testDuration() throws Throwable {
        TestBean target = check("duration1000");
        int count = target.duration1000Count.get();
        assertTrue("Expected at least 7 invocations, but had only " + count,
                count > 7);
    }

    @Test
    public void testMedianSuccessful() throws Throwable {
        check("median100Successful");
    }

    @Test(expected = PerformanceRequirementFailedError.class)
    public void testMedianFailed() throws Throwable {
        check("median1Failed");
    }

    @Test
    public void testAverageSuccessful() throws Throwable {
        check("average100Successful");
    }

    @Test(expected = PerformanceRequirementFailedError.class)
    public void testAverageFailed() throws Throwable {
        check("average1Failed");
    }

    @Test
    public void testMaxSuccessful() throws Throwable {
        check("max100Successful");
    }

    @Test
    public void testThroughputSuccessful() throws Throwable {
        check("throughputSuccessful");
    }

    @Test(expected = PerformanceRequirementFailedError.class)
    public void testThroughputFailed() throws Throwable {
        check("throughputFailed");
    }

    @Test
    public void testTotalTimeSuccessful() throws Throwable {
        check("totalTimeSuccessful");
    }

    @Test(expected = PerformanceRequirementFailedError.class)
    public void testTotalTimeFailed() throws Throwable {
        check("totalTimeFailed");
    }

    @Test
    public void testPercentileSuccessful() throws Throwable {
        check("percentileSuccessful");
    }

    @Test(expected = PerformanceRequirementFailedError.class)
    public void testPercentileFailed() throws Throwable {
        check("percentileFailed");
    }

    @Test
    public void testThreads3() throws Throwable {
        TestBean target = check("threads3");
        assertEquals(10, target.threads3IC.get());
        assertEquals(3, target.threads3TC.getThreadCount());
    }

    @Test(expected = RuntimeException.class)
    public void testThreads3Failed() throws Throwable {
        check("threads3Failed");
    }

    @Test()
    public void testThreads2Failed() throws Throwable {
        try {
            check("threads2Failed");
            Assert.fail("Exception attendue");
        } catch (AssertionError re) {
            assertEquals(
                    "Assert test : Thread count : 2, Invocation number : 6",
                    re.getMessage());
        }
    }

    @Test()
    public void testThreads2Error() throws Throwable {
        try {
            check("threads2Error");
            Assert.fail("Exception attendue");
        } catch (RuntimeException re) {
            assertEquals(
                    "java.lang.RuntimeException: Thread count : 2, Invocation number : 4",
                    re.getMessage());
        }
    }

    @Test(expected = PerformanceRequirementFailedError.class)
    public void testCancelOnViolationDefault() throws Throwable {
        TestBean test = new TestBean();
        try {
            check(test, "cancelOnViolationDefault");
        } catch (PerformanceRequirementFailedError e) {
            int count = test.cancelOnViolationDefaultCount.get();
            assertEquals(3, count);
            throw e;
        }
    }

    @Test(expected = PerformanceRequirementFailedError.class)
    public void testCancelOnViolation() throws Throwable {
        TestBean test = new TestBean();
        try {
            check(test, "cancelOnViolation");
        } catch (PerformanceRequirementFailedError e) {
            int count = test.cancelOnViolationCount.get();
            assertEquals(2, count);
            throw e;
        }
    }

    @Test(expected = PerformanceRequirementFailedError.class)
    public void testDontCancelOnViolation() throws Throwable {
        TestBean test = new TestBean();
        try {
            check(test, "dontCancelOnViolation");
        } catch (PerformanceRequirementFailedError e) {
            int count = test.dontCancelOnViolationCount.get();
            assertEquals(3, count);
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testCancelOnError() throws Throwable {
        TestBean test = new TestBean();
        try {
            check(test, "cancelOnError");
        } catch (RuntimeException e) {
            int count = test.cancelOnErrorCount.get();
            assertEquals(1, count);
            throw e;
        }
    }

    @Test(expected = RuntimeException.class)
    public void testCancelOnErrorWithAnnotation0() throws Throwable {
        TestBean test = new TestBean();
        try {
            check(test, "cancelOnErrorWithAnnotation0");
        } catch (RuntimeException e) {
            int count = test.cancelOnErrorCountWithAnnotation0.get();
            assertEquals(1, count);
            throw e;
        }
    }

    @Test(expected = PerformanceRequirementFailedError.class)
    public void testCancelOnErrorWithAnnotation01() throws Throwable {
        TestBean test = new TestBean();
        try {
            check(test, "cancelOnErrorWithAnnotation01");
        } catch (PerformanceRequirementFailedError e) {
            int count = test.cancelOnErrorCountWithAnnotation01.get();
            assertEquals(5, count);
            throw e;
        }
    }

    @Test
    public void testDontCancelOnErrorWithAnnotation() throws Throwable {
        TestBean test = new TestBean();
        check(test, "dontCancelOnErrorWithAnnotation");
        int count = test.dontCancelOnErrorWithAnnotation.get();
        assertEquals(100, count);
    }

    // helper methods
    // --------------------------------------------------------------------------------------------------

    private TestBean check(String methodName) throws NoSuchMethodException,
            Throwable {
        return check(new TestBean(), methodName);
    }

    private TestBean check(TestBean target, String methodName)
            throws NoSuchMethodException, Throwable {
        ContiPerfRule rule = new ContiPerfRule(new ListReportModule());
        Method method = TestBean.class.getDeclaredMethod(methodName,
                new Class<?>[0]);
        Statement base = new InvokerStatement(target, method);
        FrameworkMethod fwMethod = new FrameworkMethod(method);
        Statement perfTestStatement = rule.apply(base, fwMethod, target);
        perfTestStatement.evaluate();
        return target;
    }

    public static class TestBean {

        private ThreadCounter threads2TCFailed = new ThreadCounter();
        public AtomicInteger threads2ICFailed = new AtomicInteger();
        private ThreadCounter threads2TCError = new ThreadCounter();
        public AtomicInteger threads2ICError = new AtomicInteger();

        public AtomicInteger plainCount = new AtomicInteger();

        public void plain() {
            plainCount.incrementAndGet();
        }

        public AtomicInteger fiveCount = new AtomicInteger();

        @PerfTest(invocations = 5)
        public void five() {
            fiveCount.incrementAndGet();
        }

        public AtomicInteger duration1000Count = new AtomicInteger();

        @PerfTest(duration = 1000)
        public void duration1000() throws Exception {
            Thread.sleep(100);
            duration1000Count.incrementAndGet();
        }

        @PerfTest(invocations = 5)
        @Required(median = 100)
        public void median100Successful() {
        }

        @PerfTest(invocations = 5)
        @Required(median = 1)
        public void median1Failed() throws InterruptedException {
            Thread.sleep(10);
        }

        @PerfTest(invocations = 5)
        @Required(average = 100)
        public void average100Successful() {
        }

        @PerfTest(invocations = 5)
        @Required(average = 1)
        public void average1Failed() throws InterruptedException {
            Thread.sleep(10);
        }

        @Required(max = 100)
        public void max100Successful() {
        }

        @PerfTest(invocations = 10)
        @Required(throughput = 10)
        public void throughputSuccessful() throws InterruptedException {
            Thread.sleep(10);
        }

        @PerfTest(invocations = 10)
        @Required(throughput = 150)
        public void throughputFailed() throws InterruptedException {
            Thread.sleep(10);
        }

        @PerfTest(invocations = 10)
        @Required(totalTime = 500)
        public void totalTimeSuccessful() throws InterruptedException {
            Thread.sleep(10);
        }

        @PerfTest(invocations = 10)
        @Required(totalTime = 50)
        public void totalTimeFailed() throws InterruptedException {
            Thread.sleep(10);
        }

        @PerfTest(invocations = 10)
        @Required(percentiles = "90:50")
        public void percentileSuccessful() throws InterruptedException {
            Thread.sleep(10);
        }

        @PerfTest(invocations = 10)
        @Required(percentiles = "90:5")
        public void percentileFailed() throws InterruptedException {
            Thread.sleep(10);
        }

        ThreadCounter threads3TC = new ThreadCounter();
        public AtomicInteger threads3IC = new AtomicInteger();

        @PerfTest(invocations = 10, threads = 3)
        public void threads3() throws InterruptedException {
            threads3TC.get();
            threads3IC.incrementAndGet();
            Thread.sleep(100);
        }

        @PerfTest(invocations = 10, threads = 3)
        public void threads3Failed() throws ParseException {
            throw new ParseException("", 0);
        }


        @PerfTest(invocations = 20, threads = 2)
        @Required(max = 10)
        public void threads2Failed() throws ParseException,
                InterruptedException {
            threads2TCFailed.get();
            threads2ICFailed.incrementAndGet();
            Thread.sleep(20);
            if (threads2ICFailed.get() % 6 == 0) {
                assertTrue("Assert test : " + "Thread count : "
                                + threads2TCFailed.getThreadCount()
                                + ", Invocation number : " + threads2ICFailed.get(),
                        false);
            }
        }

        @PerfTest(invocations = 10, threads = 2)
        @Required(max = 10)
        public void threads2Error() throws ParseException, InterruptedException {
            threads2TCError.get();
            threads2ICError.incrementAndGet();
            Thread.sleep(20);
            if (threads2ICError.get() % 4 == 0) {
                throw new RuntimeException("Thread count : "
                        + threads2TCError.getThreadCount()
                        + ", Invocation number : " + threads2ICError.get());
            }
        }

        public AtomicInteger cancelOnViolationDefaultCount = new AtomicInteger();

        @PerfTest(invocations = 3)
        @Required(max = 200)
        public void cancelOnViolationDefault() throws InterruptedException {
            int n = cancelOnViolationDefaultCount.incrementAndGet();
            Thread.sleep(n * 150);
        }

        public AtomicInteger cancelOnViolationCount = new AtomicInteger();

        @Required(max = 200)
        @PerfTest(invocations = 3, cancelOnViolation = true)
        public void cancelOnViolation() throws InterruptedException {
            int n = cancelOnViolationCount.incrementAndGet();
            Thread.sleep(n * 150);
        }

        public AtomicInteger dontCancelOnViolationCount = new AtomicInteger();

        @Required(max = 200)
        @PerfTest(invocations = 3, cancelOnViolation = false)
        public void dontCancelOnViolation() throws InterruptedException {
            int n = dontCancelOnViolationCount.incrementAndGet();
            Thread.sleep(n * 150);
        }

        public AtomicInteger cancelOnErrorCount = new AtomicInteger();

        @PerfTest(invocations = 5)
        public void cancelOnError() throws InterruptedException {
            cancelOnErrorCount.incrementAndGet();
            throw new RuntimeException();
        }

        public AtomicInteger cancelOnErrorCountWithAnnotation0 = new AtomicInteger();

        @Required(allowedErrorsRate = 0.)
        @PerfTest(invocations = 5)
        public void cancelOnErrorWithAnnotation0() throws InterruptedException {
            cancelOnErrorCountWithAnnotation0.incrementAndGet();
            throw new RuntimeException();
        }

        public AtomicInteger cancelOnErrorCountWithAnnotation01 = new AtomicInteger();

        @Required(allowedErrorsRate = 0.1)
        @PerfTest(invocations = 5)
        public void cancelOnErrorWithAnnotation01() throws InterruptedException {
            cancelOnErrorCountWithAnnotation01.incrementAndGet();
            throw new RuntimeException();
        }

        public AtomicInteger dontCancelOnErrorWithAnnotation = new AtomicInteger();

        @Required(allowedErrorsRate = 0.11)
        @PerfTest(invocations = 100)
        public void dontCancelOnErrorWithAnnotation()
                throws InterruptedException {
            int count = dontCancelOnErrorWithAnnotation.incrementAndGet();
            if (count % 10 == 0) { // 1 test sur 10 remonte une exception
                throw new RuntimeException();
            }
        }

    }

    public static class InvokerStatement extends Statement {

        public TestBean target;
        public Method method;

        public InvokerStatement(TestBean target, Method method) {
            this.target = target;
            this.method = method;
        }

        @Override
        public void evaluate() throws Throwable {
            method.invoke(target);
        }
    }

}

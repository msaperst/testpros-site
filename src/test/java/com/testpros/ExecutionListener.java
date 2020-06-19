package com.testpros;

import com.testpros.fast.reporter.Step.Status;
import org.apache.commons.io.FileUtils;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import java.io.File;
import java.io.FileNotFoundException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.testpros.TestBase.testResults;

public class ExecutionListener extends RunListener {

    List<String> testCases = new ArrayList<>();
    String testResultTemplate = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \n" +
            "\"http://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html>\n" +
            "<head>\n" +
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
            "<style>.title { background:lightgrey; } .PASS { background:darkseagreen; } .FAIL { background:lightcoral; } table { border:2px solid darkgrey; border-collapse:collapse; } td,th { border:1px solid grey; padding:10px; }</style>\n" +
            "<title>$testSuiteName Overall Results</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "<h1>$testSuiteName Overall Results</h1>\n" +
            "<table>\n" +
            "<tr class='title'><th>Total Tests Run</th><th>Passed</th><th>Failed</th><th>Ignored</th><th>Total Time</th></tr>\n" +
            "<tr class='$overallResult'><td>$totalTests</td><td>$testsPassed</td><td>$testFailed</td><td>$testsIgnored</td><td>$totalTime</td></tr>\n" +
            "</table>\n" +
            "<h2><a href='zap.html'>Overall Security Results</a></h2>\n" +
            "<table>\n" +
            "<tr class='title'><th>High</th><th>Medium</th><th>Low</th><th>Informational</th></tr>\n" +
            "<tr><td><a href='zap.html#high'>$securityHigh</a></td><td><a href='zap.html#medium'>$securityMedium</a></td>" +
            "<td><a href='zap.html#low'>$securityLow</a></td><td><a href='zap.html#info'>$securityInfo</a></td></tr>\n" +
            "</table>\n" +
            "<h2>Individual Test Results</h2>\n" +
            "<table>\n" +
            "<tr class='title'><th>Test Name</th><th>Status</th><th>Links</th></tr>\n" +
            "$testResults" +
            "</table>\n" +
            "</body>\n" +
            "</html>";

    /**
     * Called before any tests have been run.
     */
    @Override
    public void testRunStarted(Description description) throws java.lang.Exception {
        System.out.println("Number of tests to execute : " + description.testCount());
    }

    /**
     * Called when all tests have finished
     */
    @Override
    public void testRunFinished(Result result) throws java.lang.Exception {
        System.out.println("Number of tests executed : " + result.getRunCount());
        Status overallStatus;
        if (result.getFailureCount() > 0) {
            overallStatus = Status.FAIL;
        } else {
            overallStatus = Status.PASS;
        }
        // TODO - fix testSuiteName
        String report = testResultTemplate.replace("$testSuiteName", "Test Suite")
                .replace("$overallResult", overallStatus.toString())
                .replace("$securityHigh", findInFile(new File(testResults, "zap.html"), Pattern.compile(".*#high.*>(\\d+)<.*")))
                .replace("$securityMedium", findInFile(new File(testResults, "zap.html"), Pattern.compile(".*#medium.*>(\\d+)<.*")))
                .replace("$securityLow", findInFile(new File(testResults, "zap.html"), Pattern.compile(".*#low.*>(\\d+)<.*")))
                .replace("$securityInfo", findInFile(new File(testResults, "zap.html"), Pattern.compile(".*#info.*>(\\d+)<.*")))
                .replace("$totalTests", String.valueOf(result.getRunCount()))
                .replace("$testsPassed", String.valueOf(result.getRunCount() - result.getFailureCount() - result.getIgnoreCount()))
                .replace("$testFailed", String.valueOf(result.getFailureCount()))
                .replace("$testsIgnored", String.valueOf(result.getIgnoreCount()))
                .replace("$totalTime", String.valueOf(result.getRunTime()) + " ms")
                .replace("$testResults", "")
                .replaceAll("\\$(.*?)Status", "PASS");
        File reportFile = new File(testResults, "index.html");
        FileUtils.writeStringToFile(reportFile, report, Charset.defaultCharset());
    }

    /**
     * Called when an atomic test is about to be started.
     */
    @Override
    public void testStarted(Description description) throws java.lang.Exception {
        System.out.println("Starting execution of test case : " + description.getMethodName());
    }

    private String findInFile(File haystack, Pattern needle) {
        try {
            Scanner scanner = new Scanner(haystack);

            //now read the file line by line...
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                Matcher m = needle.matcher(line);
                if(m.find()) {
                    return m.group(1);
                }
            }
        } catch(FileNotFoundException e) {
            //handle this
        }
        return "";
    }

    private void recordTestRun(String testCaseName, Status status) {
        String stringStatus;
        if (status == null) {
            stringStatus = "$" + testCaseName + "Status";
        } else {
            stringStatus = status.toString();
        }
        if (!testCases.contains(testCaseName)) {
            testResultTemplate = testResultTemplate.replace("$testResults",
                    "<tr class=" + stringStatus + ">" +
                            "<td>" + testCaseName + "</td>" +
                            "<td>" + stringStatus + "</td>" +
                            "<td><a href='" + testCaseName + ".accessibility.html'>Accessibility</a> " +
                            "<a href='" + testCaseName + ".selenium.html'>Selenium</a></td></tr>\n$testResults");
            testCases.add(testCaseName);
        }
    }

    /**
     * Called when an atomic test has finished, whether the test succeeds or fails.
     */
    @Override
    public void testFinished(Description description) throws java.lang.Exception {
        System.out.println("Finished execution of test case : " + description.getMethodName());
        recordTestRun(description.getMethodName(), null);
    }

    /**
     * Called when an atomic test fails.
     */
    @Override
    public void testFailure(Failure failure) throws java.lang.Exception {
        System.out.println("Execution of test case failed : " + failure.getMessage());
        recordTestRun(failure.getDescription().getMethodName(), Status.FAIL);
    }

    /**
     * Called when a test will not be run, generally because a test method is annotated with Ignore.
     */
    @Override
    public void testIgnored(Description description) throws java.lang.Exception {
        System.out.println("Execution of test case ignored : " + description.getMethodName());
        //TODO - change to skip
        recordTestRun(description.getMethodName(), Status.CHECK);

    }
}

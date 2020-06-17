package com.testpros;

import com.accessibility.AccessibilityScanner;
import com.accessibility.Result;
import com.testpros.fast.By;
import com.testpros.fast.ChromeDriver;
import com.testpros.fast.WebDriver;
import com.testpros.fast.WebElement;
import com.testpros.fast.reporter.Reporter;
import com.testpros.fast.reporter.Step;
import com.testpros.fast.reporter.Step.Status;
import io.github.bonigarcia.wdm.WebDriverManager;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TestName;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class TestBase {

    private static final String EXPECTED_ELEMENT = "Expected element '";
    private static final String ELEMENT = "Element '";
    private static final String END_CELL = "</td>";
    private static final String START_CELL = "<td>";
    ThreadLocal<WebDriver> drivers = new ThreadLocal<>();

    String url = "http://www.test.testpros.com/";

    @Rule
    public TestName name = new TestName();

    File testResults = new File("target/failsafe-reports/");
    String seleniumHtmlTemplate = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \n" +
            "\"http://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html>\n" +
            "<head>\n" +
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
            "<style>.PASS { color:green; } .FAIL { color:red; } table { border:2px solid darkgrey; border-collapse:collapse; } td,th { border:1px solid grey; padding:10px; }</style>\n" +
            "<title>$testCaseName Selenium Results</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "<h1>$testCaseName Selenium Results</h1>\n" +
            "<h2 class='$testCaseStatus'><span>$testCaseStatus</span> <span>$testCaseTime<span></h2>\n" +
            "<table>\n" +
            "<tr><th>Step</th><th>Action</th><th>Expected</th><th>Actual</th><th>Screenshot</th><th>Status</th><th>Time (ms)</th></tr>\n" +
            "$rows" +
            "</table>\n" +
            "</body>\n" +
            "</html>";
    String accessibilityHtmlTemplate = "<!DOCTYPE html PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \n" +
            "\"http://www.w3.org/TR/html4/loose.dtd\">\n" +
            "<html>\n" +
            "<head>\n" +
            "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">\n" +
            "<style>.PASS { color:green; } .FAIL { color:red; } .row { display: flex; } .column { flex:50%; padding:10px; }</style>\n" +
            "<title>$testCaseName Accessibility Results</title>\n" +
            "</head>\n" +
            "<body>\n" +
            "<h1>$testCaseName Accessibility Results</h1>\n" +
            "<h2 class='$testCaseStatus'><span>$testCaseStatus</span> <span>$page</span></h2>\n" +
            "<div>$report</div>\n" +
            "<div class='row'>\n" +
            "<div class='column'><h2>Errors</h2>$errors</div>\n" +
            "<div class='column'><h2>Warnings</h2>$warnings</div>\n" +
            "</div>\n" +
            "<div><img height='200' src='data:image/png;base64,$screenshot'/></div>\n" +
            "</body>\n" +
            "</html>";

    @Before
    public void setupDriver() {
        WebDriverManager.chromedriver().forceCache().setup();
        WebDriver driver = new ChromeDriver();
        if (System.getProperty("url") != null) {
            url = System.getProperty("url");
        }
        driver.get(url);
        drivers.set(driver);
    }

    @After
    public void tearDownDriver() throws IOException {
        WebDriver driver = drivers.get();
        // check for accessibility issues
        AccessibilityScanner scanner = new AccessibilityScanner(driver.getDriver());
        Map<String, Object> auditReport = scanner.runAccessibilityAudit();
        String title = driver.getTitle();
        String currentUrl = driver.getCurrentUrl();
        // kill the driver
        driver.quit();
        // write out my reports
        generateSeleniumReport(driver.getReporter());
        generateAccessibilityReport(auditReport, title, currentUrl);
    }

    void generateSeleniumReport(Reporter reporter) throws IOException {
        StringBuilder steps = new StringBuilder();
        for (Step step : reporter.getSteps()) {
            steps.append("<tr>");
            steps.append(START_CELL).append(step.getNumber()).append(END_CELL);
            steps.append(START_CELL).append(step.getAction()).append(END_CELL);
            steps.append(START_CELL).append(step.getExpected()).append(END_CELL);
            steps.append(START_CELL).append(step.getActual()).append(END_CELL);
            if (step.getScreenshot() != null) {
                //TODO - toggle images
                steps.append(START_CELL).append("<img height='200' src='data:image/png;base64,").append(step.getScreenshot()).append("'/>").append(END_CELL);
            } else {
                steps.append("<td></td>");
            }
            steps.append("<td class='").append(step.getStatus()).append("'>").append(step.getStatus()).append(END_CELL);
            steps.append(START_CELL).append(step.getTime()).append(END_CELL);
            steps.append("</tr>");
        }
        String report = seleniumHtmlTemplate.replace("$testCaseName", name.getMethodName())
                .replace("$testCaseStatus", reporter.getStatus().toString())
                .replace("$testCaseTime", reporter.getRunTime() + " ms")
                .replace("$rows", steps.toString());
        File reportFile = new File(testResults, name.getMethodName() + ".selenium.html");
        FileUtils.writeStringToFile(reportFile, report, Charset.defaultCharset());
    }

    void generateAccessibilityReport(Map<String, Object> auditReport, String title, String currentUrl) throws IOException {
        List<Result> errors;
        List<Result> warnings;
        String plainReport;
        String screenshot;
        if (auditReport.containsKey("error")) {
            errors = (List<Result>) auditReport.get("error");
        } else {
            errors = new ArrayList<>();
        }
        if (auditReport.containsKey("warning")) {
            warnings = (List<Result>) auditReport.get("warning");
        } else {
            warnings = new ArrayList<>();
        }
        if (auditReport.containsKey("plain_report")) {
            plainReport = auditReport.get("plain_report").toString().replace("\n", "\n<br/>");
        } else {
            plainReport = "";
        }
        if (auditReport.containsKey("screenshot")) {
            screenshot = Base64.getEncoder().encodeToString((byte[]) auditReport.get("screenshot"));
        } else {
            screenshot = "";
        }
        Status status = errors.isEmpty() ? Status.PASS : Status.FAIL;
        String report = accessibilityHtmlTemplate.replace("$testCaseName", name.getMethodName())
                .replace("$testCaseStatus", status.toString())
                .replace("$page", "<a target='_blank' href='" + currentUrl + "'>" + title + "</a>")
                .replace("$report", plainReport)
                .replace("$errors", getResultTable(errors))
                .replace("$warnings", getResultTable(warnings))
                .replace("$screenshot", screenshot);
        File reportFile = new File(testResults, name.getMethodName() + ".accessibility.html");
        FileUtils.writeStringToFile(reportFile, report, Charset.defaultCharset());
    }

    private String getResultTable(List<Result> results) {
        StringBuilder resultBuilder = new StringBuilder("<table><tr><th>Rule</th><th>URL</th><th>Elements</th></tr>\n");
        for (Result result : results) {
            resultBuilder.append("<tr>")
                    .append(START_CELL).append(result.getRule()).append(END_CELL)
                    .append(START_CELL).append(result.getUrl()).append(END_CELL)
                    .append(START_CELL);
            for (String element : result.getElements()) {
                resultBuilder.append(element).append("<br/><br/>");
            }
            resultBuilder.append("</td></tr>\n");
        }
        resultBuilder.append("</table>");
        return resultBuilder.toString();
    }

    void assertEquals(Object actual, Object expected, String expectedString, String actualString) {
        Step step = new Step("", expectedString);
        try {
            org.junit.Assert.assertEquals(expected, actual);
            step.setPassed();
        } catch (AssertionError e) {
            step.setFailed();
            throw e;
        } finally {
            step.setActual(actualString);
            drivers.get().getReporter().addStep(step);
        }
    }

    void assertElementTextEquals(String expected, WebElement element) {
        String actual = element.getText();
        assertEquals(actual, expected, EXPECTED_ELEMENT + element.getAttribute("resourceId") +
                "' to have text '" + expected + "'", ELEMENT + element.getAttribute("resourceId") +
                "' has text '" + actual + "'");
    }

    void assertElementTextEquals(String expected, By element) {
        String actual = drivers.get().findElement(element).getText();
        assertEquals(actual, expected, EXPECTED_ELEMENT + element + "' to have text '" + expected + "'",
                ELEMENT + element + "' has text '" + actual + "'");
    }

    void assertElementDisplayed(WebElement element) {
        assertEquals(true, element.isDisplayed(), EXPECTED_ELEMENT +
                element + "' to be displayed", ELEMENT + element + "' is visible");
    }

    void assertElementDisplayed(By element) {
        assertEquals(true, drivers.get().findElement(element).isDisplayed(), EXPECTED_ELEMENT +
                element + "' to be displayed", ELEMENT + element + "' is visible");
    }
}

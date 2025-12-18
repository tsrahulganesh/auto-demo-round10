
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Date;

public class csbbankTest {

    public static void main(String[] args) {
        // Set ChromeDriver path (adjust if needed)
        System.setProperty("webdriver.chrome.driver",
                "C:\\Users\\FA62XEA\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        // Prepare report folders
        File reportDir = new File("report");
        File screenshotDir = new File(reportDir, "screenshots");
        reportDir.mkdirs();
        screenshotDir.mkdirs();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File reportFile = new File(reportDir, "LoginReport_" + timestamp + ".html");

        // Chrome options
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--remote-allow-origins=*");
        options.addArguments("--disable-notifications");

        WebDriver driver = new ChromeDriver(options);
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(12));

        long start = System.currentTimeMillis();
        String status = "PASS";
        String message = "";
        String pageTitle = "";
        String currentUrl = "";
        String screenshotPath = "";
        String dashboardHeaderText = "";

        try {
            driver.manage().window().maximize();
            driver.get("https://csbbankonline-p2.onlinebank.com/SignIn.aspx");

            // Username
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//input[contains(@class,'form-control') and contains(@class,'component-group')]")
            )).sendKeys("Retailuser1");

            // Password
            wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.cssSelector("input.signin-password")
            )).sendKeys("Google@44566");

            // Sign In button
            wait.until(ExpectedConditions.elementToBeClickable(
                    By.xpath("//*[contains(@class,'btn') and contains(@class,'btn-primary')]")
            )).click();

            // Wait for the correct page title (use title, not URL)
            wait.until(ExpectedConditions.titleContains("Account Summary - COMMUNITY STATE BANK"));

            currentUrl = driver.getCurrentUrl();
            pageTitle = driver.getTitle();
            System.out.println("Dashboard Title: " + pageTitle);

            // Optional: try grabbing a visible header on the page (replace with real selector if available)
            try {
                // Example generic header probe. Please replace with a real selector from CSB dashboard.
                WebElement header = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.xpath("//h1|//h2|//h3|//h4|//h5|//h6")
                ));
                dashboardHeaderText = header.getText();
                System.out.println("Dashboard Header Text: " + dashboardHeaderText);
            } catch (Exception ignored) {
                // If no reliable header, title verification is sufficient
            }

            // Simple validation based on title
            if (!"Account Summary - COMMUNITY STATE BANK".equals(pageTitle)) {
                status = "FAIL";
                message = "Unexpected title: " + pageTitle;
            } else {
                message = "Login and Account Summary - COMMUNITY STATE BANK verified.";
            }

            // Optional: add a pragmatic URL check, if there is a stable pattern (adjust if you know it)
            // if (!currentUrl.contains("onlinebank.com")) {
            //     status = "FAIL";
            //     message = "Expected to be on CSB OnlineBank domain. Current URL: " + currentUrl;
            // }

        } catch (Exception e) {
            status = "FAIL";
            message = "Exception: " + e.getClass().getSimpleName() + " - " + safe(e.getMessage());

            // Screenshot on failure
            try {
                screenshotPath = new File(screenshotDir, "failure_" + timestamp + ".png").getAbsolutePath();
                TakesScreenshot ts = (TakesScreenshot) driver;
                File src = ts.getScreenshotAs(OutputType.FILE);
                Files.copy(src.toPath(), Path.of(screenshotPath));
            } catch (Exception ignored) { /* may fail if driver already quit */ }
        } finally {
            try {
                driver.quit();
            } catch (Exception ignored) {
            }

            long end = System.currentTimeMillis();
            long durationMs = end - start;

            // Write HTML report
            writeHtmlReport(reportFile, status, message, pageTitle, dashboardHeaderText,
                    currentUrl, screenshotPath, start, end, durationMs);

            System.out.println("HTML report: " + reportFile.getAbsolutePath());
            if (!screenshotPath.isEmpty()) {
                System.out.println("Failure screenshot: " + screenshotPath);
            }
        }
    }

    private static void writeHtmlReport(File reportFile,
                                        String status,
                                        String message,
                                        String title,
                                        String headerText,
                                        String url,
                                        String screenshotPath,
                                        long start,
                                        long end,
                                        long durationMs) {
        String startStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(start));
        String endStr = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(end));
        String statusClass = "PASS".equalsIgnoreCase(status) ? "status-pass" : "status-fail";

        StringBuilder sb = new StringBuilder();
        // NOTE: We are writing real HTML (no &lt;/&gt; escaping of structural tags).
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n")
                .append("<title>Account Summary - COMMUNITY STATE BANK Login Report</title>\n<style>\n")
                .append("body{font-family:Arial,sans-serif;margin:20px;} .card{border:1px solid #ddd;border-radius:8px;padding:16px;max-width:900px;}")
                .append(".status-pass{color:#0b7a0b;font-weight:bold;} .status-fail{color:#b00020;font-weight:bold;} .row{margin:8px 0;} ")
                .append("code{background:#f7f7f7;padding:2px 6px;border-radius:4px;} a{color:#0069c0;text-decoration:none;}\n")
                .append("hr{border:none;border-top:1px solid #eee;margin:14px 0;}\n")
                .append("</style>\n</head>\n<body>\n<h2>Account Summary - COMMUNITY STATE BANK Login Report</h2>\n<div class=\"card\">\n")
                .append("<div class=\"row\"><strong>Status:</strong> <span class=\"").append(statusClass).append("\">")
                .append(escapeHtml(status)).append("</span></div>\n")
                .append("<div class=\"row\"><strong>Message:</strong> ").append(escapeHtml(message)).append("</div>\n")
                .append("<div class=\"row\"><strong>Start:</strong> ").append(startStr).append("</div>\n")
                .append("<div class=\"row\"><strong>End:</strong> ").append(endStr).append("</div>\n")
                .append("<div class=\"row\"><strong>Duration:</strong> ").append(durationMs).append(" ms</div>\n<hr/>\n")
                .append("<div class=\"row\"><strong>Page Title:</strong> <code>").append(escapeHtml(title)).append("</code></div>\n")
                .append("<div class=\"row\"><strong>Dashboard Header:</strong> <code>").append(escapeHtml(headerText)).append("</code></div>\n")
                .append("<div class=\"row\"><strong>URL:</strong> <a href=\"").append(escapeHtml(url)).append("\" target=\"_blank\">")
                .append(escapeHtml(url)).append("</a></div>\n");

        if (screenshotPath != null && !screenshotPath.isEmpty()) {
            String rel = relPathForReport(reportFile, screenshotPath);
            sb.append("<div class=\"row\"><strong>Screenshot:</strong><br/>\n")
                    .append("<img src=\"").append(escapeHtml(rel)).append("\" alt=\"Screenshot\" style=\"max-width:100%;border:1px solid #ccc;\"/>\n")
                    .append("</div>\n");
        }
        sb.append("</div>\n</body>\n</html>");

        try (FileWriter fw = new FileWriter(reportFile)) {
            fw.write(sb.toString());
        } catch (IOException e) {
            System.err.println("Failed to write HTML report: " + e.getMessage());
        }
    }

    private static String relPathForReport(File reportFile, String screenshotAbs) {
        try {
            Path reportDir = reportFile.getParentFile().toPath().toAbsolutePath();
            Path screenshot = Path.of(screenshotAbs).toAbsolutePath();
            return reportDir.relativize(screenshot).toString().replace("\\", "/");
        } catch (Exception e) {
            return screenshotAbs;
        }
    }

    private static String safe(String s) {
        return (s == null) ? "" : s;
    }

    // Escape only dynamic content (do NOT escape structural HTML tags)
    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;");
    }
}
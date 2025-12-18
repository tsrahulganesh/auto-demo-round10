
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

public class HrmsloginTest {

    public static void main(String[] args) {
        // Set ChromeDriver path
        System.setProperty("webdriver.chrome.driver",
                "C:\\Users\\FA62XEA\\Downloads\\chromedriver-win64\\chromedriver-win64\\chromedriver.exe");

        // Prepare report folders
        File reportDir = new File("report");
        File screenshotDir = new File(reportDir, "screenshots");
        reportDir.mkdirs();
        screenshotDir.mkdirs();

        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        File reportFile = new File(reportDir, "LoginReport_" + timestamp + ".html");

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
            driver.get("https://opensource-demo.orangehrmlive.com/web/index.php/auth/login");

            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username"))).sendKeys("Admin");
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("password"))).sendKeys("admin123");
            wait.until(ExpectedConditions.elementToBeClickable(By.cssSelector("button[type='submit']"))).click();

            // Wait until Dashboard URL appears
            wait.until(ExpectedConditions.urlContains("/dashboard"));
            currentUrl = driver.getCurrentUrl();

            // Title (tab title)
            pageTitle = driver.getTitle(); // Expected: "OrangeHRM"
            System.out.println("Dashboard Title: " + pageTitle);

            // Optional: in-page "Dashboard" header text
            try {
                WebElement header = wait.until(ExpectedConditions.visibilityOfElementLocated(
                        By.cssSelector("header h6.oxd-text--h6, .oxd-topbar-header-breadcrumb h6")));
                dashboardHeaderText = header.getText();
                System.out.println("Dashboard Header Text: " + dashboardHeaderText);
            } catch (Exception ignored) {
                // If locator changes, title verification is enough
            }

            // Simple validation
            if (!currentUrl.contains("/dashboard")) {
                status = "FAIL";
                message = "Dashboard URL not detected.";
            } else if (!"OrangeHRM".equals(pageTitle)) {
                status = "FAIL";
                message = "Unexpected title: " + pageTitle;
            } else {
                message = "Login and dashboard verified.";
            }

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
            try { driver.quit(); } catch (Exception ignored) {}

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
        String endStr   = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(end));
        String statusClass = "PASS".equalsIgnoreCase(status) ? "status-pass" : "status-fail";

        StringBuilder sb = new StringBuilder();
        sb.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n<meta charset=\"UTF-8\">\n")
                .append("<title>OrangeHRM Login Report</title>\n<style>\n")
                .append("body{font-family:Arial,sans-serif;margin:20px;} .card{border:1px solid #ddd;border-radius:8px;padding:16px;max-width:800px;}")
                .append(".status-pass{color:#0b7a0b;font-weight:bold;} .status-fail{color:#b00020;font-weight:bold;} .row{margin:8px 0;} ")
                .append("code{background:#f7f7f7;padding:2px 6px;border-radius:4px;} a{color:#0069c0;text-decoration:none;}\n")
                .append("</style>\n</head>\n<body>\n<h2>OrangeHRM Login Report</h2>\n<div class=\"card\">\n")
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
                    .append("<img src=\"").append(rel).append("\" alt=\"Screenshot\" style=\"max-width:100%;border:1px solid #ccc;\"/>\n")
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

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }
}

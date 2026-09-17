package listeners;

import io.qameta.allure.Allure;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;

import java.io.ByteArrayInputStream;

// Captures a screenshot only when a test fails, attaching it directly to Allure - no on-disk
// copy, since Allure's own attachment storage (target/allure-results) is already the durable,
// de-duplicated place for this (unlike the old per-method-name file path, which two tests with
// the same method name could silently overwrite).
//
// Called from BaseTest's @AfterMethod rather than wired up as an ITestListener: confirmed directly
// (not guessed) that AllureTestNg finalizes and writes out its test-case result right after the
// @Test method returns - before any ITestListener.onTestFailure callback runs - so by the time an
// ITestListener sees the failure, Allure has no active test case left to attach to and
// Allure.addAttachment() silently writes an orphaned attachment file that never shows up in the
// report. @AfterMethod configuration methods still run inside that test case's still-open Allure
// context, so calling this from there is the reliable spot - and it must run before BaseTest quits
// the driver, since the screenshot needs a live session.
public class ScreenshotListener {

    private ScreenshotListener() {}

    public static void attachOnFailure(WebDriver driver, ITestResult result){
        // Called from BaseTest.run() right after IHookCallBack#runTestMethod returns - at that
        // point TestNG has already recorded the throwable but hasn't finalized result.getStatus()
        // to FAILURE yet (confirmed directly: it still reads STARTED there), so the throwable is
        // the only reliable signal available this early.
        if (result.getThrowable() == null || driver == null) {
            return;
        }
        byte[] screenshot = ((TakesScreenshot) driver).getScreenshotAs(OutputType.BYTES);
        String name = result.getTestClass().getName() + "." + result.getMethod().getMethodName();
        Allure.addAttachment(name, new ByteArrayInputStream(screenshot));
    }
}

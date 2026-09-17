package base;

import listeners.ScreenshotListener;
import org.openqa.selenium.WebDriver;
import org.testng.IHookCallBack;
import org.testng.IHookable;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;

public class BaseTest implements IHookable {

    // ! for run methods paralle
    protected ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public void setDriver(WebDriver driver) {
        this.driver.set(driver);
    }

    public WebDriver getDriver() {
        return driver.get();
    }

    // Confirmed directly (not guessed): AllureTestNg closes out its Allure test-case context the
    // moment the @Test method itself returns - before any ITestListener.onTestFailure callback or
    // @AfterMethod runs. By then Allure.addAttachment() has nothing open to attach to and silently
    // writes an orphaned attachment file that never shows up in the report. IHookable.run() wraps
    // the test method invocation itself, so the screenshot can be taken right after
    // runTestMethod() returns but still before TestNG hands control back to AllureTestNg's own
    // afterInvocation - the only point where the attachment reliably lands.
    @Override
    public void run(IHookCallBack callBack, ITestResult testResult) {
        callBack.runTestMethod(testResult);
        ScreenshotListener.attachOnFailure(getDriver(), testResult);
    }

    @AfterMethod(alwaysRun = true)
    public void tearDown() {
        if (getDriver() != null) {
//            getDriver().quit();
//            driver.remove();
        }
    }
}

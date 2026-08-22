package base;

import io.qameta.allure.Allure;
import org.aspectj.util.FileUtil;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.testng.ITestResult;
import org.testng.annotations.AfterMethod;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class BaseTest {

    // ! for run methods paralle
    protected ThreadLocal<WebDriver> driver = new ThreadLocal<>();

    public void setDriver(WebDriver driver) {
        this.driver.set(driver);
    }

    public WebDriver getDriver() {
        return driver.get();
    }

    @AfterMethod
    public void tearDown(ITestResult testResult) throws InterruptedException {
    Thread.sleep(3000);
    String testName = testResult.getName();
    File filePath = new File("target" + File.separator + "Screenshots"+ File.separator + testName  + ".png");
      takeScreenShot(filePath);
           // driver.quit();
        }

        public void takeScreenShot(File FilePath) {
        File file = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.FILE);
            try {
                FileUtil.copyFile(file, FilePath);
                InputStream stream = new FileInputStream(file);
                Allure.addAttachment("Screenshot", stream);
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
}

package factory;

import io.qameta.allure.Step;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.safari.SafariDriver;

import java.time.Duration;

public class DriverFactory {
    @Step("launch {driverName} browser")
    public static WebDriver generateDriver(String driverName) {
        WebDriver driver = null;

        switch (driverName) {
            case "chrome": driver = new ChromeDriver();

                break;
            case "firefox":  driver = new FirefoxDriver();

                break;
            case "safari": driver = new SafariDriver();

                break;
            case "edge": driver = new EdgeDriver();

                break;
            default:
                throw new IllegalArgumentException("Invalid Browser Name");
        }
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ZERO);
        return driver;
    }
}

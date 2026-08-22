package VMS;

import base.BaseTest;
import config.TestConfig;
import factory.DriverFactory;
import io.qameta.allure.Story;
import org.testng.annotations.Test;
import pages.LoginPage;


public class LoginTest extends BaseTest {

    @Story("Login")
    @Test(description = "Success Login")
    public void testLogin() {
        setDriver(DriverFactory.generateDriver(TestConfig.BROWSER));
        getDriver().get(TestConfig.AUTH_URL);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(TestConfig.USERNAME, TestConfig.PASSWORD);
    }
}
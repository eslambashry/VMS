package VMS.auth;

import base.BaseTest;
import config.TestConfig;
import factory.DriverFactory;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.testng.Assert;
import org.testng.annotations.Test;
import pages.LoginPage;


@Epic("Authentication")
@Feature("Login")
public class SuccessLoginTest extends BaseTest {

    @Story("Login")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Success Login", groups = {"smoke", "regression"})
    public void testLogin() {
        setDriver(DriverFactory.generateDriver(TestConfig.BROWSER));
        getDriver().get(TestConfig.AUTH_URL);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);

        Assert.assertTrue(loginPage.isLoggedIn(), "User should be logged in and see the Visit Requests tab");
    }
}
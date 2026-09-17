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

// AUTH-02: logging in with an invalid password should show the user an error, not just silently
// reload the page. Confirmed directly (not guessed) that the backend already rejects bad
// credentials correctly ({"success": false, "message": "Incorrect username or password", ...}) -
// this asserts that message actually reaches the user, since the frontend was observed not
// displaying it at all (a real product regression, not a test-code issue).
@Epic("Authentication")
@Feature("Login")
public class InvalidLoginTest extends BaseTest {

    @Story("Login")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Invalid Password Shows An Error And Does Not Log In", groups = {"negative", "regression"})
    public void invalidPasswordShowsError() {
        setDriver(DriverFactory.generateDriver(TestConfig.BROWSER));
        getDriver().get(TestConfig.AUTH_URL);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.attemptLogin(TestConfig.REGULAR_USERNAME, "WrongPassword123!");

        Assert.assertTrue(loginPage.isErrorMessageDisplayed("Incorrect username or password"),
                "An error message should be shown to the user after an invalid login attempt");
        Assert.assertFalse(loginPage.isLoggedIn(),
                "User should not be logged in with an incorrect password");
    }
}

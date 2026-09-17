package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;

import java.time.Duration;

public class LoginPage {
    private WebDriver driver;
    private WebDriverWait wait;
    // Deliberately short, separate from the page-level 90s wait: that longer wait exists to
    // tolerate a slow post-login redirect on success, but reusing it for "did an error message
    // appear" meant a genuinely-missing error (the real AUTH-02 regression) took the full 90s to
    // time out instead of failing fast - confirmed directly as the actual cause of the test being
    // slow, not the driver being slow to quit afterward.
    private final WebDriverWait shortWait;

    public LoginPage(WebDriver driver) {
        this.driver = driver;

        wait = new WebDriverWait(driver, Duration.ofSeconds(90));
        shortWait = new WebDriverWait(driver, Duration.ofSeconds(15));

        PageFactory.initElements(driver, this);
    }

    @FindBy(css = "input[name='username']")
    private WebElement usernameInput;

    @FindBy(css = "input[name='password']")
    private WebElement passwordInput;

    @FindBy(css = "button.submit-button")
    private WebElement submitButton;

    @FindBy(xpath = "//a[contains(@class,'tab-link')][.//h2[normalize-space()='Visit Requests']]")
    private WebElement visitsButton;

    public void login(String username, String password) {
        attemptLogin(username, password);
        // Polls isLoggedIn() rather than referencing visitsButton directly a second time - one
        // single place defines "what counts as logged in" instead of two.
        wait.until(driver -> isLoggedIn());
    }

    // For negative-login cases (AUTH-02), where waiting for the post-login "Visit Requests" tab
    // would just time out - submits credentials without assuming they succeed.
    public void attemptLogin(String username, String password) {
        wait.until(ExpectedConditions.visibilityOf(usernameInput));
        enterUsername(username);
        enterPassword(password);
        clickSubmitButton();
    }

    public boolean isLoggedIn(){
        // Only ever called after a successful login before now, where visitsButton always exists -
        // guarded here too since a failed login (see attemptLogin) leaves it genuinely absent from
        // the DOM, not just hidden, which throws NoSuchElementException rather than returning false.
        try {
            return visitsButton.isDisplayed();
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    // AUTH-02: matches on contains(), not exact text, since the frontend may wrap the backend's
    // raw message (e.g. "Incorrect username or password") with its own surrounding text/styling.
    public boolean isErrorMessageDisplayed(String errorTextFragment){
        try {
            return shortWait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//*[contains(normalize-space(text()), '" + errorTextFragment + "')]"))) != null;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // Right after submitting, a layout shift can briefly put the form-footer over this button,
    // intercepting the click even though it just reported clickable - retry through that race.
    private void clickSubmitButton() {
        int attempts = 0;
        while (true) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
                return;
            } catch (ElementClickInterceptedException e) {
                attempts++;
                if (attempts >= 3) {
                    throw e;
                }
            }
        }
    }

    @Step("enter email")
    private void enterUsername(String username) {
        usernameInput.sendKeys(username);
    }

    @Step("enter password")
    private void enterPassword(String password) {
        passwordInput.sendKeys(password);
    }

}

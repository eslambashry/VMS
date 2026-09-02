package pages;

import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
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

    public LoginPage(WebDriver driver) {
        this.driver = driver;

        wait = new WebDriverWait(driver, Duration.ofSeconds(90));

        PageFactory.initElements(driver, this);
    }

    @FindBy(css = "input[name='username']")
    private WebElement usernameInput;

    @FindBy(css = "input[name='password']")
    private WebElement passwordInput;

    @FindBy(css = "button.submit-button")
    private WebElement submitButton;

    @FindBy(css = "custom-svg-icon.collapsed-svg-icon")
    private WebElement visitsButton;

    public void login(String username, String password) {
        wait.until(ExpectedConditions.visibilityOf(usernameInput));
        enterUsername(username);
        enterPassword(password);
        clickSubmitButton();

        wait.until(ExpectedConditions.visibilityOf(visitsButton));
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

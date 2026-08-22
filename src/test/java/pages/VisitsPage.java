package pages;

import api.WorkingHoursParser;
import base.BasePage;
import components.TimePickerComponent;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class VisitsPage extends BasePage {

    public VisitsPage(WebDriver driver) {
        super(driver);
    }

    @FindBy(className = "custom-calendar-input")
    private WebElement calendarInput;

    @FindBy(id = "startTime")
    private WebElement startTimeInput;

    @FindBy(id = "endTime")
    private WebElement endTimeInput;

    // "Visit Type" is a hidden input (visitTypeId) backing a custom ng-select dropdown;
    // it can't be filled with sendKeys - the visible dropdown trigger/option needs its own locator.
    @FindBy(name = "visitTypeId")
    private WebElement visitTypeHiddenInput;

    @FindBy(id = "Purpose of visit")
    private WebElement purposeOfVisitInput;

    // "Visit Room" is a hidden input (meetingRoomId) backing a custom ng-select dropdown.
    @FindBy(name = "meetingRoomId")
    private WebElement visitRoomHiddenInput;

    // "Visit Zone" is a hidden input (visitZoneId) backing a custom ng-select dropdown.
    @FindBy(name = "visitZoneId")
    private WebElement visitZoneHiddenInput;

    // "Host Employee" is a hidden input (hostEmployeeId) backing a custom ng-select dropdown / "Me" toggle.
    @FindBy(name = "hostEmployeeId")
    private WebElement hostEmployeeHiddenInput;

    @FindBy(name = "notesToHsse")
    private WebElement notesToHsseTextarea;

    @FindBy(xpath = "//button[contains(@class,'stroke-red') and normalize-space(text())='Cancel']")
    private WebElement cancelButton;

    @FindBy(xpath = "//button[contains(@class,'ng-star-inserted') and normalize-space(text())='Next']")
    private WebElement nextButton;

    private TimePickerComponent startTimePicker(){
        return new TimePickerComponent(driver, wait, startTimeInput);
    }

    private TimePickerComponent endTimePicker(){
        return new TimePickerComponent(driver, wait, endTimeInput);
    }

    private static final By MONTH_TITLE = By.className("month-title");
    private static final By NEXT_MONTH_BUTTON =
            By.xpath("//div[contains(@class,'month-title')]/following-sibling::button[contains(@class,'nav-button')][1]");
    private static final By PREV_MONTH_BUTTON =
            By.xpath("//div[contains(@class,'month-title')]/preceding-sibling::button[contains(@class,'nav-button')][1]");
    private static final DateTimeFormatter MONTH_TITLE_FORMAT = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.ENGLISH);

    // Returns the date it actually picked, so the caller can look up that day's working hours
    // (not every day is a working day - e.g. Saturday in this system - so "tomorrow" isn't always valid).
    // workingHours is fetched directly from the API beforehand (see api.VisitMngtApiClient).
    public LocalDate selectVisitDay(List<WorkingHoursParser.WorkingHours> workingHours){
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
        calendarInput.click();
        LocalDate visitDay = WorkingHoursParser.nextWorkingDay(workingHours, LocalDate.now().plusDays(1));

        navigateToMonth(YearMonth.from(visitDay));

        // Leading/trailing days from adjacent months are rendered too (grayed out) and can share the
        // same day number as the target date (e.g. day "2" appears both in the current month and as
        // next month's overflow) - "current-month" disambiguates real, clickable days from those.
        String dayXpath = "//div[contains(@class,'day') and contains(@class,'current-month') and normalize-space(text())='"
                + visitDay.getDayOfMonth() + "']";
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(dayXpath))).click();
        return visitDay;
    }

    // The calendar only ever renders one month at a time, defaulting to the current one - so if the
    // target date falls in a different month, the "next"/"prev" nav buttons have to be clicked until
    // the header matches before the day cell can even exist to click.
    private void navigateToMonth(YearMonth target){
        for (int i = 0; i < 24; i++) {
            String titleText = wait.until(ExpectedConditions.visibilityOfElementLocated(MONTH_TITLE)).getText().trim();
            YearMonth displayed = YearMonth.parse(titleText, MONTH_TITLE_FORMAT);
            if (displayed.equals(target)) {
                return;
            }
            By navButton = displayed.isBefore(target) ? NEXT_MONTH_BUTTON : PREV_MONTH_BUTTON;
            wait.until(ExpectedConditions.elementToBeClickable(navButton)).click();
        }
        throw new IllegalStateException("Could not navigate calendar to " + target);
    }

    public void selectVisitType(String visitType){
        selectDropdownOption("visitTypeId", visitType);
    }

    public void selectVisitZone(String visitZone){
        selectDropdownOption("visitZoneId", visitZone);
    }

    // Shared by all the custom ng-select dropdowns on this page (Visit Type, Visit Room, Visit Zone,
    // Host Employee) - they all render a hidden input for form binding plus the same
    // dropdown-header/dropdown-option markup, differing only in the hidden input's name.
    private void selectDropdownOption(String hiddenInputName, String optionText){
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
        By header = By.xpath("//input[@name='" + hiddenInputName + "']/parent::*//div[contains(@class,'dropdown-header')]");
        wait.until(ExpectedConditions.elementToBeClickable(header)).click();
        String optionXpath = "//span[contains(@class,'option-text') and normalize-space(text())='" + optionText + "']";
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(optionXpath))).click();
    }

    public void selectStartTime(String time){
        startTimePicker().selectTime(time);
    }

    public void selectEndTime(String time){
        endTimePicker().selectTime(time);
    }

    @Step("generate a random name")
    public void enterPurposeOfVisit(String purpose){
        purposeOfVisitInput.sendKeys(purpose);
    }

    public void enterNotesToHsse(String notes){
        notesToHsseTextarea.sendKeys(notes);
    }

    public void clickNext(){
        nextButton.click();
    }

    public void clickCancel(){
        cancelButton.click();
    }
}

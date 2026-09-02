package pages;

import api.WorkingHoursParser;
import base.BasePage;
import components.TimePickerComponent;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
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



    // Visitors Info step: clicking the "englishFirstName" field's search-icon-button (with no
    // typing needed) queries the visitors API and renders matches as "dropdown-option" rows;
    // selecting one auto-fills the rest of the visitor's fields (Arabic name, phone, email,
    // company, ID, photos).
    private static final By VISITOR_FIRST_NAME_SEARCH_BUTTON = By.xpath(
            "//custom-input-search-form[@controlname='englishFirstName']//button[contains(@class,'search-icon-button')]");

    @FindBy(xpath = "//button[contains(@class,'stroke-red') and normalize-space(text())='Cancel']")
    private WebElement cancelButton;

    // "Next" is rendered inside a nested element (e.g. a div/p), not as the button's own direct
    // text node, so matching on the button's text() directly never finds it - match any descendant instead.
    @FindBy(xpath = "//button[.//*[normalize-space(text())='Next']]")
    private WebElement nextButton;

    @FindBy(xpath = "//button[.//*[normalize-space(text())='Submit']]")
    private WebElement submitButton;

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
        clickCalendarInput();
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

    // Even once clickable, the calendar's own data-loading spinner can flash over it right at click
    // time, intercepting the click - retry through that rather than failing on the race.
    private void clickCalendarInput() {
        int attempts = 0;
        while (true) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(calendarInput)).click();
                return;
            } catch (ElementClickInterceptedException e) {
                attempts++;
                if (attempts >= 3) {
                    throw e;
                }
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
            }
        }
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


    // Same click-intercepted race as the calendar input - the loader can flash over the button
    // right at click time even after it's reported clickable.
    public void clickNextButton() {
                wait.until(ExpectedConditions.elementToBeClickable(nextButton)).click();
    }

    public void clickSubmitButton() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
    }

    public void clickCancel(){
        cancelButton.click();
    }

    // Clicking the search-icon-button with the field left empty is enough to load its default
    // (unfiltered) results - no typing needed. nameEn should come from VisitorParser.
    // firstCompleteVisitorNameEn, so it's guaranteed to match a visitor with full data rather than
    // whichever entry happens to be listed first.
    public void searchAndSelectVisitor(String nameEn){
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
        wait.until(ExpectedConditions.elementToBeClickable(VISITOR_FIRST_NAME_SEARCH_BUTTON)).click();

        By option = By.xpath("//div[@tabindex='0'][contains(concat(' ',normalize-space(@class),' '),' dropdown-option ')]"
                + "[.//p[contains(@class,'visitor-name') and normalize-space(text())='" + nameEn + "']]");
        wait.until(ExpectedConditions.elementToBeClickable(option)).click();
    }
}

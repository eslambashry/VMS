package pages;

import api.WorkingHoursParser;
import base.BasePage;
import components.TimePickerComponent;
import io.qameta.allure.Step;
import org.openqa.selenium.By;
import org.openqa.selenium.ElementClickInterceptedException;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.Keys;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
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


    // visitor data
    @FindBy(css = "input[name='englishFirstName']")
    private WebElement visitorFirstNameEnglish;

    @FindBy(css = "input[name='englishLastName']")
    private WebElement visitorLastNameEnglish;

    @FindBy(css = "input[name='arabicFirstName']")
    private WebElement visitorFirstNameArabic;

    @FindBy(css = "input[name='arabicLastName']")
    private WebElement visitorLastNameArabic;

    @FindBy(css = "input[type='tel'].phone-input")
    private WebElement visitorPhoneNumber;

    @FindBy(css = "input[name='email']")
    private WebElement visitorEmail;

    @FindBy(css = "input[name='company']")
    private WebElement visitorCompanyName;

    @FindBy(css = "input[name='nationalId']")
    private WebElement visitorPassportOrNationalId;

    // Both "Visitor Photo" and "National ID/Passport Photo" render the same hidden input markup,
    // differing only in position on the page - so they're located as a list and indexed by the
    // visual order (0 = Visitor Photo, 1 = National ID/Passport Photo) rather than each having its
    // own selector.
    @FindBy(css = "input[type='file'].file-input-hidden")
    private List<WebElement> fileUploadInputs;

    // Vehicle Plate Number is two separate fields, not one - letters (5 chars) and digits (7
    // chars), matching the Saudi plate format (confirmed by inspecting the real form).
    @FindBy(css = "input[name='vehiclePlate-letters']")
    private WebElement vehiclePlateLettersInput;

    @FindBy(css = "input[name='vehiclePlate-digits']")
    private WebElement vehiclePlateDigitsInput;



    // Visitors Info step: clicking the "englishFirstName" field's search-icon-button (with no
    // typing needed) queries the visitors API and renders matches as "dropdown-option" rows;
    // selecting one auto-fills the rest of the visitor's fields (Arabic name, phone, email,
    // company, ID, photos).
    private static final By VISITOR_FIRST_NAME_SEARCH_BUTTON = By.xpath(
            "//custom-input-search-form[@controlname='englishFirstName']//button[contains(@class,'search-icon-button')]");

    // "Add Visitor" renders another visitor card whose fields - including englishFirstName - reuse
    // the exact same controlname as every other card (confirmed by inspecting the DOM after adding
    // a second visitor: controlname is not unique per slot). So once more than one visitor card
    // exists, VISITOR_FIRST_NAME_SEARCH_BUTTON's xpath matches multiple identical elements; only
    // document position (1st card, 2nd card, ...) tells them apart.
    private static final By ADD_VISITOR_BUTTON = By.xpath("//button[contains(@class,'add-visitor-btn')]");

    @FindBy(xpath = "//button[contains(@class,'stroke-red') and normalize-space(text())='Cancel']")
    private WebElement cancelButton;

    // "Next" is rendered inside a nested element (e.g. a div/p), not as the button's own direct
    // text node, so matching on the button's text() directly never finds it - match any descendant instead.
    @FindBy(xpath = "//button[.//*[normalize-space(text())='Next']]")
    private WebElement nextButton;

    @FindBy(xpath = "//button[.//*[normalize-space(text())='Submit']]")
    private WebElement submitButton;

    @FindBy(xpath = "//button[.//*[normalize-space(text())='Previous']]")
    private WebElement previousButton;

    // Confirmed directly: this toast can appear right after clicking Next off the vehicle plate
    // fields even when the letters are valid - the exact same value that was just rejected passes
    // cleanly seconds later with nothing changed, so it's a transient backend hiccup in the async
    // plate check, not a real validation failure. Going back a step and forward again re-runs the
    // check and reliably clears it.
    private static final By INVALID_PLATE_TOAST = By.xpath(
            "//*[contains(normalize-space(text()), 'Invalid plate letter')]");

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

// Shared first step for opening the visit-date calendar popup - used both when picking a date
    // and when checking whether a specific date is disabled (see isDayDisabled).
    public void openCalendar(){
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
        clickCalendarInput();
    }

    // Returns the date it actually picked, so the caller can look up that day's working hours
    // (not every day is a working day - e.g. Saturday in this system - so "tomorrow" isn't always valid).
    // workingHours is fetched directly from the API beforehand (see api.VisitMngtApiClient).
    public LocalDate selectVisitDay(List<WorkingHoursParser.WorkingHours> workingHours){
        openCalendar();
        LocalDate visitDay = WorkingHoursParser.nextWorkingDay(workingHours, LocalDate.now().plusDays(1));

        navigateToMonth(YearMonth.from(visitDay));
        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(dayCellXpath(visitDay.getDayOfMonth())))).click();
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


    // Leading/trailing days from adjacent months are rendered too (grayed out) and can share the
    // same day number as the target date (e.g. day "2" appears both in the current month and as
    // next month's overflow) - "current-month" disambiguates real, clickable days from those.
    private String dayCellXpath(int dayOfMonth){
        return "//div[contains(@class,'day') and contains(@class,'current-month') and normalize-space(text())='"
                + dayOfMonth + "']";
    }

    // Confirmed directly (not guessed): the calendar marks a day disabled - the same "day disabled"
    // class it uses for past, unselectable dates - whenever the working-hours API marks that
    // day-of-week as isWorkingDay=false (e.g. Saturday in this system). Assumes the calendar popup
    // is already open (see openCalendar) and navigates to the given date's month before reading it.
    public boolean isDayDisabled(LocalDate date){
        navigateToMonth(YearMonth.from(date));
        WebElement dayCell = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(dayCellXpath(date.getDayOfMonth()))));
        return dayCell.getAttribute("class").contains("disabled");
    }

    public void selectVisitType(String visitType){
        selectDropdownOption("visitTypeId", visitType);
    }

    public void selectVisitZone(String visitZone){
        selectDropdownOption("visitZoneId", visitZone);
    }

    // "Host Employee" defaults to "Me" (toggle active, dropdown disabled - confirmed by inspecting
    // the real form). Switching it off is required before the Host Employee dropdown becomes
    // interactive at all.
    private static final By HOST_ME_TOGGLE =
            By.xpath("//custom-toggle-switch-form[@controlname='isHostMe']//div[contains(@class,'toggle-switch')]");
    private static final By HOST_EMPLOYEE_DROPDOWN_CONTAINER =
            By.xpath("//input[@name='hostEmployeeId']/parent::*//div[contains(@class,'dropdown-container')]");

    public void switchToAnotherHost(){
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
        wait.until(ExpectedConditions.elementToBeClickable(HOST_ME_TOGGLE)).click();
        // The dropdown is a plain div (not a native form control), so Selenium's elementToBeClickable
        // wouldn't reflect its "disabled" class - wait for that class to actually clear instead of
        // racing the click against Angular's re-render.
        wait.until(driver -> !driver.findElement(HOST_EMPLOYEE_DROPDOWN_CONTAINER).getAttribute("class").contains("disabled"));
    }

    public void selectHostEmployee(String employeeName){
        selectDropdownOption("hostEmployeeId", employeeName);
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

    // Confirmed directly (network capture, no CDP mismatch issues): clicking Next off this step
    // fires no new HTTP request at all - this toast is a purely client-side (Angular) validator,
    // not a flaky backend call. A plain Previous -> Next cycle with the plate fields left untouched
    // does NOT clear it either (same letters, same result). What the same value reliably passes
    // with (confirmed manually) is clearing the field with select-all+delete and retyping it one
    // character at a time - see typeSlowly. The plate fields only exist on the Visitor Info step,
    // not on this review step, so Previous is required before they're reachable again.
    public void clickNextRetryingTransientPlateValidationError(String plateLetters, String plateDigits) {
        clickNextButton();
        int attempts = 0;
        while (isPlateValidationToastShowing() && attempts < 3) {
            attempts++;
            wait.until(ExpectedConditions.elementToBeClickable(previousButton)).click();
            clearField(vehiclePlateLettersInput);
            typeSlowly(vehiclePlateLettersInput, plateLetters);
            clearField(vehiclePlateDigitsInput);
            typeSlowly(vehiclePlateDigitsInput, plateDigits);
            clickNextButton();
        }
    }

    // Purely client-side, so the toast (if it's going to appear at all) is already in the DOM
    // right after the click - no network round trip to wait out. Still bounded rather than a bare
    // findElements() check: Angular's own change detection can take a beat to render it.
    private boolean isPlateValidationToastShowing(){
        try {
            new WebDriverWait(driver, Duration.ofMillis(800)).until(ExpectedConditions.visibilityOfElementLocated(INVALID_PLATE_TOAST));
            return true;
        } catch (TimeoutException e) {
            return false;
        }
    }


    // Same rejection toast as isPlateValidationToastShowing, but for the final Submit step rather
    // than the Next-button click off Visitor Info: unlike that fast, at-most-client-side check,
    // Submit involves a real request round trip, so this waits (up to the shared timeout) for the
    // toast to appear instead of doing a fixed, short poll that could miss it.
    public boolean waitForPlateValidationToast(){
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(INVALID_PLATE_TOAST)) != null;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // Selenium's .clear() sets the DOM value directly rather than going through real key events -
    // for this field's input mask (reformats the display, e.g. "P V Z", on every keystroke) that
    // can leave it out of sync with the mask directive's own state. Select-all + Delete goes
    // through the same key-event path as typing, so the mask directive sees (and recovers from) it
    // the same way it would a real user clearing the field.
    private void clearField(WebElement input){
        input.sendKeys(Keys.chord(Keys.CONTROL, "a"));
        input.sendKeys(Keys.DELETE);
    }

    // The plate fields' input mask reformats the whole displayed value and repositions the cursor
    // on every keystroke - confirmed directly: sendKeys() firing characters back-to-back can outrun
    // that reformatting and leave the mask directive's underlying (unmasked) value out of sync with
    // what's visibly displayed, which is what the "Invalid plate letter" validator actually checks.
    // A small delay between characters keeps it in sync, closer to how a human types.
    private void typeSlowly(WebElement input, String text){
        for (char c : text.toCharArray()) {
            input.sendKeys(String.valueOf(c));
            try {
                Thread.sleep(120);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    // Confirmed directly: leaving a required Visit Info field empty disables "Next" (via the
    // native disabled attribute, not just a visual style) and shows an inline error under that
    // field - rather than allowing Next to be clicked and failing later.
    //
    // Waits for the disabled state rather than checking it once, same as
    // isAddVisitorButtonDisabled: every current caller happens to check this when the button has
    // already been disabled since page load (no transition to race), but an instantaneous check
    // would silently reintroduce that exact race the moment this gets reused somewhere the state
    // actually flips right before the check - not worth leaving as a latent trap.
    public boolean isNextButtonDisabled(){
        try {
            return wait.until(driver -> !nextButton.isEnabled());
        } catch (TimeoutException e) {
            return false;
        }
    }

    // Was a single instantaneous driver.findElements() check with no wait - confirmed directly
    // that this loses a real timing race under concurrent load (3 threads): Angular's validation
    // render can lag just enough that the text genuinely isn't in the DOM yet at the instant of
    // an unguarded check, even though the field really is invalid. Same wait-then-give-up shape
    // as isVisitCreatedSuccessfully, rather than assuming the error is already there the moment
    // this is called.
    public boolean isValidationErrorDisplayed(String errorText){
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(
                    By.xpath("//*[normalize-space(text())='" + errorText + "']"))) != null;
        } catch (TimeoutException e) {
            return false;
        }
    }

    // Angular's reactive-form validation only renders a field's error message once it's been
    // "touched" (focused then blurred) - confirmed directly: leaving a field empty without ever
    // interacting with it disables Next correctly, but shows no error text until it's been
    // focused and blurred at least once.
    public void touchPurposeField(){
        purposeOfVisitInput.click();
        purposeOfVisitInput.sendKeys(Keys.TAB);
    }

    public void touchVisitTypeField(){
        touchDropdown("visitTypeId");
    }

    public void touchVisitZoneField(){
        touchDropdown("visitZoneId");
    }

    // Opens the dropdown (focus) then closes it without selecting an option - "touching" the
    // field for Angular's validation purposes, adapted for the custom dropdown widgets.
    //
    // Closes via an outside click, not a key press: confirmed directly that neither
    // WebElement.sendKeys(Keys.ESCAPE) on the header div (throws ElementNotInteractableException -
    // a plain div isn't a native text-entry element) nor Actions-dispatched Escape (leaves the
    // panel visibly open, fully loaded with options, every time) closes this widget. These custom
    // dropdowns apparently only close on an outside click, not on Escape, so one is dispatched
    // directly via JS rather than relying on keyboard support the widget doesn't implement.
    private void touchDropdown(String hiddenInputName){
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
        By header = By.xpath("//input[@name='" + hiddenInputName + "']/parent::*//div[contains(@class,'dropdown-header')]");
        wait.until(ExpectedConditions.elementToBeClickable(header)).click();
        // Some dropdowns (e.g. Visit Zone) populate their options asynchronously after opening -
        // confirmed directly: closing immediately after the click intermittently raced that,
        // leaving the panel stuck open. Waiting for an option to actually render first closes
        // that gap.
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.className("dropdown-option")));
        ((JavascriptExecutor) driver).executeScript("document.body.click();");
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

        selectVisitorFromDropdown(nameEn);
    }

    public void clickAddVisitor(){
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
        wait.until(ExpectedConditions.elementToBeClickable(ADD_VISITOR_BUTTON)).click();
    }

    // VAL-05: confirmed directly - once maxVisitorsPerVisit is reached, "Add Visitor" gets a
    // native disabled attribute (not just a visual style), so there's no 11th-visitor error to
    // wait for; the button itself refuses further clicks.
    //
    // Waits for the disabled state rather than checking it once: same mistake as
    // isValidationErrorDisplayed originally made - confirmed directly that an instantaneous check
    // right after the last visitor is selected can still see the button briefly enabled, before
    // Angular's own re-render actually applies the disabled attribute.
    public boolean isAddVisitorButtonDisabled(){
        try {
            return wait.until(driver -> !driver.findElement(ADD_VISITOR_BUTTON).isEnabled());
        } catch (TimeoutException e) {
            return false;
        }
    }

    // For the 2nd, 3rd, ... visitor card, added via clickAddVisitor(). visitorPosition is 1-based,
    // matching the "Visitor N" label shown on each card.
    //
    // Not a plain controlname match, and not a raw positional index into every search-icon-button
    // on the page either: once a visitor is selected, that card's englishFirstName field swaps its
    // search-icon-button for a different (clear/reset) button - confirmed by inspecting a filled
    // card's DOM, where the first-name field showed a clear icon while the still-untouched
    // last-name field next to it still showed the search icon. So the count of actual
    // search-icon-buttons on the page shrinks as visitors get filled in, making a global position
    // index unreliable (it happened to still work by luck at position 2, then broke at position 3
    // once earlier cards' buttons had already been swapped out).
    //
    // Scoping to the specific card by its "Visitor N" header instead sidesteps that entirely - each
    // card (custom-expanding-container) keeps its header regardless of fill state, so this reliably
    // finds "card N's own search button", not "the Nth search button remaining on the whole page".
    public void searchAndSelectVisitor(String nameEn, int visitorPosition){
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
        By searchButton = By.xpath(
                "//custom-expanding-container[.//h3[contains(@class,'visitor-name')][normalize-space(.)='Visitor "
                        + visitorPosition + "']]"
                        + "//custom-input-search-form[@controlname='englishFirstName']"
                        + "//button[contains(@class,'search-icon-button')]");
        wait.until(ExpectedConditions.elementToBeClickable(searchButton)).click();

        selectVisitorFromDropdown(nameEn);
    }

    // Same click-intercepted race as the calendar input and the login submit button - the
    // results list's own "loading-text" indicator can still be settling into place right as the
    // option becomes clickable, briefly overlapping it (confirmed directly: "Element <div
    // class="dropdown-option">...</div> is not clickable... Other element would receive the
    // click: <p class="loading-text">...</p>").
    private void selectVisitorFromDropdown(String nameEn){
        By option = By.xpath("//div[@tabindex='0'][contains(concat(' ',normalize-space(@class),' '),' dropdown-option ')]"
                + "[.//p[contains(@class,'visitor-name') and normalize-space(text())='" + nameEn + "']]");
        int attempts = 0;
        while (true) {
            try {
                wait.until(ExpectedConditions.elementToBeClickable(option)).click();
                return;
            } catch (ElementClickInterceptedException e) {
                attempts++;
                if (attempts >= 3) {
                    throw e;
                }
            }
        }
    }


    // Start Visitor Details
    public void enterVisitorFirstName(String firstName){
        visitorFirstNameEnglish.sendKeys(firstName);
    }

    // Same "touched" mechanism as touchPurposeField - the Visitors Info step's default empty
    // Visitor 1 card only shows each field's error once it's been focused and blurred, confirmed
    // directly to work the same way as the Visit Info step.
    public void touchVisitorFirstNameField(){
        visitorFirstNameEnglish.click();
        visitorFirstNameEnglish.sendKeys(Keys.TAB);
    }

    public void enterVisitorLastName(String lastName){
        visitorLastNameEnglish.sendKeys(lastName);
    }

    public void enterVisitorFirstNameArabic(String name){
        visitorFirstNameArabic.sendKeys(name);
    }

    public void enterVisitorLastNameArabic(String name){
        visitorLastNameArabic.sendKeys(name);
    }

    public void enterVisitorPhoneNumber(String phoneNumber){
        visitorPhoneNumber.sendKeys(phoneNumber);
    }

    public void enterVisitorEmail(String email){
        visitorEmail.sendKeys(email);
    }

    public void enterVisitorCompanyName(String companyName){
        visitorCompanyName.sendKeys(companyName);
    }

    public void enterVisitorNationalId(String visitorNationalId){
        visitorPassportOrNationalId.sendKeys(visitorNationalId);
    }

    public void uploadVisitorPhoto(String imagePath) {
        fileUploadInputs.get(0).sendKeys(imagePath);
    }

    public void uploadNationalIdPhoto(String imagePath) {
        fileUploadInputs.get(1).sendKeys(imagePath);
    }

    // The Vehicle section (model dropdown + plate number) only renders once this toggle is on -
    // confirmed directly (nothing about "vehicle" exists in the DOM beforehand). There's no
    // separate "Has Vehicle" control; this toggle is the only thing that reveals it.
    private static final By PARKING_REQUIRED_TOGGLE =
            By.xpath("//custom-toggle-switch-form[@controlname='parkingRequired']//div[contains(@class,'toggle-switch')]");

    public void toggleParkingRequired(){
        wait.until(ExpectedConditions.elementToBeClickable(PARKING_REQUIRED_TOGGLE)).click();
    }

    // Same custom ng-select dropdown markup as Visit Type/Zone/Host Employee - see
    // selectDropdownOption. Options are a fixed, small set (Car/Van/Truck, confirmed directly),
    // not an admin-configurable lookup, so callers can pass one of those literally.
    @Step("Select Car Type")
    public void selectVehicleModel(String model){
        selectDropdownOption("vehicleModel", model);
    }

    @Step("Enter vehicle plate letter")
    public void enterVehiclePlateLetters(String letters){
        vehiclePlateLettersInput.sendKeys(letters);
    }


    // Reads back what the input mask actually accepted, rather than what was sent - confirmed
    // directly that some letters (e.g. 'E') are silently dropped keystroke-by-keystroke by the
    // mask, leaving the field empty even though every character was sent via enterVehiclePlateLetters.
    public String getVehiclePlateLettersValue(){
        return vehiclePlateLettersInput.getAttribute("value");
    }

    @Step("Enter vehicle plate numbers")
    public void enterVehiclePlateDigits(String digits){
        vehiclePlateDigitsInput.sendKeys(digits);
    }

    // End Visitor Details

    public void clickSubmitButton() {
        wait.until(ExpectedConditions.elementToBeClickable(submitButton)).click();
    }

    // Confirmed by driving a real submission and inspecting the resulting DOM: on success the app
    // shows a confirmation modal titled "Submit Successfully" and redirects to /visits/main. The
    // modal title is the more direct signal of the two (purpose-built confirmation vs. a routing
    // side-effect), so it's used here rather than the URL.
    // The text lives on a nested <p class="main-text"> inside modal-header-title, not as that
    // div's own direct text - matching on the div's text() (rather than this element) never finds it.
    private static final By SUCCESS_MODAL_TITLE =
            By.xpath("//p[contains(@class,'main-text')][normalize-space(text())='Submit Successfully']");

    public boolean isVisitCreatedSuccessfully(){
        try {
            return wait.until(ExpectedConditions.visibilityOfElementLocated(SUCCESS_MODAL_TITLE)) != null;
        } catch (TimeoutException e) {
            return false;
        }
    }
}

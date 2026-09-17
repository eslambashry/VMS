package VMS.visits.regular;

import api.AuthClient;
import api.LookupParser;
import api.VisitMngtApiClient;
import api.WorkingHoursParser;
import base.BaseTest;
import config.TestConfig;
import factory.DriverFactory;
import io.qameta.allure.Allure;
import io.qameta.allure.Epic;
import io.qameta.allure.Feature;
import io.qameta.allure.Severity;
import io.qameta.allure.SeverityLevel;
import io.qameta.allure.Story;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import pages.LoginPage;
import pages.VisitsPage;
import utlis.UserUtils;

import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

// See UserUtils.generateRandomVehiclePlateLetters's own reference comment:
//   ABDGHJKLMNPRSTUVXZ (the Wikipedia-documented Saudi plate letter set)
//   remove M, P; add E
// M and P are confirmed directly to be rejected by the backend at Submit time (a real toast,
// "Invalid plate letter. Only approved Saudi plate letters are allowed.") - the letters do reach
// the input field fine, only the final submission is blocked. E is confirmed directly to be
// rejected far earlier and differently: the field's own input mask silently drops every keystroke,
// so the field never even holds "EEE" - a genuine product defect, since E is supposed to be valid
// (see that same "TODO missing E" note). Each row below is its own Allure test case with its own
// screenshot of the real failure; the letter under test is also printed to the console.
@Epic("Visit Management")
@Feature("Regular Visit")
public class VehiclePlateLetterValidationTest extends BaseTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @DataProvider(name = "plateLetters")
    public Object[][] plateLetters(){
        return new Object[][]{
                {"M", true},
                {"P", true},
                {"E", false},
        };
    }

    @Story("Create Regular Request With Visitor Vehicle")
    @Severity(SeverityLevel.NORMAL)
    @Test(dataProvider = "plateLetters",
            description = "Vehicle Plate Letters Field Handles Each Boundary Letter Correctly",
            groups = {"negative", "regression", "visit", "regular"})
    public void vehiclePlateLetterBehavesAsExpected(String letter, boolean acceptedByInputMask) {
        String plateLetters = letter.repeat(3);
        System.out.println("[Vehicle Plate Letter Test] Testing letter '" + letter + "' (plate letters '"
                + plateLetters + "')");

        String accessToken = AuthClient.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);
        String workingHoursJson = VisitMngtApiClient.getWorkingHours(accessToken);
        List<WorkingHoursParser.WorkingHours> workingHoursList = WorkingHoursParser.parse(workingHoursJson);
        String visitZone = LookupParser.firstActiveNameEn(VisitMngtApiClient.getVisitZones(accessToken));

        setDriver(DriverFactory.generateDriver(TestConfig.BROWSER));
        getDriver().get(TestConfig.AUTH_URL);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);
        getDriver().navigate().to(TestConfig.CREATE_VISIT_URL);

        VisitsPage visitsPage = new VisitsPage(getDriver());
        LocalDate visitDay = visitsPage.selectVisitDay(workingHoursList);

        // Kept to round hours (e.g. 10:00 -> 11:00) instead of a ":30" offset - simpler, valid
        // values for the time picker with no odd minute to land on.
        WorkingHoursParser.WorkingHours workingHours = WorkingHoursParser.forDate(workingHoursList, visitDay);
        LocalTime startTime = workingHours.startTime().getMinute() == 0
                ? workingHours.startTime()
                : workingHours.startTime().plusHours(1).withMinute(0);
        LocalTime endTime = startTime.plusHours(1);
        if (endTime.isAfter(workingHours.endTime())) {
            endTime = workingHours.endTime();
        }

        visitsPage.selectStartTime(startTime.format(TIME_FORMAT));
        visitsPage.selectEndTime(endTime.format(TIME_FORMAT));
        visitsPage.selectVisitType("Maintenance");
        visitsPage.enterPurposeOfVisit(new UserUtils().generateRandomPurpose());
        visitsPage.selectVisitZone(visitZone);

        visitsPage.clickNextButton();

        visitsPage.enterVisitorFirstName(new UserUtils().generateRandomFirstName());
        visitsPage.enterVisitorLastName(new UserUtils().generateRandomLastName());
        visitsPage.enterVisitorFirstNameArabic(new UserUtils().generateRandomArabicFirstName());
        visitsPage.enterVisitorLastNameArabic(new UserUtils().generateRandomArabicLastName());
        visitsPage.enterVisitorPhoneNumber(new UserUtils().generateRandomPhoneNumber());
        visitsPage.enterVisitorEmail(new UserUtils().generateRandomEmail());
        visitsPage.enterVisitorCompanyName(new UserUtils().generateRandomCompanyName());
        visitsPage.enterVisitorNationalId(new UserUtils().generateRandomNationalId());
        String idImage = new UserUtils().getVisitorImagePath();

        visitsPage.uploadVisitorPhoto(idImage);
        visitsPage.uploadNationalIdPhoto(idImage);

        visitsPage.toggleParkingRequired();
        visitsPage.selectVehicleModel("Car");

        visitsPage.enterVehiclePlateLetters(plateLetters);
        visitsPage.enterVehiclePlateDigits(new UserUtils().generateRandomVehiclePlateDigits());

        if (!acceptedByInputMask) {
            String actualValue = visitsPage.getVehiclePlateLettersValue();
            System.out.println("[Vehicle Plate Letter Test] Field value after typing '" + plateLetters
                    + "': '" + actualValue + "'");

            String expectedValue = String.join(" ", plateLetters.split(""));
            Assert.assertEquals(actualValue, expectedValue,
                    "Vehicle Plate Letters field should accept the approved Saudi letter '" + letter
                            + "', but every keystroke was silently dropped and the field is empty.");
            return;
        }

        visitsPage.clickNextButton();
        visitsPage.clickSubmitButton();

        // The rejection toast is transient and can auto-dismiss well before
        // isVisitCreatedSuccessfully's own (much longer) wait below gives up - confirmed directly:
        // a screenshot taken only after that wait times out shows a perfectly normal page with no
        // toast at all, because by then it's long gone. Capturing it here, the moment it's detected,
        // is what actually gets the real error into the Allure report instead of a stale page.
        boolean toastShown = visitsPage.waitForPlateValidationToast();
        System.out.println("[Vehicle Plate Letter Test] Rejection toast shown for '" + plateLetters + "': " + toastShown);
        if (toastShown) {
            byte[] screenshot = ((TakesScreenshot) getDriver()).getScreenshotAs(OutputType.BYTES);
            Allure.addAttachment("Invalid plate letter toast - " + letter, new ByteArrayInputStream(screenshot));
        }

        Assert.assertTrue(visitsPage.isVisitCreatedSuccessfully(),
                "Visit should be created successfully, but plate letters '" + plateLetters
                        + "' fall outside the approved Saudi plate letter set (" + letter + " is rejected), so the"
                        + " backend blocks submission with 'Invalid plate letter. Only approved Saudi plate"
                        + " letters are allowed.' instead of creating the visit. Rejection toast observed: "
                        + toastShown);
    }
}

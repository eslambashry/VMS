package VMS.visits.regular;

import api.AuthClient;
import api.LookupParser;
import api.VisitMngtApiClient;
import api.WorkingHoursParser;
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
import pages.VisitsPage;
import utlis.UserUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

// REG-05: a visitor can have a vehicle attached. Confirmed directly (not guessed): the Vehicle
// section (model dropdown + plate number) only appears once "Parking Required" is toggled on -
// there's no separate "Has Vehicle" control. Only reachable while entering a visitor's details
// manually (not via the search-and-select-existing-visitor flow), same as
// CreateRegularVisitRequestTestWithNewVisitorDataTest.
@Epic("Visit Management")
@Feature("Regular Visit")
public class CreateVisitWithVehicleTest extends BaseTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @Story("Create Regular Request With Visitor Vehicle")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Create Regular Visit With A Visitor's Vehicle", groups = {"regression", "visit", "regular"})
    public void createRegularVisitRequestWithVisitorVehicle() {
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

        String carPlateLetter = new UserUtils().generateRandomVehiclePlateLetters();
        String carPlateNumber = new UserUtils().generateRandomVehiclePlateDigits();

        System.out.println("carPlateLetter: " + carPlateLetter);
        System.out.println("carPlateNumber: " + carPlateNumber);

        visitsPage.enterVehiclePlateLetters(carPlateLetter);
        visitsPage.enterVehiclePlateDigits(carPlateNumber);

        // See VisitsPage.clickNextRetryingTransientPlateValidationError - the backend's async
        // plate check occasionally errors out transiently on a perfectly valid plate; clearing and
        // retyping the same letters/digits (the manual workaround) forces a fresh check.
        visitsPage.clickNextRetryingTransientPlateValidationError(carPlateLetter, carPlateNumber);

        visitsPage.clickSubmitButton();

        Assert.assertTrue(
                visitsPage.isVisitCreatedSuccessfully(),
                "Visit should be created successfully"
        );
    }
}

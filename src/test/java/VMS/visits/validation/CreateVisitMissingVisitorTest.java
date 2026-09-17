package VMS.visits.validation;

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

// VAL-04: a visit requires at least one (complete) visitor. Confirmed directly: Next stays
// disabled on the Visitors Info step while the default Visitor 1 card is empty, and - the same
// "touched" mechanism as the Visit Info step's required fields (see
// CreateVisitMissingRequiredFieldTest) - each visitor field's own error only renders once it's
// been focused and blurred.
@Epic("Visit Management")
@Feature("Validation")
public class CreateVisitMissingVisitorTest extends BaseTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @Story("Visitors Info Required Visitor Validation")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Next Is Blocked When No Visitor Has Been Added",
            groups = {"negative", "regression", "visit", "regular"})
    public void createVisitFailsWhenNoVisitorAdded() {
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

        // Visitor 1's card is present but empty by default - touching just its first name field is
        // enough to prove the same per-field validation mechanism applies here too, without needing
        // to exhaustively touch every visitor field.
        visitsPage.touchVisitorFirstNameField();

        Assert.assertTrue(visitsPage.isNextButtonDisabled(),
                "Next button should stay disabled while no complete visitor has been added");
        Assert.assertTrue(visitsPage.isValidationErrorDisplayed("Visitor First Name in English is required"),
                "Expected validation error not shown for the empty visitor field");
    }
}

package VMS.visits.regular;

import api.*;
import base.BaseTest;
import config.TestConfig;
import factory.DriverFactory;
import io.qameta.allure.*;
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

@Epic("Visit Management")
@Feature("Regular Visit")
public class CreateNormalVisitRequestTest extends BaseTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @Story("Create Regular Request")
    @Severity(SeverityLevel.CRITICAL)
    @Test(description = "Create Regular Visit", groups = {"smoke", "regression", "visit", "regular"})
    public void createRegularVisitRequest() {
        // A direct API login (separate from the browser session below) gets a real access token,
        // so backend data needed for the test (like working hours) can be fetched directly -
        // faster and far more reliable than intercepting browser network traffic for it.
        String accessToken = AuthClient.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);
        String workingHoursJson = VisitMngtApiClient.getWorkingHours(accessToken);
        List<WorkingHoursParser.WorkingHours> workingHoursList = WorkingHoursParser.parse(workingHoursJson);
        String visitZone = LookupParser.firstActiveNameEn(VisitMngtApiClient.getVisitZones(accessToken));
        String visitorNameEn = VisitorParser.firstCompleteVisitorNameEn(VisitMngtApiClient.getVisitors(accessToken));

        setDriver(DriverFactory.generateDriver(TestConfig.BROWSER));
        getDriver().get(TestConfig.AUTH_URL);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);
        loginPage.isLoggedIn();

        System.out.println(loginPage.isLoggedIn());


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
        String Purpose = new UserUtils().generateRandomPurpose();
        visitsPage.enterPurposeOfVisit(Purpose);
        visitsPage.selectVisitZone(visitZone);

        visitsPage.clickNextButton();

        visitsPage.searchAndSelectVisitor(visitorNameEn);

        visitsPage.clickNextButton();

        visitsPage.clickSubmitButton();

        Assert.assertTrue(
                visitsPage.isVisitCreatedSuccessfully(),
                "Visit should be created successfully"
        );
    }
}

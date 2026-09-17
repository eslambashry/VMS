package VMS.visits.regular;

import api.AuthClient;
import api.LookupParser;
import api.VisitMngtApiClient;
import api.VisitRequestParser;
import api.VisitorParser;
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

@Epic("Visit Management")
@Feature("Regular Visit")
public class CreateVisitAnotherHostTest extends BaseTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @Story("Create Regular Request With Another Host")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Create Regular Visit With Another Employee As Host", groups = {"regression", "visit", "regular"})
    public void createRegularVisitRequestWithAnotherHost() {
        String accessToken = AuthClient.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);
        String workingHoursJson = VisitMngtApiClient.getWorkingHours(accessToken);
        List<WorkingHoursParser.WorkingHours> workingHoursList = WorkingHoursParser.parse(workingHoursJson);
        String visitZone = LookupParser.firstActiveNameEn(VisitMngtApiClient.getVisitZones(accessToken));
        String visitorNameEn = VisitorParser.firstCompleteVisitorNameEn(VisitMngtApiClient.getVisitors(accessToken));
        // Picks an employee other than the logged-in user (see LookupParser.firstActiveNameEnExcluding)
        // so this genuinely exercises "another host", not a same-person coincidence.
        String hostEmployeeName = LookupParser.firstActiveNameEnExcluding(
                VisitMngtApiClient.getEmployees(accessToken), TestConfig.REGULAR_USERNAME);

        setDriver(DriverFactory.generateDriver(TestConfig.BROWSER));
        getDriver().get(TestConfig.AUTH_URL);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);
        getDriver().navigate().to(TestConfig.CREATE_VISIT_URL);

        VisitsPage visitsPage = new VisitsPage(getDriver());
        LocalDate visitDay = visitsPage.selectVisitDay(workingHoursList);
        System.out.println(visitDay);
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

        visitsPage.switchToAnotherHost();

        visitsPage.selectHostEmployee(hostEmployeeName);

        visitsPage.clickNextButton();

        visitsPage.searchAndSelectVisitor(visitorNameEn);

        visitsPage.clickNextButton();

        visitsPage.clickSubmitButton();

        Assert.assertTrue(
                visitsPage.isVisitCreatedSuccessfully(),
                "Visit should be created successfully"
        );

        // The success modal alone only proves "a visit was submitted" - it says nothing about which
        // host ended up on it. This is the actual behavior this test exists to verify: the backend's
        // record of the request really does show the selected employee as host, not "Me".
        boolean hostSetCorrectly = VisitRequestParser.pendingRequestExistsForHost(
                VisitMngtApiClient.getVisitRequests(accessToken), TestConfig.REGULAR_USERNAME, hostEmployeeName);
        Assert.assertTrue(hostSetCorrectly,
                "Created visit request should show " + hostEmployeeName + " as host, not the requester" + TestConfig.REGULAR_USERNAME);
    }
}
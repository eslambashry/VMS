package VMS.visits.regular;

import api.AuthClient;
import api.LookupParser;
import api.SettingsParser;
import api.VisitMngtApiClient;
import api.VisitRequestDetailsParser;
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

// REG-08 (boundary): creating a visit with exactly the configured maximum number of visitors
// should succeed. The limit itself is read live from the admin settings API rather than
// hardcoded, since it's an admin-configurable value (confirmed via GET /Visit-mngt/settings ->
// maxVisitorsPerVisit) and not a fixed constant of the system.
@Epic("Visit Management")
@Feature("Regular Visit")
public class CreateVisitMaxVisitorBoundaryTest extends BaseTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @Story("Create Regular Request At Maximum Visitor Boundary")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Create Regular Visit With The Maximum Allowed Number Of Visitors", groups = {"regression", "visit", "regular"})
    public void createRegularVisitRequestAtMaxVisitorBoundary() {
        String accessToken = AuthClient.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);
        String workingHoursJson = VisitMngtApiClient.getWorkingHours(accessToken);
        List<WorkingHoursParser.WorkingHours> workingHoursList = WorkingHoursParser.parse(workingHoursJson);
        String visitZone = LookupParser.firstActiveNameEn(VisitMngtApiClient.getVisitZones(accessToken));

        int maxVisitors = SettingsParser.maxVisitorsPerVisit(VisitMngtApiClient.getSettings(accessToken));

        // Distinct visitors, not the same one repeated - duplicate-visitor handling is an
        // unconfirmed business rule (see docs), so this avoids relying on it either way.
        List<String> visitorNames = VisitorParser.completeVisitorNamesEn(VisitMngtApiClient.getVisitors(accessToken));
        Assert.assertTrue(visitorNames.size() >= maxVisitors,
                "Need at least " + maxVisitors + " visitors with complete data to test this boundary; found "
                        + visitorNames.size());
        List<String> chosenVisitorNames = visitorNames.subList(0, maxVisitors);

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
        // Captured (not just entered) so the request can be found again afterward by matching this
        // exact value via the details endpoint - see the lookup below.
        String purpose = new UserUtils().generateRandomPurpose();
        visitsPage.enterPurposeOfVisit(purpose);
        visitsPage.selectVisitZone(visitZone);

        visitsPage.clickNextButton();

        visitsPage.searchAndSelectVisitor(chosenVisitorNames.get(0), 1);
        for (int position = 2; position <= maxVisitors; position++) {
            visitsPage.clickAddVisitor();
            visitsPage.searchAndSelectVisitor(chosenVisitorNames.get(position - 1), position);
        }

        visitsPage.clickNextButton();

        visitsPage.clickSubmitButton();

        Assert.assertTrue(
                visitsPage.isVisitCreatedSuccessfully(),
                "Visit should be created successfully with the maximum allowed number of visitors (" + maxVisitors + ")"
        );

        // Same reasoning as CreateVisitMultipleVisitorsTest: the success modal alone doesn't prove
        // how many visitors actually attached, and this request stays "Me"-hosted so host name can't
        // disambiguate it either - purpose text is what's unique to this specific request.
        List<String> candidateIds = VisitRequestParser.pendingRequestIdsForRequester(
                VisitMngtApiClient.getVisitRequests(accessToken), TestConfig.REGULAR_USERNAME);
        String matchedRequestId = candidateIds.stream()
                .filter(id -> purpose.equals(VisitRequestDetailsParser.purposeOfVisit(
                        VisitMngtApiClient.getVisitRequestDetails(accessToken, id))))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Could not find the just-created request (purpose: " + purpose + ") among "
                                + TestConfig.REGULAR_USERNAME + "'s pending requests"));

        List<String> actualVisitorNames = VisitRequestDetailsParser.visitorNamesEn(
                VisitMngtApiClient.getVisitRequestDetails(accessToken, matchedRequestId));

        Assert.assertEquals(actualVisitorNames.size(), maxVisitors,
                "Created visit request should have exactly " + maxVisitors + " visitors, found: " + actualVisitorNames);
        Assert.assertTrue(actualVisitorNames.containsAll(chosenVisitorNames),
                "Created visit request's visitors " + actualVisitorNames
                        + " should include all " + maxVisitors + " selected visitors: " + chosenVisitorNames);
    }
}

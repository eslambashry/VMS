package VMS.visits.regular;

import api.AuthClient;
import api.LookupParser;
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

@Epic("Visit Management")
@Feature("Regular Visit")
public class CreateVisitMultipleVisitorsTest extends BaseTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @Story("Create Regular Request With Multiple Visitors")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Create Regular Visit With Two Visitors", groups = {"regression", "visit", "regular"})
    public void createRegularVisitRequestWithMultipleVisitors() {
        String accessToken = AuthClient.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);
        String workingHoursJson = VisitMngtApiClient.getWorkingHours(accessToken);
        List<WorkingHoursParser.WorkingHours> workingHoursList = WorkingHoursParser.parse(workingHoursJson);
        String visitZone = LookupParser.firstActiveNameEn(VisitMngtApiClient.getVisitZones(accessToken));

        // Two distinct visitors, not the same one twice - duplicate-visitor handling is an
        // unconfirmed business rule (see docs), so this avoids relying on it either way.
        List<String> visitorNames = VisitorParser.completeVisitorNamesEn(VisitMngtApiClient.getVisitors(accessToken));
        Assert.assertTrue(visitorNames.size() >= 2,
                "Need at least 2 visitors with complete data to run this test; found " + visitorNames.size());
        String firstVisitorNameEn = visitorNames.get(0);
        String secondVisitorNameEn = visitorNames.get(1);
        System.out.println("first visitor name: " + firstVisitorNameEn);
        System.out.println("second visitor name: " + secondVisitorNameEn);
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

        visitsPage.searchAndSelectVisitor(firstVisitorNameEn, 1);
        visitsPage.clickAddVisitor();
        visitsPage.searchAndSelectVisitor(secondVisitorNameEn, 2);

        visitsPage.clickNextButton();

        visitsPage.clickSubmitButton();

        Assert.assertTrue(
                visitsPage.isVisitCreatedSuccessfully(),
                "Visit should be created successfully"
        );

        // The success modal alone only proves "a visit was submitted" - it says nothing about how
        // many visitors ended up on it. This request stays "Me"-hosted like other regular-visit
        // tests, so host name can't disambiguate it (unlike CreateVisitAnotherHostTest) - the
        // random purpose text captured above is what's unique to this specific request among any
        // other pending ones the same account might have at the same moment.
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

        Assert.assertEquals(actualVisitorNames.size(), 2,
                "Created visit request should have exactly 2 visitors, found: " + actualVisitorNames);
        Assert.assertTrue(actualVisitorNames.containsAll(List.of(firstVisitorNameEn, secondVisitorNameEn)),
                "Created visit request's visitors " + actualVisitorNames
                        + " should include both selected visitors: " + firstVisitorNameEn + ", " + secondVisitorNameEn);
    }
}
package VMS.visits.validation;

import api.AuthClient;
import api.LookupParser;
import api.SettingsParser;
import api.VisitMngtApiClient;
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

// VAL-05: a visit cannot exceed the configured maximum number of visitors. Confirmed directly
// (not guessed): once maxVisitorsPerVisit is reached, "Add Visitor" gets a native disabled
// attribute - there's no error message to wait for past that point, the button itself simply
// refuses further clicks. The limit is read live from the admin settings API (see
// CreateVisitMaxVisitorBoundaryTest), not hardcoded, since it's admin-configurable.
@Epic("Visit Management")
@Feature("Validation")
public class CreateVisitExceedMaxVisitorTest extends BaseTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @Story("Exceed Maximum Visitor Limit Validation")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Add Visitor Is Blocked Once The Maximum Visitor Limit Is Reached",
            groups = {"negative", "regression", "visit", "regular"})
    public void addVisitorIsDisabledAtMaxVisitorLimit() {
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
        visitsPage.enterPurposeOfVisit(new UserUtils().generateRandomPurpose());
        visitsPage.selectVisitZone(visitZone);

        visitsPage.clickNextButton();

        visitsPage.searchAndSelectVisitor(chosenVisitorNames.get(0), 1);
        for (int position = 2; position <= maxVisitors; position++) {
            visitsPage.clickAddVisitor();
            visitsPage.searchAndSelectVisitor(chosenVisitorNames.get(position - 1), position);
        }

        Assert.assertTrue(visitsPage.isAddVisitorButtonDisabled(),
                "Add Visitor should be disabled once the maximum of " + maxVisitors + " visitors is reached");
    }
}

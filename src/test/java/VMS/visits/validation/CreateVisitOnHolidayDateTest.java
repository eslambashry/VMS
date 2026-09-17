package VMS.visits.validation;

import api.AuthClient;
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

import java.time.LocalDate;
import java.util.List;

// Working-hours API marks Saturday (dayOfWeek=7) as isWorkingDay=false. Confirmed directly (not
// guessed) that the calendar renders such days with the same "day disabled" class it uses for past
// dates, so they can't be picked as a visit date. The holiday date is derived from the API response
// rather than hardcoded as "Saturday" so the test keeps working if the configured working days
// change. Also asserts a genuine working day stays enabled, as a control - otherwise the test could
// pass even if the calendar disabled every date.
@Epic("Visit Management")
@Feature("Validation")
public class CreateVisitOnHolidayDateTest extends BaseTest {

    @Story("Visit Date Holiday Validation")
    @Severity(SeverityLevel.NORMAL)
    @Test(description = "Calendar Disables Non-Working (Holiday) Dates",
            groups = {"negative", "regression", "visit", "regular"})
    public void calendarDisablesHolidayDate() {
        String accessToken = AuthClient.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);
        String workingHoursJson = VisitMngtApiClient.getWorkingHours(accessToken);
        List<WorkingHoursParser.WorkingHours> workingHoursList = WorkingHoursParser.parse(workingHoursJson);

        LocalDate holiday = WorkingHoursParser.nextNonWorkingDay(workingHoursList, LocalDate.now().plusDays(1));
        LocalDate workingDay = WorkingHoursParser.nextWorkingDay(workingHoursList, LocalDate.now().plusDays(1));

        setDriver(DriverFactory.generateDriver(TestConfig.BROWSER));
        getDriver().get(TestConfig.AUTH_URL);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(TestConfig.REGULAR_USERNAME, TestConfig.REGULAR_PASSWORD);
        getDriver().navigate().to(TestConfig.CREATE_VISIT_URL);

        VisitsPage visitsPage = new VisitsPage(getDriver());
        visitsPage.openCalendar();

        Assert.assertTrue(visitsPage.isDayDisabled(holiday),
                "Holiday date " + holiday + " should be disabled in the calendar");
        Assert.assertFalse(visitsPage.isDayDisabled(workingDay),
                "Working date " + workingDay + " should be selectable in the calendar");
    }
}

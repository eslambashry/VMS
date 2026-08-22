package VMS;

import api.AuthClient;
import api.LookupParser;
import api.VisitMngtApiClient;
import api.WorkingHoursParser;
import base.BaseTest;
import config.TestConfig;
import factory.DriverFactory;
import io.qameta.allure.Story;
import org.testng.annotations.Test;
import pages.LoginPage;
import pages.VisitsPage;
import utlis.UserUtils;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class CreateRegularVisitRequestTest extends BaseTest {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("hh:mm a", Locale.ENGLISH);

    @Story("Create Regular Request")
    @Test(description = "Create Regular Visit")
    public void createRegularVisitRequest() {
        // A direct API login (separate from the browser session below) gets a real access token,
        // so backend data needed for the test (like working hours) can be fetched directly -
        // faster and far more reliable than intercepting browser network traffic for it.
        String accessToken = AuthClient.login(TestConfig.USERNAME, TestConfig.PASSWORD);
        String workingHoursJson = VisitMngtApiClient.getWorkingHours(accessToken);
        List<WorkingHoursParser.WorkingHours> workingHoursList = WorkingHoursParser.parse(workingHoursJson);
        String visitZone = LookupParser.firstActiveNameEn(VisitMngtApiClient.getVisitZones(accessToken));

        setDriver(DriverFactory.generateDriver(TestConfig.BROWSER));
        getDriver().get(TestConfig.AUTH_URL);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(TestConfig.USERNAME, TestConfig.PASSWORD);
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
        System.out.println("[computed times] visitDay=" + visitDay + " startTime=" + startTime + " endTime=" + endTime);



        visitsPage.selectStartTime(startTime.format(TIME_FORMAT));
        visitsPage.selectEndTime(endTime.format(TIME_FORMAT));

        visitsPage.selectVisitType("Maintenance");
        String Purpose = new UserUtils().generateRandomPurpose();
        visitsPage.enterPurposeOfVisit(Purpose);
        visitsPage.selectVisitZone(visitZone);



      // visitsPage.clickNext();
    }
}

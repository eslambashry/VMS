package VMS;

import api.AuthClient;
import api.VisitMngtApiClient;
import api.VisitRequestParser;
import base.BaseTest;
import config.TestConfig;
import factory.DriverFactory;
import io.qameta.allure.Story;
import org.testng.annotations.Test;
import pages.LoginPage;
import pages.VisitRequestsPage;

public class ApproveRegularVisitRequestTest extends BaseTest {

    @Story("Approve Regular Request")
    @Test(description = "Approve Regular Visit")
    public void approveRegularVisitRequest() {
        // Confirms a pending request actually exists (and which one) before touching the UI, so a
        // missing pending request fails fast with a clear message instead of a 90s Selenium timeout.
        String accessToken = AuthClient.login(TestConfig.MANAGER_USERNAME, TestConfig.MANAGER_PASSWORD);
        String pendingRequestNumber = VisitRequestParser.firstPendingRequestNumber(VisitMngtApiClient.getVisitRequests(accessToken));
        System.out.println("Approving: " + pendingRequestNumber);

        setDriver(DriverFactory.generateDriver(TestConfig.BROWSER));
        getDriver().get(TestConfig.VISIT_REQUESTS);

        LoginPage loginPage = new LoginPage(getDriver());
        loginPage.login(TestConfig.MANAGER_USERNAME, TestConfig.MANAGER_PASSWORD);
        getDriver().get(TestConfig.VISIT_REQUESTS);

        // The manager account lands on the Visit Requests list right after login - no extra navigation needed.

        VisitRequestsPage visitRequestsPage = new VisitRequestsPage(getDriver());
        visitRequestsPage.approveFirstPendingRequest();
    }
}

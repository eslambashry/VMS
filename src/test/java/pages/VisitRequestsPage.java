package pages;

import base.BasePage;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.support.ui.ExpectedConditions;

public class VisitRequestsPage extends BasePage {

    public VisitRequestsPage(WebDriver driver) {
        super(driver);
    }

    // Only pending rows render an "Approve All" button at all (other rows just show plain status
    // text like "Cancelled"/"Approved All") and the list sorts pending requests first, so the first
    // match on the page is the same request VisitRequestParser.firstPendingRequestNumber found.
    private static final By APPROVE_ALL_BUTTON =
            By.xpath("(//button[.//span[normalize-space(text())='Approve All']])[1]");

    private static final By CONFIRM_APPROVE_ALL_VISITORS_BUTTON =
            By.xpath("//button[contains(@class,'primary')][.//span[normalize-space(text())='Approve All Visitors']]");

    public void approveFirstPendingRequest(){
        wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
        wait.until(ExpectedConditions.elementToBeClickable(APPROVE_ALL_BUTTON)).click();
        wait.until(ExpectedConditions.elementToBeClickable(CONFIRM_APPROVE_ALL_VISITORS_BUTTON)).click();
    }
}

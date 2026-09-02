package components;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Drives the app's custom scroll-wheel "Pickup Time" widget (used by both Start Time and End Time).
// Kept separate from any page object so the widget's quirks are handled in exactly one place:
//   - values far from the wheel's center are visually clipped and reject direct clicks
//   - values outside the currently rendered scroll window don't exist in the DOM yet
// Both are worked around by repeatedly clicking the immediate neighbor of whatever is currently
// selected (always close to center, always rendered) until the target value becomes selected.
public class TimePickerComponent {

    private static final Pattern TIME_PATTERN = Pattern.compile("(\\d{1,2}):(\\d{2})\\s*([AaPp][Mm])");
    private static final String MODAL_XPATH = "(//div[contains(@class,'time-picker-modal')])[last()]";

    private final WebDriver driver;
    private final WebDriverWait wait;
    private final WebElement opener;

    public TimePickerComponent(WebDriver driver, WebDriverWait wait, WebElement opener){
        this.driver = driver;
        this.wait = wait;
        this.opener = opener;
    }

    // Accepts the same "09:30 AM" format shown on screen.
    public void selectTime(String time){
        opener.click();

        Matcher matcher = TIME_PATTERN.matcher(time.trim());
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Time must look like '09:30 AM', got: " + time);
        }
        // The picker's hour items are zero-padded ("08", "09", ...), so pad single-digit hour to match.
        String hour = String.format("%02d", Integer.parseInt(matcher.group(1)));
        String minute = matcher.group(2);
        String period = matcher.group(3).toUpperCase();

        // AM/PM must be picked before the hour: the widget's available hour values depend on which
        // period is currently selected (e.g. "12" isn't reachable until PM is chosen), so picking
        // hour first can hit a value that isn't in the current period's range and get stuck.
        selectColumnValue(periodColumnXpath(), period);
        selectColumnValue(hourColumnXpath(), hour);
        selectColumnValue(minuteColumnXpath(), minute);

        wait.until(ExpectedConditions.elementToBeClickable(By.xpath(MODAL_XPATH + "//button[contains(@class,'done-btn')]"))).click();
    }

    private String hourColumnXpath(){
        return MODAL_XPATH + "//div[contains(@class,'time-column') and not(contains(@class,'period-column'))][1]";
    }

    private String minuteColumnXpath(){
        return MODAL_XPATH + "//div[contains(@class,'time-column') and not(contains(@class,'period-column'))][2]";
    }

    private String periodColumnXpath(){
        return MODAL_XPATH + "//div[contains(@class,'period-column')]";
    }

    private void selectColumnValue(String columnXpath, String targetText){
        int target = parseTimeValue(targetText);
        // Only the AM/PM column marks the centered item with a "selected" class - the hour and
        // minute columns only mark it via inline style (opacity: 1; font-weight: bold), so both
        // signals have to be accepted here.
        String selectedXpath = columnXpath +
                "//div[contains(@class,'time-item') and (contains(@class,'selected') or contains(@style,'font-weight: bold'))]";

        for (int i = 0; i < 30; i++) {
            WebElement selected;
            try {
                selected = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(selectedXpath)));
            } catch (TimeoutException e) {
                System.out.println("[TimePickerComponent] no centered item found. Modal HTML:\n" + dumpModalHtml());
                throw e;
            }
            int current = parseTimeValue(selected.getText().trim());
            if (current == target) {
                return;
            }
            String direction = current < target ? "following-sibling" : "preceding-sibling";
            String neighborXpath = selectedXpath + "/" + direction + "::div[contains(@class,'time-item')][1]";
            try {
                wait.until(ExpectedConditions.elementToBeClickable(By.xpath(neighborXpath))).click();
            } catch (TimeoutException e) {
                // The widget can still be settling onto its own default (e.g. End Time defaults to
                // Start Time + 1h) when "selected" was first read above - if it already landed on the
                // target by now, there's no neighbor in the stale direction to click, which isn't a
                // real failure. Only re-throw if it genuinely didn't reach the target.
                int nowCurrent = parseTimeValue(driver.findElement(By.xpath(selectedXpath)).getText().trim());
                if (nowCurrent == target) {
                    return;
                }
                throw e;
            }
        }
        System.out.println("[TimePickerComponent] exhausted attempts selecting '" + targetText + "'. Modal HTML:\n" + dumpModalHtml());
        throw new IllegalStateException("Could not select '" + targetText + "' in time picker column");
    }

    private String dumpModalHtml(){
        Object result = ((JavascriptExecutor) driver).executeScript(
                "var m = document.querySelectorAll('.time-picker-modal');"
                        + "return m.length ? m[m.length - 1].outerHTML : 'NO MODAL FOUND IN DOM';");
        return String.valueOf(result);
    }

    private int parseTimeValue(String text){
        if (text.equalsIgnoreCase("AM")) return 0;
        if (text.equalsIgnoreCase("PM")) return 1;
        return Integer.parseInt(text.trim());
    }
}

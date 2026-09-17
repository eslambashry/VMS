package components;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.TimeoutException;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;
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
    // Deliberately short and local, not the shared page-level `wait` (which can be configured for
    // 30s+ to cover legitimately slow things like page loads elsewhere in the suite). Every element
    // this drives is documented to be "always close to center, always rendered" - i.e. expected to
    // already be there. Borrowing the long shared timeout here just meant a single transient miss
    // silently ate 30s before falling through to a retry; inside a loop that can run up to 30 times,
    // a few of those stalls compounded into multi-minute hangs that looked like the test was stuck,
    // rather than the fast-failing retry loop this was meant to be.
    private final WebDriverWait shortWait;
    private final WebElement opener;

    public TimePickerComponent(WebDriver driver, WebDriverWait wait, WebElement opener){
        this.driver = driver;
        this.wait = wait;
        this.shortWait = new WebDriverWait(driver, Duration.ofSeconds(5));
        this.opener = opener;
    }

    // Accepts the same "09:30 AM" format shown on screen.
    public void selectTime(String time){
        openModal();

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

    // Same loader-flashing-over-the-field race as the calendar input and dropdowns elsewhere on
    // this page, but here it's compounded: the field-clickable check only proves the loader wasn't
    // covering the opener at that instant, not at the instant the click itself lands a moment later.
    // On forms with extra async traffic (e.g. vehicle/plate lookups, see DiagPlateNetworkTest) the
    // loader can re-appear in that gap and intercept the click, so the click silently lands on the
    // loader instead of the opener and the modal never opens - previously observed as a 30s
    // "visibility of time-picker-modal" timeout that only reproduced intermittently.
    //
    // Retry the click a few times with a shorter per-attempt wait instead of trusting a single
    // click - but only re-click when the modal never made it into the DOM at all (a genuinely
    // swallowed click). If it's present but just still animating in, re-clicking here would toggle
    // an already-open modal shut instead of helping, so that case only gets a longer wait, no re-click.
    private void openModal(){
        TimeoutException lastFailure = null;
        for (int attempt = 0; attempt < 3; attempt++) {
            if (attempt == 0 || driver.findElements(By.xpath(MODAL_XPATH)).isEmpty()) {
                wait.until(ExpectedConditions.invisibilityOfElementLocated(By.className("global-loader-container")));
                wait.until(ExpectedConditions.elementToBeClickable(opener)).click();
            }
            try {
                // The modal's open animation and its default-centered-value styling can still be
                // settling right after the click - wait for the modal itself before probing inside
                // it for a "selected" item, otherwise that probe can intermittently time out on a
                // modal that just hasn't finished rendering yet.
                new WebDriverWait(driver, Duration.ofSeconds(30))
                        .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(MODAL_XPATH)));
                return;
            } catch (TimeoutException e) {
                lastFailure = e;
            }
        }
        throw lastFailure;
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
                selected = shortWait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath(selectedXpath)));
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
                shortWait.until(ExpectedConditions.elementToBeClickable(By.xpath(neighborXpath))).click();
            } catch (TimeoutException e) {
                // The widget can still be settling onto its own default (e.g. End Time defaults to
                // Start Time + 1h) when "selected" was first read above - if it already landed on the
                // target by now, there's no neighbor in the stale direction to click, which isn't a
                // real failure. Only re-throw if it genuinely didn't reach the target.
                //
                // Checked with a short wait rather than a bare findElement: the "selected" element
                // can be transiently absent from the DOM for a moment while the widget re-renders
                // (observed directly - a bare findElement here intermittently threw
                // NoSuchElementException instead of correctly detecting the already-reached target,
                // especially under heavier load), so an instantaneous check is itself race-prone.
                try {
                    WebElement settled = new WebDriverWait(driver, Duration.ofSeconds(3))
                            .until(ExpectedConditions.visibilityOfElementLocated(By.xpath(selectedXpath)));
                    if (parseTimeValue(settled.getText().trim()) == target) {
                        return;
                    }
                } catch (TimeoutException stillNotSettled) {
                    // Falls through to rethrow the original timeout below.
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

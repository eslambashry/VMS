package api;

import org.openqa.selenium.json.Json;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;

// Parses the working-hours API response (see VisitMngtApiClient) and answers business
// questions about it. Has no network code of its own - fetching and parsing are kept separate.
public class WorkingHoursParser {

    public record WorkingHours(int dayOfWeek, boolean isWorkingDay, LocalTime startTime, LocalTime endTime) {}

    @SuppressWarnings("unchecked")
    public static List<WorkingHours> parse(String responseJson){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        return data.stream().map(WorkingHoursParser::toWorkingHours).toList();
    }

    private static WorkingHours toWorkingHours(Map<String, Object> entry){
        int dayOfWeek = ((Number) entry.get("dayOfWeek")).intValue();
        boolean isWorkingDay = (Boolean) entry.get("isWorkingDay");
        LocalTime startTime = LocalTime.parse((String) entry.get("startTime"));
        LocalTime endTime = LocalTime.parse((String) entry.get("endTime"));
        return new WorkingHours(dayOfWeek, isWorkingDay, startTime, endTime);
    }

    // API numbers days 1=Sunday..7=Saturday; java.time.DayOfWeek numbers them 1=Monday..7=Sunday.
    private static int toApiDayOfWeek(LocalDate date){
        return date.getDayOfWeek().getValue() % 7 + 1;
    }

    public static WorkingHours forDate(List<WorkingHours> workingHours, LocalDate date){
        int apiDay = toApiDayOfWeek(date);
        return workingHours.stream()
                .filter(w -> w.dayOfWeek() == apiDay)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No working hours entry for day " + apiDay));
    }

private static LocalDate nextDayMatching(List<WorkingHours> workingHours, LocalDate from, boolean workingDayWanted){
        LocalDate candidate = from;
        for (int i = 0; i < 14; i++) {
            int apiDay = toApiDayOfWeek(candidate);
            boolean working = workingHours.stream()
                    .filter(w -> w.dayOfWeek() == apiDay)
                    .findFirst()
                    .map(WorkingHours::isWorkingDay)
                    .orElse(false);
            if (working == workingDayWanted) {
                return candidate;
            }
            candidate = candidate.plusDays(1);
        }
        throw new IllegalStateException("No " + (workingDayWanted ? "working" : "non-working")
                + " day found within 14 days of " + from);
    }

    public static LocalDate nextWorkingDay(List<WorkingHours> workingHours, LocalDate from){
        return nextDayMatching(workingHours, from, true);
    }


    // Mirrors nextWorkingDay - used by negative tests that need a date the calendar should refuse
    // to let the user pick (e.g. Saturday, which the API marks isWorkingDay=false).
    public static LocalDate nextNonWorkingDay(List<WorkingHours> workingHours, LocalDate from){
        return nextDayMatching(workingHours, from, false);
    }
}

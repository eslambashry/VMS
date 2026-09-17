package api;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Creates a pending regular visit request directly against the backend, using the same
// working-day/working-hours validation as the "Create Regular Visit" UI flow (see
// WorkingHoursParser / CreateRegularVisitRequestTest) but posting straight to the API instead of
// driving Selenium through the multi-step form. Lets tests that only care about what happens to
// an existing pending request (e.g. approval) set up their own fixture instead of depending on
// another test having created and left one behind.
public class VisitRequestBuilder {

    public static String createPendingRegularVisit(String accessToken, String purposeOfVisit){
        List<WorkingHoursParser.WorkingHours> workingHoursList =
                WorkingHoursParser.parse(VisitMngtApiClient.getWorkingHours(accessToken));
        LocalDate visitDay = WorkingHoursParser.nextWorkingDay(workingHoursList, LocalDate.now().plusDays(1));
        WorkingHoursParser.WorkingHours workingHours = WorkingHoursParser.forDate(workingHoursList, visitDay);

        // Same rounding as the UI flow (10:00 -> 11:00 instead of a ":30" offset) - simpler, valid
        // values with no odd minute to land on.
        LocalTime startTime = workingHours.startTime().getMinute() == 0
                ? workingHours.startTime()
                : workingHours.startTime().plusHours(1).withMinute(0);
        LocalTime endTime = startTime.plusHours(1);
        if (endTime.isAfter(workingHours.endTime())) {
            endTime = workingHours.endTime();
        }

        Map<String, Object> body = buildRegularVisitPayload(accessToken, purposeOfVisit, visitDay, startTime, endTime);
        VisitMngtApiClient.createVisitRequest(accessToken, body);

        // The create endpoint doesn't hand back the new request's number, and requests are listed
        // newest-first - fetching the first pending one right after creating it reliably picks up
        // the one just created (same lookup ApproveRegularVisitRequestTest already relies on).
        return VisitRequestParser.firstPendingRequestNumber(VisitMngtApiClient.getVisitRequests(accessToken));
    }

    // Split out of createPendingRegularVisit so negative tests can build a request payload for a
    // date/time they control directly (e.g. a holiday) without going through nextWorkingDay - see
    // CreateVisitOnHolidayDateApiTest.
    public static Map<String, Object> buildRegularVisitPayload(String accessToken, String purposeOfVisit,
                                                                 LocalDate visitDate, LocalTime startTime, LocalTime endTime){
        String visitZoneId = LookupParser.firstActiveId(VisitMngtApiClient.getVisitZones(accessToken));
        String visitTypeId = LookupParser.firstActiveId(VisitMngtApiClient.getVisitTypes(accessToken));
        Map<String, Object> visitor = VisitorParser.firstCompleteVisitor(VisitMngtApiClient.getVisitors(accessToken));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("visitDate", visitDate.toString());
        body.put("startTime", startTime.toString());
        body.put("endTime", endTime.toString());
        body.put("visitTypeId", visitTypeId);
        body.put("visitZoneId", visitZoneId);
        body.put("purposeOfVisit", purposeOfVisit);
        body.put("isHostMe", true);
        body.put("isRecurring", false);
        body.put("needsRoom", false);
        body.put("hostEmployeeId", "");
        body.put("hostEmployeeName", "");
        body.put("hostEmployeeEmail", "");
        body.put("hostEmployeePosition", "");
        body.put("meetingRoomId", "");
        body.put("meetingRoomName", "");
        body.put("notesToHsse", "");
        body.put("recurrence", null);
        body.put("visitors", List.of(toVisitorPayload(visitor)));
        return body;
    }

    // GET /visitors only has full names (nameEn/nameAr); the create-visit-request payload wants
    // them split into first/last plus the full name - splitting on the first space is enough for
    // this dataset (every visitor here has exactly a first and last name).
    private static Map<String, Object> toVisitorPayload(Map<String, Object> visitor){
        String nameEn = (String) visitor.get("nameEn");
        String nameAr = (String) visitor.get("nameAr");
        String[] enParts = nameEn.split(" ", 2);
        String[] arParts = nameAr.split(" ", 2);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("visitorId", visitor.get("id"));
        payload.put("visitorStatus", "");
        payload.put("englishFirstName", enParts[0]);
        payload.put("englishLastName", enParts.length > 1 ? enParts[1] : "");
        payload.put("englishFullName", nameEn);
        payload.put("arabicFirstName", arParts[0]);
        payload.put("arabicLastName", arParts.length > 1 ? arParts[1] : "");
        payload.put("arabicFullName", nameAr);
        payload.put("company", visitor.get("company"));
        payload.put("countryCode", visitor.get("countryCode"));
        payload.put("email", visitor.get("email"));
        payload.put("mobile", visitor.get("mobile"));
        payload.put("nationalId", visitor.get("nationalId"));
        payload.put("idType", null);
        payload.put("itemIds", List.of());
        payload.put("itemsDeclared", false);
        payload.put("itemsOther", "");
        payload.put("parkingRequired", false);
        return payload;
    }
}

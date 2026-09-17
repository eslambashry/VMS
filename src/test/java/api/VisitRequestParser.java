package api;

import org.openqa.selenium.json.Json;

import java.util.List;
import java.util.Map;

// Parses the visit-requests API response (see VisitMngtApiClient) - same "data.content" shape as
// VisitorParser. Used to confirm a pending request actually exists (and get its number) before
// driving the approval through the UI, rather than blindly clicking whatever the list shows first.
public class VisitRequestParser {

    private static final String PENDING_STATUS = "PENDING_APPROVAL";

    @SuppressWarnings("unchecked")
    public static String firstPendingRequestNumber(String responseJson){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        return content.stream()
                .filter(entry -> PENDING_STATUS.equals(entry.get("requestStatus")))
                .map(entry -> (String) entry.get("requestNumber"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No pending visit request found in visit-requests response"));
    }

    @SuppressWarnings("unchecked")
    public static String statusOf(String responseJson, String requestNumber){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        return content.stream()
                .filter(entry -> requestNumber.equals(entry.get("requestNumber")))
                .map(entry -> (String) entry.get("requestStatus"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Request " + requestNumber + " not found in visit-requests response"));
    }

    // Confirms the actual business outcome of a host-selection test: not just "some visit got
    // created" but that a pending request exists with the specific host that was selected.
    // Matches on requesterEmail + hostEmployeeName together (not the request number, which the
    // create endpoint never hands back) - requesterEmail alone isn't enough to isolate "this" request
    // under parallel execution, since another test using the same account could have a pending
    // request of its own at the same moment. The host name is what makes the match specific to this
    // scenario, since no other current test selects a non-"Me" host.
    @SuppressWarnings("unchecked")
    public static boolean pendingRequestExistsForHost(String responseJson, String requesterEmail, String hostEmployeeName){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        return content.stream().anyMatch(entry ->
                PENDING_STATUS.equals(entry.get("requestStatus"))
                        && requesterEmail.equalsIgnoreCase((String) entry.get("requesterEmail"))
                        && hostEmployeeName.equals(entry.get("hostEmployeeName")));
    }

    // Candidate ids (not requestNumbers - the details endpoint takes id) for a requester's pending
    // requests. Used when there's no single distinguishing list-level field available (e.g.
    // multi-visitor tests, where host stays "Me" like other regular-visit tests) - the caller fetches
    // each candidate's full details (see VisitMngtApiClient.getVisitRequestDetails) and matches on
    // something only the details response has, such as purposeOfVisit.
    @SuppressWarnings("unchecked")
    public static List<String> pendingRequestIdsForRequester(String responseJson, String requesterEmail){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        return content.stream()
                .filter(entry -> PENDING_STATUS.equals(entry.get("requestStatus")))
                .filter(entry -> requesterEmail.equalsIgnoreCase((String) entry.get("requesterEmail")))
                .map(entry -> (String) entry.get("id"))
                .toList();
    }
}

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
}

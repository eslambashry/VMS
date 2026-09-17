package api;

import org.openqa.selenium.json.Json;

import java.util.List;
import java.util.Map;

// Parses the visitors API response (see VisitMngtApiClient) - unlike the lookup endpoints
// (LookupParser), the list here is nested under "data.content" rather than directly under "data".
public class VisitorParser {

    // Some visitor records are missing fields the create-visit form requires (e.g. no Arabic name
    // or no photo) - selecting one of those from the UI's suggestion panel would autofill the form
    // incompletely and fail later validation. Picking a visitor with every required field already
    // present up front avoids that, rather than trying to detect/recover from it after selection.
    private static final List<String> REQUIRED_FIELDS = List.of(
            "nameEn", "nameAr", "nationalId", "email", "mobile", "company", "photoBase64", "idImageBase64");

    @SuppressWarnings("unchecked")
    public static String firstCompleteVisitorNameEn(String responseJson){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        return content.stream()
                .filter(VisitorParser::hasAllRequiredFields)
                .map(entry -> (String) entry.get("nameEn"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No visitor with complete data found in visitors response"));
    }

    private static boolean hasAllRequiredFields(Map<String, Object> entry){
        return REQUIRED_FIELDS.stream().allMatch(field -> entry.get(field) instanceof String s && !s.isBlank());
    }

    // Needed for multi-visitor tests, which must pick two distinct visitors rather than reuse the
    // same one twice - duplicate-visitor handling is an unconfirmed business rule (see docs), so
    // tests should avoid relying on it either way rather than assuming it's accepted or rejected.
    @SuppressWarnings("unchecked")
    public static List<String> completeVisitorNamesEn(String responseJson){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        return content.stream()
                .filter(VisitorParser::hasAllRequiredFields)
                .map(entry -> (String) entry.get("nameEn"))
                .toList();
    }

    // Same "complete data" filter as firstCompleteVisitorNameEn, but hands back the raw entry
    // instead of just the display name - needed to build a create-visit-request payload
    // (see VisitRequestBuilder), which needs more of the visitor's fields than the UI flow does.
    @SuppressWarnings("unchecked")
    public static Map<String, Object> firstCompleteVisitor(String responseJson){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> content = (List<Map<String, Object>>) data.get("content");
        return content.stream()
                .filter(VisitorParser::hasAllRequiredFields)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No visitor with complete data found in visitors response"));
    }
}

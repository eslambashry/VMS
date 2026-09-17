package api;

import org.openqa.selenium.json.Json;

import java.util.List;
import java.util.Map;

// Parses lookup-list API responses (visit zones, visit types, ...) that all share the same
// {id, nameEn, isActive, ...} shape under "data" (see VisitMngtApiClient).
public class LookupParser {

    @SuppressWarnings("unchecked")
    public static String firstActiveNameEn(String responseJson){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        return data.stream()
                .filter(entry -> (Boolean) entry.get("isActive"))
                .map(entry -> (String) entry.get("nameEn"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active entry found in lookup response"));
    }

    @SuppressWarnings("unchecked")
    public static String firstActiveId(String responseJson){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        return data.stream()
                .filter(entry -> (Boolean) entry.get("isActive"))
                .map(entry -> (String) entry.get("id"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No active entry found in lookup response"));
    }

    // Used for the employees lookup (see VisitMngtApiClient.getEmployees) to pick a host that is
    // genuinely someone other than the logged-in user, rather than whichever entry happens to be
    // listed first (which could coincidentally be the user's own account).
    @SuppressWarnings("unchecked")
    public static String firstActiveNameEnExcluding(String responseJson, String excludeEmail){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        List<Map<String, Object>> data = (List<Map<String, Object>>) body.get("data");
        return data.stream()
                .filter(entry -> (Boolean) entry.get("isActive"))
                .filter(entry -> !excludeEmail.equalsIgnoreCase((String) entry.get("email")))
                .map(entry -> (String) entry.get("nameEn"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "No active entry found (excluding " + excludeEmail + ") in lookup response"));
    }
}

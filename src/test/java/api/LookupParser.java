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
}

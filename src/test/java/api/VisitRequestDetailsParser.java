package api;

import org.openqa.selenium.json.Json;

import java.util.List;
import java.util.Map;

// Parses a single visit request's full details (see VisitMngtApiClient.getVisitRequestDetails) -
// a different shape from the list endpoint (VisitRequestParser): "data" here is the request
// itself, not a page of them, and it includes fields (visitors, purposeOfVisit) the list doesn't.
public class VisitRequestDetailsParser {

    @SuppressWarnings("unchecked")
    public static String purposeOfVisit(String detailsJson){
        Map<String, Object> body = new Json().toType(detailsJson, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return (String) data.get("purposeOfVisit");
    }

    // Names of every visitor attached to the request, in the order the API returns them.
    @SuppressWarnings("unchecked")
    public static List<String> visitorNamesEn(String detailsJson){
        Map<String, Object> body = new Json().toType(detailsJson, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        List<Map<String, Object>> visitors = (List<Map<String, Object>>) data.get("visitors");
        return visitors.stream()
                .map(entry -> (Map<String, Object>) entry.get("visitor"))
                .map(visitor -> (String) visitor.get("nameEn"))
                .toList();
    }
}

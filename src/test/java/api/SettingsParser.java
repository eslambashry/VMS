package api;

import org.openqa.selenium.json.Json;

import java.util.Map;

// Parses the admin-configurable Visit-mngt settings (see VisitMngtApiClient.getSettings) - a flat
// object under "data", unlike the list/paged shapes elsewhere in this package.
public class SettingsParser {

    @SuppressWarnings("unchecked")
    public static int maxVisitorsPerVisit(String responseJson){
        Map<String, Object> body = new Json().toType(responseJson, Map.class);
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        return ((Number) data.get("maxVisitorsPerVisit")).intValue();
    }
}

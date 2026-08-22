package config;

// Single source of truth for environment/credential values. Override with -Dvms.xxx=... (e.g. in CI)
// without touching test source; defaults below keep local runs working unchanged.
public class TestConfig {

    public static final String BASE_URL = System.getProperty("vms.baseUrl", "http://10.254.192.60");
    public static final String AUTH_URL = BASE_URL + "/#/auth";
    public static final String CREATE_VISIT_URL = BASE_URL + "/#/visits/create-visit";

    // The backend API lives on a different host than the frontend SPA.
    public static final String API_BASE_URL = System.getProperty("vms.apiBaseUrl", "http://10.254.192.118");

    public static final String USERNAME = System.getProperty("vms.username", "b.amer@injaz-consulting.com");
    public static final String PASSWORD = System.getProperty("vms.password", "Welcome@1");

    public static final String BROWSER = System.getProperty("vms.browser", "edge");

    private TestConfig() {}
}

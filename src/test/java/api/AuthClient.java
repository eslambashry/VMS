package api;

import config.TestConfig;
import io.restassured.response.Response;
import objects.User;

import static io.restassured.RestAssured.given;

// Logs in directly against the backend auth API (bypassing the browser) to get a real access
// token, so other backend endpoints can be called directly with proper authorization - no need
// to intercept browser network traffic or guess at client-side storage keys.
public class AuthClient {

    public static String login(String username, String password){
        User user = new User(username, password);

        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .contentType("application/json")
                .body(user)
        .when()
                .post("api/v1/idm/auth/login")
        .then()
                .extract().response();

        return response.body().path("data.accessToken");
    }
}

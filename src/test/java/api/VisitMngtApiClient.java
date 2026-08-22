package api;

import config.EndPoint;
import config.TestConfig;
import io.restassured.response.Response;

import static io.restassured.RestAssured.given;

// Direct authenticated calls to the Visit-mngt backend API using a real access token
// (see AuthClient) - no need to intercept browser network traffic for data the API
// can just hand us directly.
public class VisitMngtApiClient {

    public static String getWorkingHours(String accessToken){
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get("api/v1/Visit-mngt/working-hours")
        .then()
                .extract().response();
        return response.body().asString();
    }

    public static String getVisitsType(String accessToken){
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .when()
                .get(EndPoint.VISIT_ZONE)
                .then()
                .extract().response();

        return response.body().asString();
    }

    public static String getVisitZones(String accessToken){
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get(EndPoint.VISIT_ZONE)
        .then()
                .extract().response();

        return response.body().asString();
    }

}

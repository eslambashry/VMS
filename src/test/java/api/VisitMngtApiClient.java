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
                .get(EndPoint.WORKING_HOURS)
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

    public static String getVisitors(String accessToken){
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("size", 10)
        .when()
                .get(EndPoint.VISITORS)
        .then()
                .extract().response();

        return response.body().asString();
    }

    public static String getVisitTypes(String accessToken){
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get(EndPoint.VISIT_TYPES)
        .then()
                .extract().response();

        return response.body().asString();
    }

    public static String getEmployees(String accessToken){
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get(EndPoint.EMPLOYEES)
        .then()
                .extract().response();

        return response.body().asString();
    }

    // requestBody is a Map (see VisitRequestBuilder) rather than a dedicated POJO - the payload
    // has a lot of fields the tests never vary (meeting room, host employee, ...), so a fixed
    // shape built once by the builder is simpler than a class with a matching constructor/setters.
    public static void createVisitRequest(String accessToken, Object requestBody){
        given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .contentType("application/json")
                .body(requestBody)
        .when()
                .post(EndPoint.CREATE_VISIT_REQUEST)
        .then()
                .statusCode(201);
    }

    public static String getVisitRequestDetails(String accessToken, String requestId){
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get(EndPoint.VISIT_REQUEST_DETAILS + requestId)
        .then()
                .extract().response();

        return response.body().asString();
    }

    public static String getSettings(String accessToken){
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
        .when()
                .get(EndPoint.SETTINGS)
        .then()
                .extract().response();

        return response.body().asString();
    }

    public static String getVisitRequests(String accessToken){
        Response response = given()
                .baseUri(TestConfig.API_BASE_URL)
                .header("Authorization", "Bearer " + accessToken)
                .queryParam("page", 0)
                .queryParam("size", 10)
        .when()
                .get(EndPoint.VISIT_REQUESTS)
        .then()
                .extract().response();

        return response.body().asString();
    }

}

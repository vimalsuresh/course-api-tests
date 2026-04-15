package com.enrollment.api.tests;

import com.enrollment.api.support.TestConfig;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Health Check")
public class HealthCheckTest extends TestConfig {

    @Test
    @DisplayName("GET /status - server responds with 200 and a status message")
    void serverIsRunning() {
        Response response = given().spec(spec)
                .get("/status")
                .then().extract().response();

        assertThat(response.statusCode())
                .as("Status endpoint should return 200")
                .isEqualTo(200);

        String status = response.jsonPath().getString("status");
        assertThat(status)
                .as("Response body should contain a status message")
                .isNotBlank();

        System.out.println("Server status: " + status);
    }
}
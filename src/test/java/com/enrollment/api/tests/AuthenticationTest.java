package com.enrollment.api.tests;

import com.enrollment.api.support.Auth;
import com.enrollment.api.support.TestConfig;
import io.restassured.response.Response;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Authentication Tests")
public class AuthenticationTest extends TestConfig {

    // --- Instructor ---

    @Test
    @DisplayName("Instructor login with valid credentials returns 200 and a token")
    void instructorLoginSuccess() {
        Response response = given().spec(spec)
                .body(Map.of("username", Auth.INSTRUCTOR, "password", Auth.PASSWORD))
                .post("/instructor/login")
                .then().extract().response();

        assertThat(response.statusCode()).isEqualTo(200);

        String token = response.jsonPath().getString("token");
        assertThat(token)
                .as("Instructor login should return a non-empty JWT token")
                .isNotBlank();
    }

    @Test
    @DisplayName("Instructor login with wrong password returns 401")
    void instructorLoginWrongPassword() {
        int status = Auth.attemptLogin("/instructor/login", Auth.INSTRUCTOR, "wrongpassword");
        assertThat(status)
                .as("Wrong password should return 401")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("Instructor login with unknown username returns 401")
    void instructorLoginUnknownUser() {
        int status = Auth.attemptLogin("/instructor/login", "instructor_unknown999", Auth.PASSWORD);
        assertThat(status)
                .as("Unknown instructor username should return 401")
                .isEqualTo(401);
    }

    // --- Student ---

    @Test
    @DisplayName("Student login with valid credentials returns 200 and a token")
    void studentLoginSuccess() {
        Response response = given().spec(spec)
                .body(Map.of("username", Auth.STUDENT, "password", Auth.PASSWORD))
                .post("/student/login")
                .then().extract().response();

        assertThat(response.statusCode()).isEqualTo(200);

        String token = response.jsonPath().getString("token");
        assertThat(token)
                .as("Student login should return a non-empty JWT token")
                .isNotBlank();
    }

    @Test
    @DisplayName("Student login with wrong password returns 401")
    void studentLoginWrongPassword() {
        int status = Auth.attemptLogin("/student/login", Auth.STUDENT, "wrongpassword");
        assertThat(status)
                .as("Wrong password should return 401")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("Student login with unknown username returns 401")
    void studentLoginUnknownUser() {
        int status = Auth.attemptLogin("/student/login", "student_unknown999", Auth.PASSWORD);
        assertThat(status)
                .as("Unknown student username should return 401")
                .isEqualTo(401);
    }

    @Test
    @DisplayName("Token is different on each login")
    void eachLoginProducesANewToken() {
        String token1 = Auth.studentToken();
        sleep(1000);
        String token2 = Auth.studentToken();

        assertThat(token1)
                .as("Each login should produce a unique token")
                .isNotEqualTo(token2);
    }
}
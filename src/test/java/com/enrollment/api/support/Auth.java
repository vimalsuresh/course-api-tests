package com.enrollment.api.support;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;

import java.util.Map;

public class Auth {

    public static final String INSTRUCTOR = Config.get("instructor.username");
    public static final String STUDENT    = Config.get("student.username");
    public static final String PASSWORD   = Config.get("password");

    public static String instructorToken() {
        return getToken("/instructor/login", INSTRUCTOR, PASSWORD);
    }

    public static String studentToken() {
        return getToken("/student/login", STUDENT, PASSWORD);
    }

    public static String getToken(String endpoint, String username, String password) {
        return RestAssured.given()
                .baseUri(TestConfig.BASE_URL)
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post(endpoint)
                .then()
                .statusCode(200)
                .extract()
                .jsonPath()
                .getString("token");
    }

    public static int attemptLogin(String endpoint, String username, String password) {
        return RestAssured.given()
                .baseUri(TestConfig.BASE_URL)
                .contentType(ContentType.JSON)
                .body(Map.of("username", username, "password", password))
                .post(endpoint)
                .then()
                .extract()
                .statusCode();
    }
}
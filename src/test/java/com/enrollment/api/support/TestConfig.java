package com.enrollment.api.support;

import io.restassured.RestAssured;
import io.restassured.builder.RequestSpecBuilder;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import io.restassured.specification.RequestSpecification;
import org.junit.jupiter.api.BeforeAll;

public abstract class TestConfig {

    public static final String BASE_URL = Config.get("base.url");
    private static final boolean VERBOSE_LOGGING = false;
    protected static RequestSpecification spec;

    @BeforeAll
    static void setup() {
        RequestSpecBuilder builder = new RequestSpecBuilder()
                .setBaseUri(BASE_URL)
                .setContentType(ContentType.JSON)
                .setAccept(ContentType.JSON);
        if (VERBOSE_LOGGING) {
            builder.addFilter(new RequestLoggingFilter())
                   .addFilter(new ResponseLoggingFilter());
        }
        spec = builder.build();
        waitForServer();
    }

    private static void waitForServer() {
        System.out.println("Waiting for server to be ready...");
        for (int i = 1; i <= 5; i++) {
            try {
                int status = RestAssured.given()
                        .baseUri(BASE_URL)
                        .get("/status")
                        .statusCode();
                if (status == 200) {
                    System.out.println("Server is up.");
                    return;
                }
            } catch (Exception e) {
                System.out.println("Attempt " + i + " - not ready yet, retrying in 15s");
            }
            sleep(15000);
        }
        System.out.println("Server did not respond in time, proceeding anyway.");
    }

    protected static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
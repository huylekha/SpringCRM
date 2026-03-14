package com.company.platform.performance;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;

/**
 * Basic load test simulation for CRM platform. Tests health endpoints and basic CRUD operations.
 *
 * <p>Run with: mvn gatling:test
 */
public class BasicLoadSimulation extends Simulation {

  // Configuration
  private static final String BASE_URL = System.getProperty("BASE_URL", "http://localhost:8080");
  private static final int USERS = Integer.parseInt(System.getProperty("USERS", "10"));
  private static final int RAMP_DURATION =
      Integer.parseInt(System.getProperty("RAMP_DURATION", "30"));

  // HTTP Protocol Configuration
  HttpProtocolBuilder httpProtocol =
      http.baseUrl(BASE_URL)
          .acceptHeader("application/json")
          .contentTypeHeader("application/json")
          .userAgentHeader("Gatling Load Test");

  // Scenarios
  ScenarioBuilder healthCheckScenario =
      scenario("Health Check")
          .exec(http("Health Check").get("/actuator/health").check(status().is(200)));

  ScenarioBuilder customerListScenario =
      scenario("Customer List")
          .exec(
              http("List Customers")
                  .get("/api/customers?page=0&size=20")
                  .check(status().is(200))
                  .check(jsonPath("$.content").exists()));

  ScenarioBuilder customerDetailsScenario =
      scenario("Customer Details")
          .exec(
              http("Get Customer Details")
                  .get("/api/customers/#{customerId}")
                  .check(status().in(200, 404)));

  // Load Profile
  {
    setUp(
            healthCheckScenario.injectOpen(constantUsersPerSec(5).during(Duration.ofSeconds(30))),
            customerListScenario.injectOpen(
                rampUsersPerSec(1).to(USERS).during(Duration.ofSeconds(RAMP_DURATION))),
            customerDetailsScenario.injectOpen(
                rampUsersPerSec(1).to(USERS / 2).during(Duration.ofSeconds(RAMP_DURATION))))
        .protocols(httpProtocol)
        .assertions(
            global().responseTime().max().lt(2000),
            global().successfulRequests().percent().gt(95.0),
            forAll().failedRequests().percent().lt(5.0));
  }
}

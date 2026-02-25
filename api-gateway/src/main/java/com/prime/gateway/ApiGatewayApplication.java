package com.prime.gateway;

 import org.springframework.boot.SpringApplication;
 import org.springframework.boot.autoconfigure.SpringBootApplication;
 import org.springframework.context.annotation.ComponentScan;
 import org.springframework.context.annotation.FilterType;

 /**
  * API Gateway Application.
  * Single entry point for all microservices.
  *
  * Responsibilities:
  * - Request routing to downstream services
  * - Authentication enforcement (JWT validation)
  * - Rate limiting
  * - CORS handling
  * - Request/Response logging
  * - Circuit breaker patterns
  */
 @SpringBootApplication
 @ComponentScan(
         basePackages = {"com.prime.gateway", "com.prime.common"},
         excludeFilters = {
                 @ComponentScan.Filter(type = FilterType.REGEX,
                         pattern = "com\\.prime\\.common\\.exception\\.GlobalExceptionHandler")
         }
 )
 public class ApiGatewayApplication {

     public static void main(String[] args) {
         System.setProperty("spring.main.web-application-type", "reactive");
         SpringApplication.run(ApiGatewayApplication.class, args);
     }
 }
package com.company.platform.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.company.platform.auth", "com.company.platform.shared"})
@EntityScan(basePackages = {"com.company.platform.auth", "com.company.platform.shared"})
@EnableJpaRepositories(basePackages = {"com.company.platform.auth", "com.company.platform.shared"})
public class AuthServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(AuthServiceApplication.class, args);
  }
}

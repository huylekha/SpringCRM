package com.company.platform.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@ComponentScan(basePackages = {"com.company.platform.crm", "com.company.platform.shared"})
@EntityScan(basePackages = {"com.company.platform.crm", "com.company.platform.shared"})
@EnableJpaRepositories(basePackages = {"com.company.platform.crm", "com.company.platform.shared"})
public class CrmServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CrmServiceApplication.class, args);
  }
}

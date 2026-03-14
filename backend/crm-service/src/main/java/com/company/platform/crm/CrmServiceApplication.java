package com.company.platform.crm;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {"com.company.platform.crm", "com.company.platform.shared"})
public class CrmServiceApplication {

  public static void main(String[] args) {
    SpringApplication.run(CrmServiceApplication.class, args);
  }
}

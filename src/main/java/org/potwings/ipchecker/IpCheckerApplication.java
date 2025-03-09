package org.potwings.ipchecker;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class IpCheckerApplication implements CommandLineRunner {

  public static void main(String[] args) {
    SpringApplication.run(IpCheckerApplication.class, args);
  }

  @Override
  public void run(String... args) throws Exception {
  }
}

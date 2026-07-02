package com.acorn.abc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class AbcApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbcApplication.class, args);
    }

}

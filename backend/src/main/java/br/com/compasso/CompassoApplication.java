package br.com.compasso;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class CompassoApplication {

    public static void main(String[] args) {
        SpringApplication.run(CompassoApplication.class, args);
    }
}

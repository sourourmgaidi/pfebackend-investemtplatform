package tn.iset.investplatformpfe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class InvestPlatformPfeApplication {

    public static void main(String[] args) {
        SpringApplication.run(InvestPlatformPfeApplication.class, args);
    }

}

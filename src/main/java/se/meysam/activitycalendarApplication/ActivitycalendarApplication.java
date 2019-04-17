package se.meysam.activitycalendarApplication;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ActivitycalendarApplication {

    public static void main(String[] args) {
        SpringApplication.run(ActivitycalendarApplication.class, args);
        System.out.println("\n Please open: \nhttp://localhost:8080/");
    }

}

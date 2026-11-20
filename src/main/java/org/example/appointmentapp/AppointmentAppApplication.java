package org.example.appointmentapp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AppointmentAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentAppApplication.class, args);
    }

}

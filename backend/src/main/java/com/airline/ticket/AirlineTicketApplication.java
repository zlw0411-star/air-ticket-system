package com.airline.ticket;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@MapperScan("com.airline.ticket")
public class AirlineTicketApplication {
    public static void main(String[] args) {
        SpringApplication.run(AirlineTicketApplication.class, args);
    }
}

package com.xrfriends.leadership;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.cluster.leader.event.LeaderEventPublisherConfiguration;
import org.springframework.context.annotation.Import;

@SpringBootApplication
public class LeadershipApplication {

    public static void main(String[] args) {
        SpringApplication.run(LeadershipApplication.class, args);
    }

}

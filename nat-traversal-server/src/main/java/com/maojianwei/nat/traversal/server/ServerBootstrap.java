package com.maojianwei.nat.traversal.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * NAT Traversal Server.
 *
 * @author Jianwei Mao
 */
@SpringBootApplication
public class ServerBootstrap {
    public static void main(String[] args) {
        SpringApplication.run(ServerBootstrap.class, args);
    }
}

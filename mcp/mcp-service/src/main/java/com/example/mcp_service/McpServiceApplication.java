package com.example.mcp_service;

import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@SpringBootApplication
public class McpServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(McpServiceApplication.class, args);
    }

}

@Service
class SchedulerTool {

    @McpTool(description = "schedule an appointment to schedule a pick up or adoption for Pooch Palace")
    Instant schedule(@McpToolParam int dogId) {
        var i = Instant.now().plus(3, ChronoUnit.DAYS);
        IO.println("scheduling " + dogId + "/" + i + " for " + SecurityContextHolder
                .getContextHolderStrategy()
                .getContext()
                .getAuthentication()
                .getName());
        return i;
    }
}
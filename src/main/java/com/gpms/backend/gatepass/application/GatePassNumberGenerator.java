package com.gpms.backend.gatepass.application;

import java.time.Year;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class GatePassNumberGenerator {

    private final JdbcTemplate jdbcTemplate;

    public GatePassNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextRequestNumber() {
        Long sequence = jdbcTemplate.queryForObject("select nextval('request_number_seq')", Long.class);
        return formatRequestNumber(sequence);
    }

    public String nextGatePassNumber() {
        Long sequence = jdbcTemplate.queryForObject("select nextval('gate_pass_number_seq')", Long.class);
        return formatGatePassNumber(sequence);
    }

    public static String formatRequestNumber(Long sequence) {
        return "REQ-" + Year.now().getValue() + "-" + String.format("%06d", sequence);
    }

    public static String formatGatePassNumber(Long sequence) {
        return "GP-" + Year.now().getValue() + "-" + String.format("%06d", sequence);
    }
}

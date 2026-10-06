package com.gpms.backend.gatepass.application;

import com.gpms.backend.company.domain.Company;
import java.time.Year;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Mints request and gate pass numbers, counted per company.
 *
 * One global sequence used to serve everybody, which on a shared
 * deployment meant a new client's first pass was numbered GP-2026-000457
 * - carrying on somebody else's count, and telling them roughly how
 * many passes that somebody issues. Each company now counts for itself,
 * behind its own short code:
 *
 *     ACME-GP-2026-000001
 *     ACME-REQ-2026-000001
 *
 * The counter resets with the year, because the year is in the number.
 */
@Component
public class GatePassNumberGenerator {

    private static final String REQUEST = "REQUEST";
    private static final String GATE_PASS = "GATE_PASS";

    private final JdbcTemplate jdbcTemplate;

    public GatePassNumberGenerator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public String nextRequestNumber(Company company) {
        return format(company, "REQ", next(company.getId(), REQUEST));
    }

    public String nextGatePassNumber(Company company) {
        return format(company, "GP", next(company.getId(), GATE_PASS));
    }

    /*
     * One statement, and atomic on its own: the insert either creates
     * this year's counter at 1 or bumps the existing one, and returns
     * whichever value it settled on. Two requests arriving together are
     * serialised by the primary key rather than by a lock we have to
     * remember to take.
     */
    private long next(UUID companyId, String kind) {

        Long value = jdbcTemplate.queryForObject("""
                insert into company_document_counters (company_id, kind, year, last_value)
                values (?, ?, ?, 1)
                on conflict (company_id, kind, year)
                do update set last_value = company_document_counters.last_value + 1
                returning last_value
                """,
                Long.class,
                companyId,
                kind,
                Year.now().getValue());

        return value == null ? 1L : value;
    }

    /**
     * The number without a company in front of it: REQ-2026-000145.
     *
     * Kept as a static, and kept separate from the prefix, because this
     * is the shape of every number issued before companies existed -
     * documents already printed and filed under it.
     */
    public static String formatRequestNumber(long sequence) {
        return bare("REQ", sequence);
    }

    public static String formatGatePassNumber(long sequence) {
        return bare("GP", sequence);
    }

    private static String bare(String document, long sequence) {
        return document + "-"
                + Year.now().getValue() + "-"
                + String.format("%06d", sequence);
    }

    private String format(Company company, String document, long sequence) {
        return prefixOf(company) + bare(document, sequence);
    }

    /*
     * The company's code, or nothing if it somehow has none - a number
     * without a prefix is still a usable number, where refusing to mint
     * one would stop the truck.
     */
    private String prefixOf(Company company) {
        String code = company == null ? null : company.getCode();
        return code == null || code.isBlank() ? "" : code.trim().toUpperCase() + "-";
    }
}

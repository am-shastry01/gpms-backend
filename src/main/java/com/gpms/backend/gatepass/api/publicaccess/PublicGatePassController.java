package com.gpms.backend.gatepass.api.publicaccess;

import com.gpms.backend.gatepass.application.GatePassRequestService;
import com.gpms.backend.gatepass.application.QrCodeService;
import com.gpms.backend.gatepass.domain.GatePassRequest;
import java.nio.charset.StandardCharsets;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * What the truck driver opens from the SMS link.
 *
 * Deliberately outside /api/v1 and unauthenticated: the driver has no
 * account. The random token in the URL is the only credential, which
 * is why it expires and can be consumed once.
 *
 * HTML rather than JSON, because the destination is a phone browser.
 */
@RestController
@RequestMapping("/public/gate-pass")
public class PublicGatePassController {

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
                    .withZone(ZoneOffset.UTC);

    private final GatePassRequestService gatePassRequestService;
    private final QrCodeService qrCodeService;

    public PublicGatePassController(
            GatePassRequestService gatePassRequestService,
            QrCodeService qrCodeService
    ) {
        this.gatePassRequestService = gatePassRequestService;
        this.qrCodeService = qrCodeService;
    }

    @GetMapping(value = "/{token}", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> page(@PathVariable String token) {

        GatePassRequest pass =
                gatePassRequestService.findByAccessTokenForPublicPage(token);

        if (pass == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .contentType(MediaType.TEXT_HTML)
                    .body(errorPage());
        }

        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(org.springframework.http.CacheControl.noStore())
                .body(passPage(pass, token));
    }

    /**
     * The QR itself, embedded by the page above.
     *
     * It encodes "GPMS:&lt;token&gt;" so the guard's scanner can tell a
     * driver pass apart from any other QR it might be pointed at.
     */
    @GetMapping(value = "/{token}/qr.png", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qr(@PathVariable String token) {

        GatePassRequest pass =
                gatePassRequestService.findByAccessTokenForPublicPage(token);

        if (pass == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] png = qrCodeService.generatePng("GPMS:" + token);

        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .contentType(MediaType.IMAGE_PNG)
                .body(png);
    }

    private String passPage(GatePassRequest pass, String token) {

        String vehicle = pass.getVehicle() == null
                ? "-" : pass.getVehicle().getRegistrationNumber();

        String driver = pass.getDriver() == null
                ? "-" : pass.getDriver().getName();

        String vendor = pass.getVendor() == null
                ? "-" : pass.getVendor().getName();

        String warehouse = pass.getWarehouse() == null
                ? "-" : pass.getWarehouse().getName();

        String expiry = pass.getTokenExpiresAt() == null
                ? "-" : DISPLAY_FORMAT.format(pass.getTokenExpiresAt()) + " UTC";

        boolean used = pass.getTokenConsumedAt() != null;

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Gate Pass %s</title>
                  <style>
                    *{box-sizing:border-box}
                    body{margin:0;padding:24px 16px;background:#f8fafc;
                         font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",
                         Roboto,Helvetica,Arial,sans-serif;color:#0f172a}
                    .card{max-width:420px;margin:0 auto;background:#fff;
                          border-radius:16px;padding:24px;
                          box-shadow:0 1px 3px rgba(15,23,42,.08)}
                    h1{margin:0 0 4px;font-size:20px}
                    .sub{margin:0 0 20px;color:#64748b;font-size:14px}
                    .qr{display:block;margin:0 auto;width:260px;height:260px;
                        background:#fff;border-radius:12px}
                    .num{text-align:center;font-size:22px;font-weight:700;
                         letter-spacing:1px;margin:16px 0 4px}
                    .hint{text-align:center;color:#64748b;font-size:13px;
                          margin:0 0 20px}
                    dl{margin:0;border-top:1px solid #e2e8f0;padding-top:16px}
                    .row{display:flex;justify-content:space-between;
                         gap:16px;padding:8px 0}
                    dt{color:#64748b;font-size:13px;margin:0}
                    dd{margin:0;font-size:14px;font-weight:600;text-align:right}
                    .used{margin:16px 0 0;padding:12px;border-radius:12px;
                          background:#fee2e2;color:#991b1b;font-size:13px;
                          text-align:center}
                    .foot{max-width:420px;margin:16px auto 0;text-align:center;
                          color:#94a3b8;font-size:12px}
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>Gate Pass</h1>
                    <p class="sub">Show this screen to the security guard.</p>
                    <img class="qr" src="/public/gate-pass/%s/qr.png"
                         alt="Gate pass QR code">
                    <p class="num">%s</p>
                    <p class="hint">The guard will scan this code.</p>
                    <dl>
                      <div class="row"><dt>Vehicle</dt><dd>%s</dd></div>
                      <div class="row"><dt>Driver</dt><dd>%s</dd></div>
                      <div class="row"><dt>Vendor</dt><dd>%s</dd></div>
                      <div class="row"><dt>Warehouse</dt><dd>%s</dd></div>
                      <div class="row"><dt>Valid until</dt><dd>%s</dd></div>
                    </dl>
                    %s
                  </div>
                  <p class="foot">Do not share this link.</p>
                </body>
                </html>
                """.formatted(
                escape(pass.getGatePassNumber()),
                escape(token),
                escape(pass.getGatePassNumber()),
                escape(vehicle),
                escape(driver),
                escape(vendor),
                escape(warehouse),
                escape(expiry),
                used
                        ? "<p class=\"used\">This pass has already been "
                        + "scanned at the gate.</p>"
                        : ""
        );
    }

    private String errorPage() {

        return """
                <!doctype html>
                <html lang="en">
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1">
                  <title>Gate Pass unavailable</title>
                  <style>
                    body{margin:0;padding:48px 16px;background:#f8fafc;
                         font-family:-apple-system,BlinkMacSystemFont,"Segoe UI",
                         Roboto,Helvetica,Arial,sans-serif;color:#0f172a;
                         text-align:center}
                    .card{max-width:420px;margin:0 auto;background:#fff;
                          border-radius:16px;padding:32px 24px;
                          box-shadow:0 1px 3px rgba(15,23,42,.08)}
                    h1{font-size:18px;margin:0 0 8px}
                    p{color:#64748b;font-size:14px;margin:0}
                  </style>
                </head>
                <body>
                  <div class="card">
                    <h1>This pass link is not valid</h1>
                    <p>It may have expired, or the pass may have been
                       cancelled. Please contact the warehouse for a new one.</p>
                  </div>
                </body>
                </html>
                """;
    }

    /* The values are ours, but a vendor or driver name is free text. */
    private String escape(String value) {

        if (value == null) {
            return "-";
        }

        return new String(value.getBytes(StandardCharsets.UTF_8),
                StandardCharsets.UTF_8)
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}

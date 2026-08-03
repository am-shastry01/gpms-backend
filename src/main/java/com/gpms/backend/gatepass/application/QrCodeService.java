package com.gpms.backend.gatepass.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.gpms.backend.common.exception.BusinessException;
import com.gpms.backend.gatepass.domain.GatePassRequest;
import java.io.ByteArrayOutputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Service;

@Service
public class QrCodeService {

    private final ObjectMapper objectMapper;

    public QrCodeService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String buildQrContent(GatePassRequest gatePassRequest) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("gatePassId", gatePassRequest.getId());
        payload.put("gatePassNumber", gatePassRequest.getGatePassNumber());
        payload.put("truckNumber", gatePassRequest.getVehicle().getRegistrationNumber());
        payload.put("vendor", gatePassRequest.getVendor().getName());
        payload.put("status", gatePassRequest.getStatus().name());
        payload.put("warehouse", gatePassRequest.getWarehouse().getCode());
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            throw new BusinessException("Failed to build QR payload");
        }
    }

    public byte[] generatePng(String qrContent) {
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(qrContent, BarcodeFormat.QR_CODE, 280, 280);
            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
                MatrixToImageWriter.writeToStream(matrix, "PNG", outputStream);
                return outputStream.toByteArray();
            }
        } catch (Exception exception) {
            throw new BusinessException("Failed to generate QR code");
        }
    }
}

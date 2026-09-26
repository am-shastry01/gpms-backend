package com.gpms.backend.transport.application;

import com.gpms.backend.transport.api.dto.TransportResponse;
import com.gpms.backend.transport.domain.Transport;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface TransportMapper {

    TransportResponse toResponse(Transport transport);
}

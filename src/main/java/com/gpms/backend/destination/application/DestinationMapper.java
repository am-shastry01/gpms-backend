package com.gpms.backend.destination.application;

import com.gpms.backend.destination.api.dto.DestinationResponse;
import com.gpms.backend.destination.domain.Destination;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DestinationMapper {

    DestinationResponse toResponse(Destination destination);
}

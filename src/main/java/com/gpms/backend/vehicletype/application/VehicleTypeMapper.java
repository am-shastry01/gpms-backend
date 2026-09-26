package com.gpms.backend.vehicletype.application;

import com.gpms.backend.vehicletype.api.dto.VehicleTypeResponse;
import com.gpms.backend.vehicletype.domain.VehicleType;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VehicleTypeMapper {

    VehicleTypeResponse toResponse(VehicleType vehicleType);
}

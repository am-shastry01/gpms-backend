package com.gpms.backend.vehicle.application;

import com.gpms.backend.vehicle.api.dto.VehicleResponse;
import com.gpms.backend.vehicle.domain.Vehicle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface VehicleMapper {

    @Mapping(target = "vendorId", expression = "java(vehicle.getVendor() != null ? vehicle.getVendor().getId() : null)")
    @Mapping(target = "vendorName", expression = "java(vehicle.getVendor() != null ? vehicle.getVendor().getName() : null)")
    VehicleResponse toResponse(Vehicle vehicle);
}

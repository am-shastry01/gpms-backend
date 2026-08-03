package com.gpms.backend.warehouse.application;

import com.gpms.backend.warehouse.api.dto.WarehouseResponse;
import com.gpms.backend.warehouse.domain.Warehouse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface WarehouseMapper {

    WarehouseResponse toResponse(Warehouse warehouse);
}

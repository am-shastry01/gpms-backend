package com.gpms.backend.driver.application;

import com.gpms.backend.driver.api.dto.DriverResponse;
import com.gpms.backend.driver.domain.Driver;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface DriverMapper {

    DriverResponse toResponse(Driver driver);
}

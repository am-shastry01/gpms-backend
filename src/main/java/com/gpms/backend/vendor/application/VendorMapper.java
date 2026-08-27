package com.gpms.backend.vendor.application;

import com.gpms.backend.vendor.api.dto.VendorResponse;
import com.gpms.backend.vendor.domain.Vendor;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface VendorMapper {

    VendorResponse toResponse(Vendor vendor);
}

package com.gpms.backend.notification.application;

import com.gpms.backend.notification.api.dto.NotificationResponse;
import com.gpms.backend.notification.domain.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    @Mapping(target = "gatePassRequestId", expression = "java(notification.getGatePassRequest() != null ? notification.getGatePassRequest().getId() : null)")
    NotificationResponse toResponse(Notification notification);
}

package com.gpms.backend.user.api.dto;

/*
 * The outcome of removing a person.
 *
 * Not a bare 204, because there are two honest outcomes and the admin
 * needs to be told which one happened: the row was deleted outright,
 * or it had to stay because that person's name and signature appear on
 * gate passes that have already been issued.
 */
public record UserDeleteResponse(
        boolean removed,
        String message
) {
}

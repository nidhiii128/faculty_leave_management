package com.flms.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class LeaveNotFoundException extends RuntimeException {
    public LeaveNotFoundException(Long id) {
        super("Leave request " + id + " not found");
    }
}

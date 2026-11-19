package com.dt2.techrepair.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class Http {
    private Http() {}
    static ResponseStatusException bad(String msg) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, msg); }
    static ResponseStatusException conflict(String msg) { return new ResponseStatusException(HttpStatus.CONFLICT, msg); }
    static ResponseStatusException notFound() { return new ResponseStatusException(HttpStatus.NOT_FOUND, "Registro não encontrado."); }
}

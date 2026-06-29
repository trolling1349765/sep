package fpt.capstone.service;

import fpt.capstone.dto.request.AuthenticationRequest;
import fpt.capstone.dto.request.IntrospectRequest;
import fpt.capstone.dto.response.AuthenticationResponse;
import fpt.capstone.dto.response.IntrospectResponse;

public interface AuthenticationService {
    AuthenticationResponse authenticate(AuthenticationRequest request) throws Throwable;

    IntrospectResponse introspect(IntrospectRequest request);
}

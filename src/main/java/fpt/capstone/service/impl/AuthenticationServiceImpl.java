package fpt.capstone.service.impl;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import fpt.capstone.dto.request.AuthenticationRequest;
import fpt.capstone.dto.request.IntrospectRequest;
import fpt.capstone.dto.response.APIResponse;
import fpt.capstone.dto.response.AuthenticationResponse;
import fpt.capstone.dto.response.IntrospectResponse;
import fpt.capstone.exceprion.AppException;
import fpt.capstone.exceprion.enums.ErrorCode;
import fpt.capstone.repository.UserRepository;
import fpt.capstone.service.AuthenticationService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuthenticationServiceImpl implements AuthenticationService {

    UserRepository userRepository;

    @NonFinal//-> ko inject vao contructor
            @Value("${jwt.signerKey}")
    String SIGNER_KEY;

    @Override
    public AuthenticationResponse authenticate(AuthenticationRequest request) throws Throwable {
        var user = userRepository.findByUsername(request.getUsername()).orElseThrow(
                () -> {
                    List<APIResponse>  responses = new ArrayList<>();
                    APIResponse  response = new APIResponse();

                    response.setCode(ErrorCode.USERNAME_NOT_EXISTED.getCode());
                    response.setMessage(ErrorCode.USERNAME_NOT_EXISTED.getMessage());
                    response.setData(request.getUsername());

                    responses.add(response);

                    return new AppException(responses);
                }
        );

        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());
        if (!authenticated) {

            APIResponse  response = APIResponse.builder()
                    .code(ErrorCode.UNAUTHENTICATED.getCode())
                    .message(ErrorCode.UNAUTHENTICATED.getMessage())
                    .data(request.getUsername())
                    .build();
            List<APIResponse>  responses = new ArrayList<>();
            responses.add(response);
            throw new AppException(responses);
        }

        String token = generateToken(user.getId());

        return AuthenticationResponse.builder()
                .token(token)
                .build();
    }

    @Override
    public IntrospectResponse introspect(IntrospectRequest request) {
        String token = request.getToken();
        try {
            JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
            SignedJWT signedJWT = SignedJWT.parse(token);
            Date expireDate = signedJWT.getJWTClaimsSet().getExpirationTime();
            var verified = signedJWT.verify(verifier);
            return IntrospectResponse.builder()
                    .isValid(verified && expireDate.after(new Date()))
                    .build();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

    }

    private String generateToken(String userId) {

        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS256);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(userId)
                .issuer("trolling1349765")
                .issueTime(new Date())
                .expirationTime(new Date(
                        Instant.now().plus(1, ChronoUnit.HOURS).toEpochMilli()
                ))
                .claim("creator", "trolling1349765")
                .build();
        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            throw new RuntimeException(e);
        }
    }
}

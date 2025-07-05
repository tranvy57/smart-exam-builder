package iuh.fit.smartexambuilderbe.services;

import com.nimbusds.jose.*;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jose.crypto.MACVerifier;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import iuh.fit.smartexambuilderbe.dtos.requests.AuthenticationRequest;
import iuh.fit.smartexambuilderbe.dtos.requests.IntrospectRequest;
import iuh.fit.smartexambuilderbe.dtos.requests.LogoutRequest;
import iuh.fit.smartexambuilderbe.dtos.requests.RefreshRequest;
import iuh.fit.smartexambuilderbe.dtos.responses.AuthenticationResponse;
import iuh.fit.smartexambuilderbe.dtos.responses.IntrospectResponse;
import iuh.fit.smartexambuilderbe.entities.InvalidatedToken;
import iuh.fit.smartexambuilderbe.entities.User;
import iuh.fit.smartexambuilderbe.exceptions.UnauthorizedException;
import iuh.fit.smartexambuilderbe.mappers.UserMapper;
import iuh.fit.smartexambuilderbe.repositories.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.auth.AUTH;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationService {
    UserRepository userRepository;
    UserMapper userMapper;
    RedisService redisService;

    @NonFinal
    @Value("${jwt.signerKey}")
    protected String SIGNER_KEY;

    @NonFinal
    @Value("${jwt.valid-duration}")
    protected long VALID_DURATION;

    @NonFinal
    @Value("${jwt.refreshable-duration}")
    protected long REFRESHABLE_DURATION;

    public IntrospectResponse introspect(IntrospectRequest request) throws ParseException {
        var token = request.getToken();

        boolean isValid = true;

        log.info("Token: " + token);

        try {
            verifyToken(token, false);
        } catch (Exception e) {
            isValid = false;
        }

        return IntrospectResponse.builder()
                .userId(isValid ? SignedJWT.parse(token).getJWTClaimsSet().getSubject() : null)
                .valid(isValid)
                .build();
    }

    public void logout(LogoutRequest request) throws ParseException, JOSEException {
        try {
            var signToken = verifyToken(request.getToken(), true);

            String jit = signToken.getJWTClaimsSet().getJWTID();

            redisService.saveInvalidatedToken(jit, request.getToken());
        } catch (UnauthorizedException exception) {

            log.info("Token already expired");
        }
    }

    public AuthenticationResponse refreshToken(RefreshRequest request) throws ParseException, JOSEException {
        var signedJWT = verifyToken(request.getToken(), true);

        var jit = signedJWT.getJWTClaimsSet().getJWTID();

        redisService.saveInvalidatedToken(jit, request.getToken());

        var username = signedJWT.getJWTClaimsSet().getSubject();

        var user = userRepository.findUserByUsername(username)
                .orElseThrow(() -> new UnauthorizedException("Người dùng không tồn tại"));

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(token)
                .user(userMapper.toUserResponse(user))
                .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        PasswordEncoder passwordEncoder = new BCryptPasswordEncoder(10);
        var user = userRepository.findUserByUsername(request.getUsername())
                .orElseThrow(() -> new UnauthorizedException("Sai tên đăng nhập hoặc mật khẩu"));

        boolean authenticated = passwordEncoder.matches(request.getPassword(), user.getPassword());

        if (!authenticated) {
            throw new UnauthorizedException("Sai tên đăng nhập hoặc mật khẩu");
        }

        var token = generateToken(user);

        return AuthenticationResponse.builder()
                .authenticated(true)
                .token(token)
                .user(userMapper.toUserResponse(user))
                .build();
    }

    String generateToken(User user){
        JWSHeader jwsHeader = new JWSHeader(JWSAlgorithm.HS512);

        JWTClaimsSet jwtClaimsSet = new JWTClaimsSet.Builder()
                .subject(user.getUsername())
                .issuer("SmartExamBuilder")
                .issueTime(new Date())
                .expirationTime(new Date(Instant.now().plus(VALID_DURATION, ChronoUnit.SECONDS).toEpochMilli()))
                .jwtID(UUID.randomUUID().toString())
                .claim("scope", user.getRole())
                .build();

        Payload payload = new Payload(jwtClaimsSet.toJSONObject());

        JWSObject jwsObject = new JWSObject(jwsHeader, payload);

        try {
            jwsObject.sign(new MACSigner(SIGNER_KEY.getBytes()));
            return jwsObject.serialize();
        } catch (JOSEException e) {
            log.error("Cannot create token", e);
            throw new RuntimeException(e);
        }
    }

    SignedJWT verifyToken(String token, boolean isRefresh) throws JOSEException, ParseException {
        Objects.requireNonNull(token, "Token không được null");
        Objects.requireNonNull(SIGNER_KEY, "SIGNER_KEY không được null");

        JWSVerifier verifier = new MACVerifier(SIGNER_KEY.getBytes());
        SignedJWT signedJWT = SignedJWT.parse(token);

        if(!signedJWT.verify(verifier)) {
            throw new UnauthorizedException("Token không hợp lệ");
        }

        Date expiryTime;
        var claims = signedJWT.getJWTClaimsSet();

        if(isRefresh){
            Date issueTime = claims.getIssueTime();
            if(issueTime == null) {
                throw new UnauthorizedException("Thiếu Issue Time trong refresh token");
            }
            expiryTime = new Date(issueTime.toInstant().plus(REFRESHABLE_DURATION, ChronoUnit.SECONDS).toEpochMilli());
        } else {
            expiryTime = claims.getExpirationTime();
        }

        if(expiryTime == null || expiryTime.before(new Date())) {
            throw new UnauthorizedException("Token đã hết hạn");
        }
        if(redisService.isTokenInvalidated(claims.getJWTID())) {
            throw new UnauthorizedException("Token đã bị vô hiệu hóa");
        }

        return signedJWT;
    }

//    private String buildScope(User user) {
//        if (user.getRole() == null) {
//            return "";
//        }
//
//        return "ROLE_" + user.getRole().name();
//    }
}

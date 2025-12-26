package com.game.contraband.infrastructure.jwt;

import com.game.contraband.domain.auth.TokenEncoder;
import com.game.contraband.domain.auth.TokenType;
import com.game.contraband.global.auth.TokenProperties;
import com.nimbusds.jose.EncryptionMethod;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWEAlgorithm;
import com.nimbusds.jose.JWEEncrypter;
import com.nimbusds.jose.JWEHeader;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSHeader.Builder;
import com.nimbusds.jose.KeyLengthException;
import com.nimbusds.jose.Payload;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class JwtEncoder implements TokenEncoder {

    private static final String CLAIM_ID = "id";
    private static final String TOKEN_CONTENT_TYPE = "JWT";

    private final JWEEncrypter jweEncrypter;
    private final JwsSignerFinder jwsSignerFinder;
    private final TokenProperties tokenProperties;

    @Override
    public String encode(LocalDateTime publishTime, TokenType tokenType, Long memberId) {
        try {
            return serializeToken(publishTime, tokenType, memberId);
        } catch (KeyLengthException e) {
            throw new FailedEncodeTokenException("키 길이를 지원하지 않는 환경입니다.", e);
        } catch (JOSEException e) {
            throw new FailedEncodeTokenException("토큰 인코딩 작업 중 문제가 발생했습니다.", e);
        }
    }

    private String serializeToken(LocalDateTime publishTime, TokenType tokenType, Long memberId) throws JOSEException {
        JWEHeader header = createJweHeader();
        JWTClaimsSet claims = createJwtPayload(tokenType, memberId, publishTime);
        SignedJWT signedJwt = setupSignedJwt(claims, tokenType);
        JWEObject jweObject = setupJweObject(header, signedJwt);

        return jweObject.serialize();
    }

    private JWEHeader createJweHeader() {
        return new JWEHeader.Builder(JWEAlgorithm.A192KW, EncryptionMethod.A256GCM)
                .contentType(TOKEN_CONTENT_TYPE)
                .build();
    }

    private JWTClaimsSet createJwtPayload(TokenType tokenType, Long memberId, LocalDateTime publishTime) {
        Date issuedTime = convertDate(publishTime);

        return new JWTClaimsSet.Builder()
                .issuer(tokenProperties.issuer())
                .issueTime(issuedTime)
                .expirationTime(calculateExpirationTime(issuedTime, tokenType))
                .claim(CLAIM_ID, memberId)
                .build();
    }

    private SignedJWT setupSignedJwt(JWTClaimsSet claims, TokenType tokenType) throws JOSEException {
        JWSHeader jwsHeader = new Builder(JWSAlgorithm.HS256).build();
        SignedJWT signedJwt = new SignedJWT(jwsHeader, claims);

        signedJwt.sign(jwsSignerFinder.findByTokenType(tokenType));
        return signedJwt;
    }

    private JWEObject setupJweObject(JWEHeader header, SignedJWT signedJwt) throws JOSEException {
        Payload payload = new Payload(signedJwt);
        JWEObject jweObject = new JWEObject(header, payload);

        jweObject.encrypt(jweEncrypter);
        return jweObject;
    }

    private Date convertDate(LocalDateTime target) {
        Instant targetInstant = target.atZone(ZoneId.systemDefault())
                                      .toInstant();

        return Date.from(targetInstant);
    }

    private Date calculateExpirationTime(Date issuedTime, TokenType tokenType) {
        Long expiredMillisSeconds = tokenProperties.findExpiredMillisSeconds(tokenType);

        return new Date(issuedTime.getTime() + expiredMillisSeconds);
    }

    public static class FailedEncodeTokenException extends IllegalStateException {

        public FailedEncodeTokenException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}

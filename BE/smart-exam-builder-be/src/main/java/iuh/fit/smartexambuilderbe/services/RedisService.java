package iuh.fit.smartexambuilderbe.services;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisService {
    private final RedisTemplate<String, String> redisTemplate;

    private static final String TOKEN_KEY_PREFIX = "invalid_token";

    public void saveInvalidatedToken(String tokenId, String token){
        redisTemplate.opsForValue().set(TOKEN_KEY_PREFIX+tokenId, token);
    }

    public boolean isTokenInvalidated(String tokenId) {
        return redisTemplate.hasKey(TOKEN_KEY_PREFIX + tokenId);
    }
}

package com.expmatik.backend.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository) {
        this.refreshTokenRepository = refreshTokenRepository;
    }

    public void save(RefreshToken refreshTokenEntity) {
        refreshTokenRepository.save(refreshTokenEntity);
    }

    public RefreshToken findByUserAndDeviceId(User user, String deviceId) {
        return refreshTokenRepository.findByUserAndDeviceId(user, deviceId)
                .orElse(new RefreshToken());
    }

    @Transactional
    public long deleteByUserAndDeviceId(User user, String deviceId) {
        long result = refreshTokenRepository.deleteByUserAndDeviceId(user, deviceId);
        refreshTokenRepository.flush();
        return result;
    }

    @Transactional
    public void delete(RefreshToken storedToken) {
        refreshTokenRepository.deleteById(storedToken.getId());
    }
}




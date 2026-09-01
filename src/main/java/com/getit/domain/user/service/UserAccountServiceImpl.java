package com.getit.domain.user.service;

import com.getit.domain.user.dto.OAuthRegistrationResult;
import com.getit.domain.user.dto.OAuthUserRegistration;
import com.getit.domain.user.dto.UserAccount;
import com.getit.domain.user.entity.User;
import com.getit.domain.user.exception.UserErrorCode;
import com.getit.domain.user.repository.UserRepository;
import com.getit.global.exception.BusinessException;
import java.time.LocalDateTime;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserAccountServiceImpl implements UserAccountService {

  private final UserRepository userRepository;

  @Override
  @Transactional
  public OAuthRegistrationResult registerOrUpdateOAuthUser(OAuthUserRegistration registration) {
    return userRepository.findByProviderId(registration.providerId())
        .map(existing -> {
          existing.updateProfile(registration.name(), registration.profileImageUrl());
          return new OAuthRegistrationResult(UserAccount.from(existing), false);
        })
        .orElseGet(() -> {
          User created = userRepository.save(User.createGuest(
              registration.providerId(),
              registration.email(),
              registration.name(),
              registration.profileImageUrl()
          ));
          return new OAuthRegistrationResult(UserAccount.from(created), true);
        });
  }

  @Override
  public Optional<UserAccount> findActiveById(Long userId) {
    return userRepository.findById(userId)
        .filter(user -> !user.isDeleted())
        .map(UserAccount::from);
  }

  /**
   * 이미 동의했으면 {@code User.consentToPrivacy} 가 최초 시각을 유지한다. 여기서 따로
   * 분기하지 않는 이유는 그 판단이 엔티티의 불변식이기 때문이다.
   */
  @Override
  @Transactional
  public UserAccount recordPrivacyConsent(Long userId, LocalDateTime consentedAt) {
    User user = userRepository.findById(userId)
        .filter(existing -> !existing.isDeleted())
        .orElseThrow(() -> new BusinessException(UserErrorCode.USER_NOT_FOUND));

    user.consentToPrivacy(consentedAt);
    return UserAccount.from(user);
  }
}

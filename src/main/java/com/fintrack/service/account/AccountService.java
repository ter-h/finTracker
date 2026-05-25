package com.fintrack.service.account;

import com.fintrack.domain.model.Account;
import com.fintrack.domain.model.User;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.UserRepository;
import com.fintrack.web.dto.request.CreateAccountRequest;
import com.fintrack.web.dto.response.AccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final UserRepository userRepository;

    public List<AccountResponse> getAccountsForUser(UUID userId) {
        return accountRepository
            .findByUserIdAndIsArchivedFalseOrderByCreatedAtAsc(userId)
            .stream()
            .map(AccountResponse::from)
            .toList();
    }

    @Transactional
    public AccountResponse createAccount(UUID userId, CreateAccountRequest request) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Account account = new Account();
        account.setUser(user);
        account.setName(request.name().strip());
        account.setType(request.type());
        account.setCurrency(request.currency().toUpperCase());

        BigDecimal openingBalance = request.openingBalance() != null
            ? request.openingBalance()
            : BigDecimal.ZERO;
        account.setBalance(openingBalance);

        account = accountRepository.save(account);
        log.info("Account created: userId={} accountId={}", userId, account.getId());
        return AccountResponse.from(account);
    }

    @Transactional
    public AccountResponse archiveAccount(UUID userId, UUID accountId) {
        Account account = accountRepository.findByIdAndUserId(accountId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        account.setArchived(true);
        account = accountRepository.save(account);
        return AccountResponse.from(account);
    }
}
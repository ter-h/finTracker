package com.fintrack.service.account;

import com.fintrack.domain.enums.AccountType;
import com.fintrack.domain.model.Account;
import com.fintrack.domain.model.User;
import com.fintrack.exception.ResourceNotFoundException;
import com.fintrack.repository.AccountRepository;
import com.fintrack.repository.UserRepository;
import com.fintrack.web.dto.request.CreateAccountRequest;
import com.fintrack.web.dto.response.AccountResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    private AccountService accountService;

    @BeforeEach
    void setUp() {
        accountService = new AccountService(accountRepository, userRepository);
    }

    @Test
    void getAccountsForUser_returnsMappedAccountList() {
        // Arrange
        UUID userId = UUID.randomUUID();

        Account checking = new Account();
        checking.setId(UUID.randomUUID());
        checking.setName("Everyday Checking");
        checking.setType(AccountType.CHECKING);
        checking.setCurrency("AUD");
        checking.setBalance(new BigDecimal("500.00"));

        Account savings = new Account();
        savings.setId(UUID.randomUUID());
        savings.setName("Emergency Fund");
        savings.setType(AccountType.SAVINGS);
        savings.setCurrency("AUD");
        savings.setBalance(new BigDecimal("2000.00"));

        when(accountRepository.findByUserIdAndIsArchivedFalseOrderByCreatedAtAsc(userId))
            .thenReturn(List.of(checking, savings));

        // Act
        List<AccountResponse> result = accountService.getAccountsForUser(userId);

        // Assert: right size, right order, and each entity mapped to its DTO correctly.
        // .extracting(...) pulls one field out of every element, so you can assert
        // on the whole list at once instead of indexing into it one by one.
        assertThat(result).hasSize(2);
        assertThat(result)
            .extracting(AccountResponse::name)
            .containsExactly("Everyday Checking", "Emergency Fund");
        assertThat(result.get(0).balance()).isEqualByComparingTo("500.00");
        assertThat(result.get(0).type()).isEqualTo("CHECKING");
    }

    @Test
    void createAccount_withOpeningBalance_usesProvidedBalance() {

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setDisplayName("new");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        CreateAccountRequest req = new CreateAccountRequest("acc1", AccountType.SAVINGS, "AUD", new BigDecimal("100.00"));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        AccountResponse res = accountService.createAccount(userId, req);

        assertThat(res.balance()).isEqualByComparingTo("100.00");

    }

    @Test
    void createAccount_withoutOpeningBalance_defaultsToZero() {

        UUID userId = UUID.randomUUID();
        User user = new User();
        user.setId(userId);
        user.setDisplayName("new");
        
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        CreateAccountRequest req = new CreateAccountRequest("acc1", AccountType.SAVINGS, "AUD", null);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> {
            Account toSave = invocation.getArgument(0);
            toSave.setId(UUID.randomUUID());
            return toSave;
        });

        AccountResponse res = accountService.createAccount(userId, req);

        assertThat(res.balance()).isEqualByComparingTo("0");

    }

    @Test
    void createAccount_whenUserNotFound_throwsResourceNotFoundException() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> accountService.createAccount(userId, null));
        verify(accountRepository, never()).save(any());
    }

    @Test
    void archiveAccount_whenFound_marksArchivedAndSaves() {
        UUID accId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        Account account = new Account();
        account.setId(accId);
        account.setArchived(false);
        account.setName("acc");
        account.setType(AccountType.SAVINGS);

        when(accountRepository.findByIdAndUserId(accId, userId)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));
        AccountResponse res = accountService.archiveAccount(userId, accId);
        assertThat(res.isArchived()).isTrue();

        // check the object passed to save() was already flipped to archived, not just the response
        verify(accountRepository).save(argThat(Account::isArchived));
    }

    @Test
    void archiveAccount_whenNotFoundOrNotOwnedByUser_throwsResourceNotFoundException() {
        UUID accId = UUID.randomUUID();
        UUID userID = UUID.randomUUID();

        when(accountRepository.findByIdAndUserId(accId, userID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> accountService.archiveAccount(userID, accId));
        verify(accountRepository, never()).save(any()); 

    }
}

package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.service.AccountsService;
import com.dws.challenge.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountsServiceTest {

  @Mock
  private NotificationService notificationService;

  @Mock
  private AccountsRepository accountsRepository;

  @InjectMocks
  private AccountsService accountsService;


  @Test
  void addAccount() {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));

    accountsService.createAccount(account);

    verify(accountsRepository).createAccount(account);
  }

  @Test
  void addAccount_failsOnDuplicateId() {
    String uniqueId = "Id-999";
    Account account = new Account(uniqueId);

    doThrow(new DuplicateAccountIdException("Account id " + uniqueId + " already exists!"))
            .when(accountsRepository).createAccount(account);

    assertThrows(DuplicateAccountIdException.class, () -> {
      accountsService.createAccount(account);
    });
  }

  @Test
  void transferAmount_sameAccountIds_throwsDuplicateAccountIdException() {
    TransferRequest request = new TransferRequest("Id-123", "Id-123", new BigDecimal("100.00"));

    DuplicateAccountIdException exception = assertThrows(DuplicateAccountIdException.class, () -> {
      accountsService.transferAmount(request);
    });

    assertEquals("accountfrom  and accountTo id's cannot be same.", exception.getMessage());
  }


  @Test
  void transferAmount_accountFromNotFound_throwsAccountNotFoundException() {
    TransferRequest request = new TransferRequest("Id-1234", "Id-1235", new BigDecimal("100.00"));
    when(accountsRepository.getAccount("Id-1234")).thenReturn(null);
    AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () -> {
      accountsService.transferAmount(request);
    });
    assertEquals("Account id Id-1234 not found.", exception.getMessage());
  }


  @Test
  void transferAmount_accountToNotFound_throwsAccountNotFoundException() {
    TransferRequest request = new TransferRequest("Id-1234", "Id-1235", new BigDecimal("100.00"));
    Account accountFrom = new Account("acc1", new BigDecimal("500.00"));
    when(accountsRepository.getAccount("Id-1234")).thenReturn(accountFrom);
    when(accountsRepository.getAccount("Id-1235")).thenReturn(null);
    AccountNotFoundException exception = assertThrows(AccountNotFoundException.class, () -> {
      accountsService.transferAmount(request);
    });
    assertEquals("Account id Id-1235 not found.", exception.getMessage());
  }


  @Test
  void transferAmount_shouldFail_WhenInsufficientBalance() {
    TransferRequest request = new TransferRequest("Id-1234", "Id-1235", new BigDecimal("1000.00"));
    Account accountFrom = new Account("Id-1234", new BigDecimal("500.00"));
    Account accountTo = new Account("Id-1235", new BigDecimal("200.00"));

    when(accountsRepository.getAccount(accountFrom.getAccountId())).thenReturn(accountFrom);
    when(accountsRepository.getAccount(accountTo.getAccountId())).thenReturn(accountTo);
    assertThrows(InsufficientBalanceException.class, () -> {
      accountsService.transferAmount(request);
    });

    assertThat(accountFrom.getBalance()).isEqualByComparingTo("500.00");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("200.00");
    verify(accountsRepository, never()).saveAccounts(any(), any());
    verify(notificationService, never()).notifyAboutTransfer(any(), anyString());
  }


  @Test
  void transferAmount_success(){
    TransferRequest request = new TransferRequest("Id-1234", "Id-1235", new BigDecimal("100.00"));
    Account accountFrom = new Account("Id-1234", new BigDecimal("1000.00"));
    Account accountTo = new Account("Id-1235", new BigDecimal("1000.00"));


    when(accountsRepository.getAccount("Id-1234")).thenReturn(accountFrom);
    when(accountsRepository.getAccount("Id-1235")).thenReturn(accountTo);

    accountsService.transferAmount(request);

    assertThat(accountFrom.getBalance()).isEqualByComparingTo("900.00");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("1100.00");

    verify(accountsRepository).saveAccounts(accountFrom, accountTo);

    verify(notificationService).notifyAboutTransfer(accountFrom,
            "Amount 100.00 deducted from account id Id-1234");
    verify(notificationService).notifyAboutTransfer(accountTo,
            "Amount 100.00 credited to account id Id-1235");
  }


}

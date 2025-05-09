package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.AccountNotFoundException;
import com.dws.challenge.exception.DuplicateAccountIdException;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


@Service
@Slf4j
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;


  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }


  public void transferAmount(TransferRequest request){
    if (request.getAccountFromId().equals(request.getAccountToId())){
      throw new DuplicateAccountIdException(
              "accountfrom  and accountTo id's cannot be same.");
    }
    Account accountFrom = this.accountsRepository.getAccount(request.getAccountFromId());
    if (accountFrom == null){
      throw new AccountNotFoundException("Account id " + request.getAccountFromId() + " not found.");
    }
    Account accountTo =  this.accountsRepository.getAccount(request.getAccountToId());
    if (accountTo == null){
      throw new AccountNotFoundException("Account id " + request.getAccountToId() + " not found.");
    }
    if (accountFrom != null && accountTo != null ){
      if (accountFrom.getBalance().compareTo(request.getAmount()) >= 0){
        accountFrom.setBalance(accountFrom.getBalance().subtract(request.getAmount()));
        accountTo.setBalance(accountTo.getBalance().add(request.getAmount()));
        this.accountsRepository.saveAccounts(accountFrom, accountTo);
        notificationService.notifyAboutTransfer(accountFrom, "Amount " + request.getAmount() + " deducted from account id " + request.getAccountFromId());
        notificationService.notifyAboutTransfer(accountTo, "Amount " + request.getAmount() + " credited to account id " + request.getAccountToId());
      }else{
        throw new InsufficientBalanceException("Account id " + request.getAccountFromId() + " does not have sufficient balance.");
      }

    }
  }
}

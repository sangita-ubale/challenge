package com.dws.challenge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

import java.math.BigDecimal;

import com.dws.challenge.domain.Account;
import com.dws.challenge.domain.TransferRequest;
import com.dws.challenge.exception.InsufficientBalanceException;
import com.dws.challenge.service.AccountsService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@WebAppConfiguration
class AccountsControllerTest {

  private MockMvc mockMvc;

  @Autowired
  private AccountsService accountsService;

  private ObjectMapper objectMapper = new ObjectMapper();

  @Autowired
  private WebApplicationContext webApplicationContext;

  @BeforeEach
  void prepareMockMvc() {
    this.mockMvc = webAppContextSetup(this.webApplicationContext).build();

    // Reset the existing accounts before each test.
    accountsService.getAccountsRepository().clearAccounts();
  }

  @Test
  void createAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    Account account = accountsService.getAccount("Id-123");
    assertThat(account.getAccountId()).isEqualTo("Id-123");
    assertThat(account.getBalance()).isEqualByComparingTo("1000");
  }

  @Test
  void createDuplicateAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isCreated());

    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountNoBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\"}")).andExpect(status().isBadRequest());
  }

//  @Test
//  void createAccountNoBody() throws Exception {
//    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON))
//      .andExpect(status().isBadRequest());
//  }

  @Test
  void createAccountNegativeBalance() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"Id-123\",\"balance\":-1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void createAccountEmptyAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts").contentType(MediaType.APPLICATION_JSON)
      .content("{\"accountId\":\"\",\"balance\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void getAccount() throws Exception {
    String uniqueAccountId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueAccountId, new BigDecimal("123.45"));
    this.accountsService.createAccount(account);
    this.mockMvc.perform(get("/v1/accounts/" + uniqueAccountId))
      .andExpect(status().isOk())
      .andExpect(
        content().string("{\"accountId\":\"" + uniqueAccountId + "\",\"balance\":123.45}"));
  }

  @Test
  void transferAmountEmptyFromAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"\",\"accountToId\":\"Id-123\", \"amount\":1000}")).andExpect(status().isBadRequest());
  }


  @Test
  void transferAmountEmptyToAccountId() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"\", \"amount\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void transferAmountToSameAccount() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-123\", \"amount\":1000}")).andExpect(status().isBadRequest());
  }

  @Test
  void transferAmountFromAccountNotFound() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-1234\", \"amount\":1000}")).andExpect(status().isNotFound());
  }

  @Test
  void transferAmountToAccountNotFound() throws Exception {
    this.mockMvc.perform(post("/v1/accounts/transfer").contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-1234\", \"amount\":1000}")).andExpect(status().isNotFound());
  }


  @Test
  void transferAmountSuccess() throws Exception {

    Account accountFrom = new Account("Id-123");
    accountFrom.setBalance(new BigDecimal("1000"));

    Account accountTo = new Account("Id-1234");
    accountTo.setBalance(new BigDecimal("1000"));

    accountsService.createAccount(accountFrom);
    accountsService.createAccount(accountTo);

    this.mockMvc.perform(post("/v1/accounts/transfer")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-1234\", \"amount\":100}"))
            .andExpect(status().isOk());

    Account updatedAccountFrom = accountsService.getAccount("Id-123");
    assertThat(updatedAccountFrom.getBalance()).isEqualByComparingTo("900");

    Account updatedAccountTo = accountsService.getAccount("Id-1234");
    assertThat(accountTo.getBalance()).isEqualByComparingTo("1100");
  }

}

package com.example.mstemplateredis.v1.api;
import com.example.mstemplateredis.v1.model.Account;
import com.example.mstemplateredis.v1.service.AccountService;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Stream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AccountService accountService;

    private static final String BASE_URL = "/brd-api/ms-template-redis/v1/accounts";

    // --- GET /{customerId} ---
    @Nested
    class GetAccounts {

        static Stream<Arguments> customerIdProvider() {
            return Stream.of(
                    Arguments.of("cust1", List.of(new Account("RO00AAA123456789", "cust1", BigDecimal.valueOf(200), null, null)), 200),
                    Arguments.of("custEmpty", List.of(), 200),
                    Arguments.of("custError", null, 500)
            );
        }

        @ParameterizedTest(name = "GET accounts for customerId: {0}, expected status: {2}")
        @MethodSource("customerIdProvider")
        void shouldHandleVariousGetScenarios(String customerId, List<Account> result, int expectedStatus) throws Exception {
            if (expectedStatus == 500) {
                when(accountService.getAccountsByCustomerId(customerId))
                        .thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR));
            } else {
                when(accountService.getAccountsByCustomerId(customerId)).thenReturn(result);
            }

            mockMvc.perform(get(BASE_URL + "/" + customerId))
                    .andExpect(status().is(expectedStatus));
        }
    }

    // --- POST /{customerId} ---
    @Nested
    class CreateAccount {

        static Stream<Arguments> accountJsonProvider() {
            return Stream.of(
                    // Invalid cases
                    Arguments.of("""
                            {
                                "iban": "INVALID",
                                "customerId": "cust123",
                                "balance": 100
                            }
                            """, 400),
                    Arguments.of("""
                            {
                                "customerId": "cust123",
                                "balance": 100
                            }
                            """, 400),
                    Arguments.of("""
                            {
                                "iban": "RO49AAAA1B31007593840000",
                                "balance": 100
                            }
                            """, 400),
                    Arguments.of("""
                            {
                                "iban": "RO49AAAA1B31007593840000",
                                "customerId": "cust123",
                                "balance": -50
                            }
                            """, 400),
                    Arguments.of("{}", 400),

                    // Valid case
                    Arguments.of("""
                            {
                                "iban": "RO49AAAA1B31007593840000",
                                "customerId": "cust123",
                                "balance": 100
                            }
                            """, 201)
            );
        }

        @ParameterizedTest(name = "POST account payload, expect HTTP {1}")
        @MethodSource("accountJsonProvider")
        void shouldHandleAccountCreationScenarios(String json, int expectedStatus) throws Exception {
            String customerId = "cust123";
            if (expectedStatus == 201) {
                Account mockResponse = new Account();
                mockResponse.setIban("RO49AAAA1B31007593840000");
                mockResponse.setBalance(BigDecimal.valueOf(100));
                when(accountService.createAccount(any(Account.class), eq(customerId)))
                        .thenReturn(mockResponse);
            }

            var resultActions = mockMvc.perform(post(BASE_URL + "/" + customerId)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().is(expectedStatus));

            if (expectedStatus == 201) {
                resultActions.andExpect(jsonPath("$.iban").value("RO49AAAA1B31007593840000"))
                        .andExpect(jsonPath("$.balance").value(100));
            }
        }
    }

    // --- PUT /{customerId}/update ---
    @Nested
    class UpdateAccount {

        static Stream<Arguments> updateParamsProvider() {
            return Stream.of(
                    Arguments.of("cust1", "RO49AAAA1B31007593840000", "200", 200),
                    Arguments.of("cust1", "RO49AAAA1B31007593840000", "-10", 400),
                    Arguments.of("cust1", "", "300", 400),
                    Arguments.of("", "RO49AAAA1B31007593840000", "300", 500)
            );
        }

        @ParameterizedTest(name = "PUT update custId={0}, iban={1}, balance={2} -> status {3}")
        @MethodSource("updateParamsProvider")
        void shouldHandleUpdateValidation(String customerId, String iban, String balance, int expectedStatus) throws Exception {
            mockMvc.perform(put(BASE_URL + "/" + customerId + "/update")
                            .param("iban", iban)
                            .param("balance", balance))
                    .andExpect(status().is(expectedStatus));

            if (expectedStatus == 200) {
                mockMvc.perform(put(BASE_URL + "/" + customerId + "/update")
                                .param("iban", iban)
                                .param("balance", balance))
                        .andExpect(status().isOk())
                        .andExpect(content().string("Account updated successfully."));
            }
        }
    }

    // --- DELETE /{customerId}/delete/{iban} ---
    @Nested
    class DeleteAccount {

        static Stream<Arguments> deleteArgsProvider() {
            return Stream.of(
                    Arguments.of("cust1", "RO49AAAA1B31007593840000", 200),
                    Arguments.of("cust1", "", 400),
                    Arguments.of("", "RO49AAAA1B31007593840000", 400)
            );
        }

        @ParameterizedTest(name = "DELETE account custId={0}, iban={1} -> status {2}")
        @MethodSource("deleteArgsProvider")
        void shouldHandleDeleteScenarios(String customerId, String iban, int expectedStatus) throws Exception {
            mockMvc.perform(delete(BASE_URL + "/" + customerId + "/delete/" + iban))
                    .andExpect(status().is(expectedStatus));

            if (expectedStatus == 200) {
                mockMvc.perform(delete(BASE_URL + "/" + customerId + "/delete/" + iban))
                        .andExpect(status().isOk())
                        .andExpect(content().string("Account deleted successfully."));
            }
        }
    }
}
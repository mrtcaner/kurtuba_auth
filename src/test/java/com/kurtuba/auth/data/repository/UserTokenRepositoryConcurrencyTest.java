package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.UserToken;
import com.kurtuba.auth.support.PostgresIntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
class UserTokenRepositoryConcurrencyTest extends PostgresIntegrationTestSupport {

    @Autowired
    private UserTokenRepository userTokenRepository;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void markRefreshTokenAsUsedIfAvailable_whenCalledConcurrently_onlyOneSucceeds() throws Exception {
        Instant now = Instant.now();
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        UserToken token = tx.execute(status -> userTokenRepository.save(UserToken.builder()
                .userId("user-1")
                .refreshToken("refresh-token-hash")
                .refreshTokenExp(now.plus(Duration.ofMinutes(5)))
                .refreshTokenUsed(false)
                .jti(UUID.randomUUID().toString())
                .clientId("client-1")
                .auds(List.of("kurtuba"))
                .scopes(List.of("SCOPE_USER"))
                .blocked(false)
                .createdDate(now)
                .expirationDate(now.plus(Duration.ofMinutes(5)))
                .build()));

        int threadCount = 8;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> futures = new ArrayList<>();

        try {
            for (int i = 0; i < threadCount; i++) {
                futures.add(executorService.submit(() -> {
                    ready.countDown();
                    start.await(2, TimeUnit.SECONDS);
                    return tx.execute(status ->
                            userTokenRepository.markRefreshTokenAsUsedIfAvailable(token.getId(), Instant.now()));
                }));
            }

            assertTrue(ready.await(3, TimeUnit.SECONDS));
            start.countDown();

            int successCount = 0;
            for (Future<Integer> future : futures) {
                successCount += future.get(5, TimeUnit.SECONDS);
            }

            assertEquals(1, successCount);
            boolean refreshTokenUsed = tx.execute(status ->
                    userTokenRepository.findById(token.getId()).orElseThrow().isRefreshTokenUsed());
            assertTrue(refreshTokenUsed);
        } finally {
            executorService.shutdownNow();
        }
    }
}

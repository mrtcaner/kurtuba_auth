package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface RegisteredClientRepository extends CrudRepository<RegisteredClient, String> {

    Optional<RegisteredClient> findByClientId(String clientId);

    Optional<RegisteredClient> findByClientName(String clientName);

    List<RegisteredClient> findByClientType(RegisteredClientType registeredClientType);

}

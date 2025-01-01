package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.RegisteredClientType;
import com.kurtuba.auth.data.model.RegisteredClient;
import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface RegisteredClientRepository extends CrudRepository<RegisteredClient, String> {

    RegisteredClient findByClientId(String clientId);

    RegisteredClient findByClientName(String clientName);

    List<RegisteredClient> findByClientType(RegisteredClientType registeredClientType);

}

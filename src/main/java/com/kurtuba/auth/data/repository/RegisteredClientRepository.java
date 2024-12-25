package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.RegisteredClient;
import org.springframework.data.repository.CrudRepository;

public interface RegisteredClientRepository extends CrudRepository<RegisteredClient, String> {

    RegisteredClient findByClientId(String clientId);

    RegisteredClient findByClientName(String clientName);


}

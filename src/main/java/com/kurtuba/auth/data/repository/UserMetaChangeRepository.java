package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.UserMetaChange;
import org.springframework.data.repository.CrudRepository;

public interface UserMetaChangeRepository extends CrudRepository<UserMetaChange, String> {

    UserMetaChange findByUserIdAndCode(String userId, String Code);

    UserMetaChange findByMetaAndCode(String meta, String code);

    UserMetaChange findByIdAndCode(String id, String code);

    UserMetaChange findByCode(String code);

}

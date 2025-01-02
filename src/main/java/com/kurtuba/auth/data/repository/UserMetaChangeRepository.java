package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.MetaChangeType;
import com.kurtuba.auth.data.model.UserMetaChange;
import org.springframework.data.repository.CrudRepository;
;

public interface UserMetaChangeRepository extends CrudRepository<UserMetaChange, String> {

    UserMetaChange findByLinkParam(String linkParam);

    void deleteAllByExecutedIsFalseAndUserIdAndMetaChangeType(String userId, MetaChangeType metaChangeType);

}

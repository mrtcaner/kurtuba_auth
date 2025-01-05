package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.model.UserMetaChange;
import org.springframework.data.repository.CrudRepository;
;import java.time.LocalDateTime;

public interface UserMetaChangeRepository extends CrudRepository<UserMetaChange, String> {

    UserMetaChange findByLinkParam(String linkParam);

    void deleteAllByExecutedIsFalseAndUserIdAndMetaOperationType(String userId, MetaOperationType metaOperationType);

    UserMetaChange findByUserIdAndMetaOperationTypeAndExpirationDateAfterAndExecutedIsFalse(String userId,
                                                                                            MetaOperationType metaOperationType,
                                                                                            LocalDateTime after);

    UserMetaChange findByLinkParamAndMetaOperationTypeAndExpirationDateAfterAndExecutedIsFalse(String linkParam,
                                                                                               MetaOperationType metaOperationType,
                                                                                               LocalDateTime after);

}

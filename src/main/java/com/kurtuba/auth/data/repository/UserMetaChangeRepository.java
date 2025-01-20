package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.enums.MetaOperationType;
import com.kurtuba.auth.data.model.UserMetaChange;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
;import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface UserMetaChangeRepository extends CrudRepository<UserMetaChange, String> {

    Optional<UserMetaChange> findByLinkParam(String linkParam);

    void deleteAllByExecutedIsFalseAndUserIdAndMetaOperationType(String userId, MetaOperationType metaOperationType);

    Optional<UserMetaChange> findByUserIdAndMetaOperationTypeAndExpirationDateAfterAndExecutedIsFalse(String userId,
                                                                                            MetaOperationType metaOperationType,
                                                                                            LocalDateTime afterDate);

    Optional<UserMetaChange> findByLinkParamAndMetaOperationTypeAndExpirationDateAfterAndExecutedIsFalse(String linkParam,
                                                                                               MetaOperationType metaOperationType,
                                                                                                         LocalDateTime afterDate);

    @Query("SELECT umc FROM UserMetaChange umc WHERE umc.contactType = 'MOBILE' and  umc.maxTryCount <= umc.tryCount " +
            "and umc.executed = false and umc.userId = :userId and umc.updatedDate >= :afterDate")
    List<UserMetaChange> getUsersFailedMobileVerificationAttemptsAfterDate(String userId, LocalDateTime afterDate);

}

package com.kurtuba.auth.service;

import com.kurtuba.auth.data.model.UserMetaChange;
import com.kurtuba.auth.data.repository.UserMetaChangeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserMetaChangeService {

    final
    UserMetaChangeRepository userMetaChangeRepository;

    public UserMetaChangeService(UserMetaChangeRepository userMetaChangeRepository) {
        this.userMetaChangeRepository = userMetaChangeRepository;
    }

    @Transactional
    public void save(UserMetaChange userMetaChange){
        // delete all non executed and same type of meta change requests of the user
        // there will be always one active userMetaChange per User-MetaChangeType
        userMetaChangeRepository.deleteAllByExecutedIsFalseAndUserIdAndMetaChangeType(
                userMetaChange.getUserId(),userMetaChange.getMetaChangeType());
        userMetaChangeRepository.save(userMetaChange);
    }

    public Optional<UserMetaChange> findById(String userMetaChangeId){
        return userMetaChangeRepository.findById(userMetaChangeId);
    }

    public UserMetaChange findByLinkParam(String linkParam){
        return userMetaChangeRepository.findByLinkParam(linkParam);
    }


}

package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.UserSetting;
import org.springframework.data.repository.CrudRepository;

public interface UserSettingRepository extends CrudRepository<UserSetting, String> {
}

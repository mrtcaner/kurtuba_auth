package com.kurtuba.auth.data.repository;

import com.kurtuba.auth.data.model.LocalizationSupportedCountry;
import org.springframework.data.repository.CrudRepository;

import java.util.List;
import java.util.Optional;

public interface LocalizationSupportedCountryRepository extends CrudRepository<LocalizationSupportedCountry, String> {

    Optional<LocalizationSupportedCountry> findByCountryCode(String countryCode);

    List<LocalizationSupportedCountry> findAllByOrderByCountryCodeAsc();
}

package com.zingo.app.repository;

import com.zingo.app.entity.City;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CityRepository extends JpaRepository<City, Long> {
  Optional<City> findFirstByPostalCode(String postalCode);

  Optional<City> findFirstByNameIgnoreCase(String name);
}

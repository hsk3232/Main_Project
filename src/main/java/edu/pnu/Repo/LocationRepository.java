package edu.pnu.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {

}

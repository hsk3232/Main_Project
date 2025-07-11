package edu.pnu.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.Location;

public interface LocationRepository extends JpaRepository<Location, Long> {
	List<Location> findByScanLocationContaining(String scanLocation);
}

package edu.pnu.Repo;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.Csv;

public interface CsvRepository extends JpaRepository<Csv, Long> {
	
	List<Csv> findByMemberUserId(String userId);
	
	List<Csv> findByFileNameContaining(String search);

	
}

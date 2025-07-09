package edu.pnu.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.Csv;

public interface CsvRepository extends JpaRepository<Csv, Long> {

}

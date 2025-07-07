package edu.pnu.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.EPC;

public interface EPCRepository extends JpaRepository<EPC, String> {

}

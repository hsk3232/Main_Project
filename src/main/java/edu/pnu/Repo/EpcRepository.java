package edu.pnu.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.Epc;

public interface EpcRepository extends JpaRepository<Epc, String> {

}

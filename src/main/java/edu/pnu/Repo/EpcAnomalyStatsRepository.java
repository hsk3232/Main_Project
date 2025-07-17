package edu.pnu.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.EpcAnomalyStats;

public interface EpcAnomalyStatsRepository extends JpaRepository<EpcAnomalyStats, Long> {
	
}

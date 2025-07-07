package edu.pnu.Repo;

import org.springframework.data.jpa.repository.JpaRepository;

import edu.pnu.domain.EventHistory;

public interface EventHistoryRepository extends JpaRepository<EventHistory, Long> {

}

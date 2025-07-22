package edu.pnu.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@Builder
@Entity
@Table(name = "aidata")
public class AiData {
	@Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
	Long aiId;
	
	
	@OneToOne(fetch = FetchType.LAZY)
	@JoinColumn(name="event_id")
	private EventHistory eventHistory;
	
	@Builder.Default
	private boolean anomaly = false;

	@Builder.Default
	private boolean jump = false;
	@Builder.Default
	private double jumpScore = 0.0;

	@Builder.Default
	private boolean evtOrderErr = false;
	@Builder.Default
	private double evtOrderErrScore = 0.0;

	@Builder.Default
	private boolean epcFake = false;
	@Builder.Default
	private double epcFakeScore = 0.0;

	@Builder.Default
	private boolean epcDup = false;
	@Builder.Default
	private double epcDupScore = 0.0;

	@Builder.Default
	private boolean locErr = false;
	@Builder.Default
	private double locErrScore = 0.0;
}

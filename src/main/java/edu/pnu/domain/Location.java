package edu.pnu.domain;

import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
@Entity
@Builder
@Table(name="location")
public class Location {
	@Id
	@Column(name ="location_id")
	private Long locationId;
	private String scanLocation;
	
	//1:N관계에서 1이 부모, 연관관계 노예
	// 1개의 장소는 다양안 eventHistory가질 수 있음 -> List로 받아야함.
	@OneToMany(mappedBy = "location", fetch = FetchType.LAZY)
	@ToString.Exclude
	private List<EventHistory> eventHistory;
}

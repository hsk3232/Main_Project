package edu.pnu.domain;

import java.util.List;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
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
@Table(name="location")
public class Location {
	@Id
	private Long locationId;
	private String scanLocation;
	
	@OneToMany(mappedBy = "location", fetch = FetchType.LAZY)
	@ToString.Exclude
	private List<EventHistory> eventHistory;
}

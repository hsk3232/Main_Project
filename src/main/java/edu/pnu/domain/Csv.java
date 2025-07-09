package edu.pnu.domain;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
public class Csv {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long fileId;

	@Column(nullable = false)
    private String fileName; // 실제 파일명

    private String filePath; // 저장 경로

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member; // 업로더 FK (member_id로 연결)

    private Long fileSize; // 파일 사이즈(byte)

    @CreationTimestamp
    @Column(updatable = false, name = "created_at")
    private LocalDateTime createdAt; // 업로드 시각

}

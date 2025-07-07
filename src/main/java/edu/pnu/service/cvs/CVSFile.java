package edu.pnu.service.cvs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVReader;

import edu.pnu.Repo.EPCRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.Repo.LocationRepository;
import edu.pnu.Repo.ProductRepository;
import edu.pnu.domain.EPC;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CVSFile {

	private final EPCRepository epcRepo;
	private final EventHistoryRepository eventRepo;
	private final LocationRepository locationRepo;
	private final ProductRepository productRepo;

	public void saveCsv(MultipartFile file) throws Exception {

		// cvs 1줄씩 읽기 위해 BufferedReader 사용
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8));
				CSVReader csv = new CSVReader(reader)) {

			String[] header = csv.readNext(); // header
			if (header == null)
				throw new RuntimeException("CSV에 헤어가 없음");

			// 1. 칼럼명-인덱스 매핑
			Map<String, Integer> colIdx = new HashMap<>();
			for (int i = 0; i < header.length; i++) {
				colIdx.put(header[i], i);
			}

			String[] l; // line
			while ((l = csv.readNext()) != null) {

				// 2. CSV 한 줄(line)에서 PK 값 추출 -> 모든 PK키부터 조회해 놔야함.
				String epcCode = l[colIdx.get("epc_code")];
				Long locationId = Long.parseLong(l[colIdx.get("location_id")]);
				Long productId = Long.parseLong(l[colIdx.get("product_id")]);

				// 3. **EPC, Location 엔티티 미리 조회 (없으면 예외)**
				// => 반드시 EventHistory 생성/저장 전에!
				EPC epc = epcRepo.findById(epcCode).orElseThrow(() -> new RuntimeException("EPC 없음: " + epcCode));
				Location location = locationRepo.findById(locationId)
						.orElseThrow(() -> new RuntimeException("Location 없음: " + locationId));

				// Location 객체 생성 및 데이터 저장
				location.setLocationId(Long.parseLong(l[colIdx.get("location_id")]));
				location.setScanLocation(l[colIdx.get("scan_location")]);
				locationRepo.save(location);

				// EventHistory 객체 생성 및 데이터 저장
				// 1. FK 입력을 하려면, 주인 Entity에서 연결한 Entity 객체를 직접 set 해야함.
				EventHistory event = new EventHistory();
				event.setEpc(epc);
				event.setLocation(location);
				event.setHubType(l[colIdx.get("hub_type")]);
				event.setBusinessStep(l[colIdx.get("business_step")]);
				event.setEventType(l[colIdx.get("event_type")]);
				event.setEventTime(LocalDateTime.parse(l[colIdx.get("event_time")]));
				event.setManufactureDate(LocalDate.parse(l[colIdx.get("manufacture_date")]));
				

			}
		}

	}

}

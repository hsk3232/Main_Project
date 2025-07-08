package edu.pnu.service.cvs;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import edu.pnu.Repo.EPCRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.Repo.LocationRepository;
import edu.pnu.Repo.ProductRepository;
import edu.pnu.domain.EPC;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;
import edu.pnu.domain.Product;
import edu.pnu.service.member.CustomUserDetails;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvFile {

	// --- Repository 주입 (DB와 연동) ---
	private final ProductRepository productRepo;
	private final EPCRepository epcRepo;
	private final LocationRepository locationRepo;
	private final EventHistoryRepository eventHistoryRepo;

	/**
	 * [주요 기능] - TSV(탭구분) CSV 파일을 한 줄씩 읽어서, - 날짜 필드는 LocalDateTime으로 변환 - 에러 발생 시 어느
	 * 행에서 났는지 명확히 표시
	 */

	public void saveCsv(MultipartFile file, CustomUserDetails user) throws Exception {

		try (// 1. 파일을 문자 스트림(UTF-8)으로 읽음
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
			// 2. OpenCSV에서 탭('\t') 구분자로 파싱하도록 설정
			CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
			CSVReader csv = new CSVReaderBuilder(reader).withCSVParser(parser).build();

			// 3. 첫 줄: 컬럼명(헤더) 읽어서 인덱스 맵핑
			String[] header = csv.readNext();
			if (header == null)
				throw new RuntimeException("CSV에 헤더가 없음");

			Map<String, Integer> colIdx = new HashMap<>();
			for (int i = 0; i < header.length; i++) {
				colIdx.put(header[i], i);
			}

			// 4. 날짜 형식 지정 (실제 데이터에 맞게)
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"); // event_time, manufacture_date
			DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd"); // expiry_date
			
			Long userLocationId = user.getLocationId();

			// 4. 중복 insert 방지용 Set 준비
			Set<Long> insertedLocations = new HashSet<>();
			Set<Long> insertedProducts = new HashSet<>();
			Set<String> insertedEPCs = new HashSet<>();

			// 5. 배치 저장용 리스트
			List<Location> locations = new ArrayList<>();
			List<Product> products = new ArrayList<>();
			List<EPC> epcs = new ArrayList<>();
			List<EventHistory> events = new ArrayList<>();

			int rowNum = 1;
			int batchSize = 1000;

			String[] row;
			while ((row = csv.readNext()) != null) {
				rowNum++;
				final String errorRow = "[row " + rowNum + "]";

				try {
					// [1] Location (중복 insert 방지)
					Long locationId = Long.parseLong(row[colIdx.get("location_id")]);
					if (!locationId.toString().equals(userLocationId)) {
			            throw new IllegalArgumentException("해당 공장의 데이터가 아닙니다");
			        }
					if (!insertedLocations.contains(locationId)) {
						Location location = new Location();
						location.setLocationId(locationId);
						if (colIdx.containsKey("scan_location"))
							location.setScanLocation(row[colIdx.get("scan_location")]);
						locations.add(location);
						insertedLocations.add(locationId);
					}

					// [2] Product (PK: epc_product, 중복 insert 방지)
					Long epcProduct = Long.parseLong(row[colIdx.get("epc_product")]);
					if (!insertedProducts.contains(epcProduct)) {
						Product product = new Product();
						product.setEpcProduct(epcProduct); // ⭐️ PK: epcProduct(Long)
						if (colIdx.containsKey("product_name"))
							product.setProductName(row[colIdx.get("product_name")]);
						products.add(product);
						insertedProducts.add(epcProduct);
					}

					// [3] EPC (중복 insert 방지)
					String epcCode = row[colIdx.get("epc_code")];
					if (!insertedEPCs.contains(epcCode)) {
						EPC epc = new EPC();
						epc.setEpcCode(epcCode);
						if (colIdx.containsKey("epc_company"))
							epc.setEpcCompany(row[colIdx.get("epc_company")]);
						if (colIdx.containsKey("epc_lot"))
							epc.setEpcLot(Long.parseLong(row[colIdx.get("epc_lot")]));
						if (colIdx.containsKey("epc_serial"))
							epc.setEpcSerial(row[colIdx.get("epc_serial")]);
						// FK: Product 연결 (epc_product로)
						Product prodFK = new Product();
						prodFK.setEpcProduct(epcProduct); // PK만 세팅
						epc.setProduct(prodFK);           // FK는 반드시 setProduct만 사용!
						epcs.add(epc);
						insertedEPCs.add(epcCode);
					}

					
					// [4] EventHistory는 무조건 insert (중복 없음, 각 row마다 insert)
					EventHistory event = new EventHistory();
					if (colIdx.containsKey("event_id") && !row[colIdx.get("event_id")].isEmpty())
						event.setEventId(Long.parseLong(row[colIdx.get("event_id")]));
					EPC epcFK = new EPC();
					epcFK.setEpcCode(epcCode);
					event.setEpc(epcFK); // FK(임시 객체)
					Location locationFK = new Location();
					locationFK.setLocationId(locationId);
					event.setLocation(locationFK); // FK(임시 객체)
					if (colIdx.containsKey("hub_type"))
						event.setHubType(row[colIdx.get("hub_type")]);
					if (colIdx.containsKey("business_step"))
						event.setBusinessStep(row[colIdx.get("business_step")]);
					if (colIdx.containsKey("event_type"))
						event.setEventType(row[colIdx.get("event_type")]);
					if (colIdx.containsKey("event_time") && !row[colIdx.get("event_time")].isEmpty())
						event.setEventTime(LocalDateTime.parse(row[colIdx.get("event_time")], dtf));
					if (colIdx.containsKey("manufacture_date") && !row[colIdx.get("manufacture_date")].isEmpty())
						event.setManufactureDate(
								LocalDateTime.parse(row[colIdx.get("manufacture_date")], dtf).toLocalDate());
					if (colIdx.containsKey("expiry_date") && !row[colIdx.get("expiry_date")].isEmpty())
						event.setExpiryDate(LocalDate.parse(row[colIdx.get("expiry_date")], ymdFormatter));
					events.add(event);

					
					// [5] 배치 저장 (batchSize마다 부모→자식 순서)
					if (locations.size() >= batchSize) {
						try {
							locationRepo.saveAll(locations);
						} catch (Exception e) {
							System.err.println(errorRow + " Location insert 오류: " + e.getMessage());
						}
						locations.clear();
					}
					if (products.size() >= batchSize) {
						try {
							productRepo.saveAll(products);
						} catch (Exception e) {
							System.err.println(errorRow + " Product insert 오류: " + e.getMessage());
						}
						products.clear();
					}
					if (epcs.size() >= batchSize) {
						try {
							epcRepo.saveAll(epcs);
						} catch (Exception e) {
							System.err.println(errorRow + " EPC insert 오류: " + e.getMessage());
						}
						epcs.clear();
					}
					if (events.size() >= batchSize) {
						try {
							eventHistoryRepo.saveAll(events);
						} catch (Exception e) {
							System.err.println(errorRow + " EventHistory insert 오류: " + e.getMessage());
						}
						events.clear();
					}

				} catch (Exception ex) {
					System.err.println(errorRow + " Insert 오류: " + ex.getMessage());
				}
			}

			// [6] 남은 데이터 마지막 저장
			if (!locations.isEmpty()) {
				try {
					locationRepo.saveAll(locations);
				} catch (Exception e) {
					System.err.println("마지막 Location insert 오류: " + e.getMessage());
				}
			}
			if (!products.isEmpty()) {
				try {
					productRepo.saveAll(products);
				} catch (Exception e) {
					System.err.println("마지막 Product insert 오류: " + e.getMessage());
				}
			}
			if (!epcs.isEmpty()) {
				try {
					epcRepo.saveAll(epcs);
				} catch (Exception e) {
					System.err.println("마지막 EPC insert 오류: " + e.getMessage());
				}
			}
			if (!events.isEmpty()) {
				try {
					eventHistoryRepo.saveAll(events);
				} catch (Exception e) {
					System.err.println("마지막 EventHistory insert 오류: " + e.getMessage());
				}
			}
		}
	}
}
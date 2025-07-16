//package edu.pnu.service.csv;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.Arrays;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Set;
//import java.util.stream.Collectors;
//
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import com.opencsv.CSVParser;
//import com.opencsv.CSVParserBuilder;
//import com.opencsv.CSVReader;
//import com.opencsv.CSVReaderBuilder;
//
//import edu.pnu.Repo.CsvRepository;
//import edu.pnu.Repo.EpcRepository;
//import edu.pnu.Repo.EventHistoryRepository;
//import edu.pnu.Repo.LocationRepository;
//import edu.pnu.Repo.MemberRepository;
//import edu.pnu.Repo.ProductRepository;
//import edu.pnu.config.CustomUserDetails;
//import edu.pnu.domain.Csv;
//import edu.pnu.domain.Epc;
//import edu.pnu.domain.EventHistory;
//import edu.pnu.domain.Location;
//import edu.pnu.domain.Member;
//import edu.pnu.domain.Product;
//import edu.pnu.exception.BadRequestException;
//import edu.pnu.exception.CsvFileNotFoundException;
//import edu.pnu.exception.FileUploadException;
//import edu.pnu.exception.InvalidCsvFormatException;
//import lombok.RequiredArgsConstructor;
//
//@Service
//@RequiredArgsConstructor
//public class CsvSaveService4 {
//
//	// ------------------ DI로 JPA Repository 주입 ------------------
//	private final ProductRepository productRepo;
//	private final EpcRepository epcRepo;
//	private final LocationRepository locationRepo;
//	private final EventHistoryRepository eventHistoryRepo;
//	private final CsvRepository csvRepo;
//	private final MemberRepository memberRepo;
//
//	/**
//	 * CSV 파일 업로드 후 데이터를 파싱하여 DB에 대용량/배치로 효율적으로 저장하는 메인 서비스 메서드
//	 */
//	public void postCsv(MultipartFile file, CustomUserDetails user) {
//		// [1] 사용자/파일 유효성 체크 -----------------------
//		if (user == null)
//			throw new BadRequestException("[오류] : [CsvSaveService] 로그인 정보 없음");
//		String userId = user.getUserId();
//		// Member 정보 조회 (없으면 에러)
//		Member member = memberRepo.findByUserId(userId).orElseThrow(() -> new RuntimeException("회원 정보 없음"));
//
//		// 파일이 없거나 비어있으면 예외
//		if (file == null || file.isEmpty())
//			throw new CsvFileNotFoundException("[오류] : [CsvSaveService] 파일을 찾지 못했음");
//
//		// 확장자가 .csv 아니면 예외
//		if (!file.getOriginalFilename().endsWith(".csv"))
//			throw new FileUploadException("[오류] : [CsvSaveService] CSV 파일 아님");
//
//		// [2] 업로드 파일 정보 로그용으로 Csv 엔티티 저장 -----------------
//		Csv csvLog = Csv.builder().fileName(file.getOriginalFilename()).filePath("c:/MainProject/save_csv") // 실제 저장
//																											// 경로(샘플)
//				.fileSize(file.getSize()).member(member).build();
//		csvLog = csvRepo.save(csvLog); // DB에 기록 남기기
//
//		try (BufferedReader reader = new BufferedReader(
//				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
//
//			// (1) 파일을 TSV(탭 구분)로 읽기 위한 OpenCSV 설정
//			CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
//			CSVReader csv = new CSVReaderBuilder(reader).withCSVParser(parser).build();
//
//			// (2) 첫 줄(헤더) 읽어서 컬럼명 확인
//			String[] header = csv.readNext();
//			if (header == null)
//				throw new InvalidCsvFormatException(
//						"[오류] : [CsvSaveService] CSV 파일 header 없음 (fileName=" + csvLog.getFileName() + ")");
//
//			// (3) 필수 컬럼명 모두 존재하는지 확인 (없으면 바로 예외)
//			String[] requiredColumns = { "location_id", "epc_product", "epc_code", "epc_lot", "event_type",
//					"business_step", "event_time" };
//			for (String col : requiredColumns) {
//				if (Arrays.stream(header).noneMatch(col::equals))
//					throw new InvalidCsvFormatException("[오류] : 필수 컬럼(" + col + ")이 없습니다.");
//			}
//
//			// (4) 컬럼명-인덱스 맵 생성 (컬럼명으로 쉽게 값 추출 가능)
//			Map<String, Integer> colIdx = new HashMap<>();
//			for (int i = 0; i < header.length; i++)
//				colIdx.put(header[i], i);
//
//			// (5) 날짜 포맷 준비 (문자열 -> 날짜 변환용)
//			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//			DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
//
//			// [6] 모든 데이터 row를 메모리에 미리 읽으면서, 각종 ID 집합도 추출
//			List<String[]> allRows = new ArrayList<>();
//			Set<Long> allLocationIds = new HashSet<>();
//			Set<Long> allProductIds = new HashSet<>();
//			Set<String> allEpcCodes = new HashSet<>();
//
//			String[] row;
//			while ((row = csv.readNext()) != null) {
//				allRows.add(row);
//				// DB 중복 체크를 위해 ID 집합도 미리 모두 모아둠
//				allLocationIds.add(parseLongSafe(getValue(colIdx, row, "location_id")));
//				allProductIds.add(parseLongSafe(getValue(colIdx, row, "epc_product")));
//				allEpcCodes.add(getValue(colIdx, row, "epc_code"));
//			}
//
//			// [7] 이미 DB에 존재하는 ID 집합을 미리 조회 (중복 저장 방지/existsById 반복 방지)
//			Set<Long> existLocationIds = locationRepo.findAllById(allLocationIds).stream().map(Location::getLocationId)
//					.collect(Collectors.toSet());
//			Set<Long> existProductIds = productRepo.findAllById(allProductIds).stream().map(Product::getEpcProduct)
//					.collect(Collectors.toSet());
//			Set<String> existEpcCodes = epcRepo.findAllById(allEpcCodes).stream().map(Epc::getEpcCode)
//					.collect(Collectors.toSet());
//
//			// [8] 실제 insert 대상 ID set, insert 대상 객체 리스트 선언
//			Set<Long> insertedLocations = new HashSet<>(existLocationIds); // 이미 존재하는 값은 중복 insert 막기 위함
//			Set<Long> insertedProducts = new HashSet<>(existProductIds);
//			Set<String> insertedEPCs = new HashSet<>(existEpcCodes);
//
//			List<Location> locations = new ArrayList<>();
//			List<Product> products = new ArrayList<>();
//			List<Epc> epcs = new ArrayList<>();
//			List<EventHistory> events = new ArrayList<>();
//
//			int batchSize = 4000; // 배치 insert 기준 (실무: 2000~5000 추천)
//			int totalRows = allRows.size();
//			int savedCount = 0;
//
//			// [9] 실제 데이터 Row별로 객체 생성 & 배치 저장
//			for (int i = 0; i < allRows.size(); i++) {
//				String[] dataRow = allRows.get(i);
//				int rowNum = i + 2; // 1-based 헤더 기준, 데이터는 2번 줄부터
//				final String errorRow = "[row " + rowNum + "]";
//
//				try {
//					// --- [A] Location 생성: location_id 중복 insert 방지
//					Long locationId = parseLongSafe(getValue(colIdx, dataRow, "location_id"));
//					if (!insertedLocations.contains(locationId)) {
//						locations.add(Location.builder().locationId(locationId)
//								.scanLocation(getValue(colIdx, dataRow, "scan_location"))
//								.latitude(parseDoubleSafe(getValue(colIdx, dataRow, "latitude")))
//								.longitude(parseDoubleSafe(getValue(colIdx, dataRow, "longitude"))).build());
//						insertedLocations.add(locationId);
//					}
//
//					// --- [B] Product 생성: epc_product 중복 insert 방지
//					Long epcProduct = parseLongSafe(getValue(colIdx, dataRow, "epc_product"));
//					if (!insertedProducts.contains(epcProduct)) {
//						products.add(Product.builder().epcProduct(epcProduct)
//								.productName(getValue(colIdx, dataRow, "product_name")).build());
//						insertedProducts.add(epcProduct);
//					}
//
//					// --- [C] EPC 생성: epc_code 중복 insert 방지
//					String epcCode = getValue(colIdx, dataRow, "epc_code");
//					if (!insertedEPCs.contains(epcCode)) {
//						epcs.add(Epc.builder().epcCode(epcCode)
//								.epcCompany(parseLongSafe(getValue(colIdx, dataRow, "epc_company")))
//								.epcLot(parseLongSafe(getValue(colIdx, dataRow, "epc_lot")))
//								.epcSerial(getValue(colIdx, dataRow, "epc_serial"))
//								// 연관관계(외래키) 미리 설정, 단편 객체로 연결
//								.location(Location.builder().locationId(locationId).build())
//								.product(Product.builder().epcProduct(epcProduct).build()).build());
//						insertedEPCs.add(epcCode);
//					}
//
//					// --- [D] EventHistory(이벤트 기록)는 중복없이 모두 저장
//					EventHistory.EventHistoryBuilder eventBuilder = EventHistory.builder();
//					// (선택) event_id가 있으면 세팅 (일반적으로 auto-increment)
//					if (!isNullOrEmpty(getValue(colIdx, dataRow, "event_id")))
//						eventBuilder.eventId(parseLongSafe(getValue(colIdx, dataRow, "event_id")));
//					eventBuilder.epc(Epc.builder().epcCode(epcCode).build())
//							.location(Location.builder().locationId(locationId).build())
//							.hubType(getValue(colIdx, dataRow, "hub_type"))
//							.eventType(getValue(colIdx, dataRow, "event_type")).fileLog(csvLog); // 업로드 파일 로그 엔티티 연결
//
//					// business_step 원본/정규화 모두 저장
//					String businessRaw = getValue(colIdx, dataRow, "business_step");
//					if (!isNullOrEmpty(businessRaw)) {
//						eventBuilder.businessOriginal(businessRaw).businessStep(normalizeBusinessStep(businessRaw));
//					}
//
//					// 날짜 컬럼은 값이 있을 때만 파싱(문자열→LocalDate/LocalDateTime)
//					if (!isNullOrEmpty(getValue(colIdx, dataRow, "event_time")))
//						eventBuilder.eventTime(LocalDateTime.parse(getValue(colIdx, dataRow, "event_time"), dtf));
//					if (!isNullOrEmpty(getValue(colIdx, dataRow, "manufacture_date")))
//						eventBuilder.manufactureDate(
//								LocalDateTime.parse(getValue(colIdx, dataRow, "manufacture_date"), dtf));
//					if (!isNullOrEmpty(getValue(colIdx, dataRow, "expiry_date")))
//						eventBuilder
//								.expiryDate(LocalDate.parse(getValue(colIdx, dataRow, "expiry_date"), ymdFormatter));
//
//					events.add(eventBuilder.build());
//
//					// --- [E] 배치 insert: 일정 개수마다 DB에 저장 (메모리 사용 최소화)
//					batchSave(locations, locationRepo, batchSize, errorRow + " Location");
//					batchSave(products, productRepo, batchSize, errorRow + " Product");
//					batchSave(epcs, epcRepo, batchSize, errorRow + " EPC");
//
//					if (events.size() >= batchSize) {
//						try {
//							eventHistoryRepo.saveAll(events); // 대량 insert
//							savedCount += events.size();
//							double percent = (savedCount * 100.0) / totalRows;
//							// 실시간 진행률 log (콘솔)
//							System.out.printf("[Batch] EventHistory 저장 성공: %d개, 진행률: %.2f%%\n", events.size(), percent);
//							events.clear(); // 저장 후 리스트 비움
//						} catch (Exception e) {
//							// 저장 에러는 전체 중단하지 않고, 콘솔 에러만 출력
//							System.err.println(errorRow + " EventHistory insert 오류: " + e.getMessage());
//						}
//					}
//				} catch (Exception ex) {
//					// Row 단위 에러는 전체 저장 멈추지 않고 로그만 남김 (실무 신뢰성↑)
//					System.err.println(errorRow + " Insert 오류: " + ex.getMessage());
//				}
//			}
//
//			// [10] 마지막 남은 데이터/객체들 저장 (배치 이하 사이즈)
//			batchSave(locations, locationRepo, 1, "마지막 Location");
//			batchSave(products, productRepo, 1, "마지막 Product");
//			batchSave(epcs, epcRepo, 1, "마지막 EPC");
//			if (!events.isEmpty()) {
//				try {
//					eventHistoryRepo.saveAll(events);
//					savedCount += events.size();
//					double percent = (savedCount * 100.0) / totalRows;
//					System.out.printf("[마지막] EventHistory 저장 성공: %d개, 진행률: %.2f%%\n", events.size(), percent);
//				} catch (Exception e) {
//					System.err.println("마지막 EventHistory insert 오류: " + e.getMessage());
//				}
//			}
//
//		} catch (Exception e) {
//			// 파일 전체 파싱/저장 에러는 롤백 및 에러 전파 (로그)
//			throw new RuntimeException("[CsvSaveService] 파일 읽기/저장 중 오류: " + e.getMessage(), e);
//		}
//	}
//
//	// ==================== 헬퍼 메서드 구간 ====================
//
//	/**
//	 * 일정 개수(batchSize)마다 리스트를 DB에 저장하는 공통 메서드 (실패시 전체 중단 없이 에러만 콘솔 출력)
//	 */
//	private <T> void batchSave(List<T> list, org.springframework.data.jpa.repository.JpaRepository<T, ?> repo,
//			int batchSize, String logTitle) {
//		if (list.size() >= batchSize) {
//			try {
//				repo.saveAll(list);
//				list.clear();
//			} catch (Exception e) {
//				System.err.println(logTitle + " insert 오류: " + e.getMessage());
//			}
//		}
//	}
//
//	/** 컬럼명으로 값 안전하게 추출 (null-safe) */
//	private String getValue(Map<String, Integer> colIdx, String[] row, String col) {
//		Integer idx = colIdx.get(col);
//		return (idx != null && idx < row.length) ? row[idx] : null;
//	}
//
//	/** String → Long 안전 변환(빈 값/예외시 null) */
//	private Long parseLongSafe(String s) {
//		try {
//			return (isNullOrEmpty(s)) ? null : Long.parseLong(s.trim());
//		} catch (Exception e) {
//			return null;
//		}
//	}
//
//	/** String → Double 안전 변환(빈 값/예외시 null) */
//	private Double parseDoubleSafe(String s) {
//		try {
//			return (isNullOrEmpty(s)) ? null : Double.parseDouble(s.trim());
//		} catch (Exception e) {
//			return null;
//		}
//	}
//
//	/** null 또는 빈 문자열 체크 */
//	private boolean isNullOrEmpty(String s) {
//		return s == null || s.trim().isEmpty();
//	}
//
//	/** business_step 값 정규화 (원본 값 → 도메인 상수) */
//	private String normalizeBusinessStep(String input) {
//		if (input == null)
//			return null;
//		input = input.trim().toLowerCase();
//		if (input.contains("factory"))
//			return "Factory";
//		if (input.contains("wms"))
//			return "WMS";
//		if (input.contains("logistics_hub") || input.contains("logi") || input.contains("hub"))
//			return "LogiHub";
//		if (input.startsWith("w_stock"))
//			return "Wholesaler";
//		if (input.startsWith("r_stock"))
//			return "Reseller";
//		if (input.contains("pos"))
//			return "POS";
//		return null; // 매칭되는 값 없으면 null
//	}
//}
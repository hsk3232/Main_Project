package edu.pnu.service.csv;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.opencsv.CSVParser;
import com.opencsv.CSVParserBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;

import edu.pnu.Repo.CsvRepository;
import edu.pnu.Repo.EpcRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.Repo.LocationRepository;
import edu.pnu.Repo.MemberRepository;
import edu.pnu.Repo.ProductRepository;
import edu.pnu.config.CustomUserDetails;
import edu.pnu.domain.Csv;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;
import edu.pnu.domain.Member;
import edu.pnu.domain.Product;
import edu.pnu.events.EventHistorySavedEvent;
import edu.pnu.exception.BadRequestException;
import edu.pnu.exception.CsvFileNotFoundException;
import edu.pnu.exception.FileUploadException;
import edu.pnu.exception.InvalidCsvFormatException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvSaveService {

	// ---- 의존성 주입 ----
	private final ProductRepository productRepo;
	private final EpcRepository epcRepo;
	private final LocationRepository locationRepo;

	private final CsvRepository csvRepo;
	private final MemberRepository memberRepo;
	private final CsvSaveBatchService batchService; // JdbcTemplate batch insert
	
	private final EventHistoryRepository eventHistoryRepo;
    private final ApplicationEventPublisher eventPublisher;
	
	private final WebSocketService webSocketService;

	private final int chunkSize = 1000; // 한 번에 읽어 처리할 row 수 (청크 단위)

	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■ [비동기 업로드 진입 메서드] ■■■■■■■■■■■■■■■■■■■■■■■■
	// CSV 파일을 chunk 단위로 버퍼링해서 효율적으로 파싱 및 저장하는 메인 메서드
	@Async
	public CompletableFuture<Void> postCsv(MultipartFile file, CustomUserDetails user) {
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

			log.info("[시작] CSV 파일 업로드 요청 처리 시작 - file: {}", file.getOriginalFilename());

			// [1] 유저 및 파일 검증 (Null 체크, 형식 체크, 로그인 체크 등)
			if (user == null)
				throw new BadRequestException("[오류] : [CsvSaveService] 로그인 정보 없음");

			String userId = user.getUserId();
			Member member = memberRepo.findByUserId(userId).orElseThrow(() -> new RuntimeException("회원 정보 없음"));

			if (file == null || file.isEmpty())
				throw new CsvFileNotFoundException("[오류] : [CsvSaveService] 파일을 찾지 못했음");

			if (!file.getOriginalFilename().endsWith(".csv"))
				throw new FileUploadException("[오류] : [CsvSaveService] CSV 파일 아님");

			// [2] 업로드 파일 정보 저장 (Csv 로그 엔티티)
			Csv csvLog = Csv.builder().fileName(file.getOriginalFilename()).filePath("c:/MainProject/save_csv")
					.fileSize(file.getSize()).member(member).build();

			csvLog = csvRepo.save(csvLog); //엔티티에 저장 완료

			// [3] CSV 파싱을 위한 Reader/Parser 준비 (탭 구분자)
			CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
			CSVReader csv = new CSVReaderBuilder(reader).withCSVParser(parser).build();

			// [4] 헤더 추출 및 필수 컬럼 존재 여부 검증
			String[] header = csv.readNext();
			if (header == null)
				throw new InvalidCsvFormatException(
						"[오류] : [CsvSaveService] CSV 파일 header 없음 (fileName=" + csvLog.getFileName() + ")");

			String[] requiredColumns = { "location_id", "epc_product", "epc_code", "epc_lot", "event_type",
					"business_step", "event_time" };
			
			for (String col : requiredColumns) {
				if (Arrays.stream(header).noneMatch(col::equals)) 
					// hearder돌면서 requiredColumns 비교해서 하나라도 같은 것이 있으면 false
					throw new InvalidCsvFormatException("[오류] : 필수 컬럼(" + col + ")이 없습니다.");
			}

			// [5] 컬럼명 <-> 인덱스 매핑
			Map<String, Integer> colIdx = new HashMap<>();
			for (int i = 0; i < header.length; i++)
				colIdx.put(header[i], i);

			// [6] 날짜 포맷터 준비
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

			// [7] 오류 로깅 맵, 중복 저장 방지 Set 등 각종 데이터 구조 초기화
			Map<String, List<Integer>> errorRows = new HashMap<>();

			// === [추가] 청크 전에 DB에 이미 있는 ID, CODE 미리 Set에 추가 ===
			Set<Long> allLocationIds = new HashSet<>();
			Set<Long> allProductIds = new HashSet<>();
			Set<String> allEpcCodes = new HashSet<>();

			// 미리 데이터 쌓을 리스트, 중복 체크 세트 선언 (while문 밖)
			Set<Long> factoryLocationIds = new HashSet<>();

			List<Location> locations = new ArrayList<>();
			List<Product> products = new ArrayList<>();
			List<Epc> epcs = new ArrayList<>();
			List<EventHistory> eventHistories = new ArrayList<>();

			Set<Long> insertedLocations = null;
			Set<Long> insertedProducts = null;
			Set<String> insertedEPCs = null;

			// [8] chunk 단위로 파일을 읽으며, 파싱/저장/검증 진행
			List<String[]> chunk = new ArrayList<>(chunkSize);
			String[] row;
			int rowNum = 1; // 헤더는 1줄이므로 1부터 시작

			// --- 1차 패스: 모든 row를 한 줄씩 읽으면서 id/code/factoryLocation 세트 집계 ---
			// 그리고 chunk 쌓기, chunkSize마다 저장까지 동시에!
			while ((row = csv.readNext()) != null) {
				rowNum++;
				chunk.add(row);

				Long locId = parseLongSafe(getValue(colIdx, row, "location_id"));
				Long prodId = parseLongSafe(getValue(colIdx, row, "epc_product"));
				String epcCode = getValue(colIdx, row, "epc_code");

				// allLocationIds/allProductIds/allEpcCodes 누적
				if (locId != null)
					allLocationIds.add(locId);
				if (prodId != null)
					allProductIds.add(prodId);
				if (!isNullOrEmpty(epcCode))
					allEpcCodes.add(epcCode);

				// factoryLocationIds 누적
				String businessStep = getValue(colIdx, row, "business_step");
				if ("Factory".equalsIgnoreCase(businessStep)) {
					if (locId != null)
						factoryLocationIds.add(locId);
				}

				// === chunkSize에 도달하면 ===
				if (chunk.size() >= chunkSize) {
					// === chunkSize 도달시점(최초)에만 중복세트 초기화 ===
					if (insertedLocations == null) {
						Set<Long> existLocationIds = locationRepo.findAllById(allLocationIds).stream()
								.map(Location::getLocationId).collect(Collectors.toSet());
						Set<Long> existProductIds = productRepo.findAllById(allProductIds).stream()
								.map(Product::getEpcProduct).collect(Collectors.toSet());
						Set<String> existEpcCodes = epcRepo.findAllById(allEpcCodes).stream().map(Epc::getEpcCode)
								.collect(Collectors.toSet());

						insertedLocations = new HashSet<>(existLocationIds);
						insertedProducts = new HashSet<>(existProductIds);
						insertedEPCs = new HashSet<>(existEpcCodes);
					}

					// chunkSize에 도달할 때마다 batch 저장

					log.info("[진행] 청크 처리 시작 ({}줄) - 시작 row: {}", chunk.size(), rowNum - chunk.size() + 1);
					parseAndStoreChunk(chunk, colIdx, csvLog, dtf, ymdFormatter, insertedLocations, insertedProducts,
							insertedEPCs, locations, products, epcs, eventHistories, errorRows,
							rowNum - chunk.size() + 1);

					try {
						// [1] Location 저장 (중복 없는 신규만)
					    batchService.saveLocations(locations);

					    // [2] Product 저장
					    batchService.saveProducts(products);

					    // [3] Epc 저장
					    batchService.saveEpcs(epcs);

					    // [4] EventHistory 저장 (마지막!)
					    batchService.saveEventHistories(eventHistories);
						log.info("[성공] 청크 처리 완료 (row: {} ~ {})", rowNum - chunkSize + 1, rowNum);

					} catch (Exception e) {
						// ★ 저장 실패 시, 해당 chunk의 row 번호를 모두 errorRows에 기록 ★
						int startRow = rowNum - chunkSize + 1;
						int endRow = rowNum;
						List<Integer> failRows = new ArrayList<>();
						for (int i = startRow; i <= endRow; i++)
							failRows.add(i);
						errorRows.computeIfAbsent("DB 저장 실패", k -> new ArrayList<>()).addAll(failRows);
						log.error("[오류] 청크 저장 실패 (row: {} ~ {}): {}", startRow, endRow, e.getMessage());
					}

					locations.clear();
					products.clear();
					epcs.clear();
					eventHistories.clear();
					chunk.clear();
				}
			}
			// [9] 마지막 남은 chunk 저장 처리
			// === while문 종료 후, chunk가 남았으면 저장 ===
			if (!chunk.isEmpty()) {
				// ★ chunkSize만큼 데이터가 없으면, insertedLocations가 null일 수 있으니 한 번 더 체크
				if (insertedLocations == null) {
					Set<Long> existLocationIds = locationRepo.findAllById(allLocationIds).stream()
							.map(Location::getLocationId).collect(Collectors.toSet());
					Set<Long> existProductIds = productRepo.findAllById(allProductIds).stream()
							.map(Product::getEpcProduct).collect(Collectors.toSet());
					Set<String> existEpcCodes = epcRepo.findAllById(allEpcCodes).stream().map(Epc::getEpcCode)
							.collect(Collectors.toSet());

					insertedLocations = new HashSet<>(existLocationIds);
					insertedProducts = new HashSet<>(existProductIds);
					insertedEPCs = new HashSet<>(existEpcCodes);
				}

				try {
					// [1] Location 저장 (중복 없는 신규만)
				    batchService.saveLocations(locations);

				    // [2] Product 저장
				    batchService.saveProducts(products);

				    // [3] Epc 저장
				    batchService.saveEpcs(epcs);

				    // [4] EventHistory 저장 (마지막!)
				    batchService.saveEventHistories(eventHistories);
					log.info("[성공] 마지막 청크 처리 완료 (row: {} ~ {})", rowNum - chunk.size() + 1, rowNum);
				} catch (Exception e) {
					// ★ 저장 실패 시, 해당 chunk의 row 번호를 모두 errorRows에 기록 ★
					int startRow = rowNum - chunkSize + 1;
					int endRow = rowNum;
					List<Integer> failRows = new ArrayList<>();
					for (int i = startRow; i <= endRow; i++)
						failRows.add(i);
					errorRows.computeIfAbsent("DB 저장 실패", k -> new ArrayList<>()).addAll(failRows);
					log.error("[오류] 청크 저장 실패 (row: {} ~ {}): {}", startRow, endRow, e.getMessage());
				}

				locations.clear();
				products.clear();
				epcs.clear();
				eventHistories.clear();
				chunk.clear();
			}
			log.info("[END] 전체 CSV 업로드 완료 - 총 row 수: {}", rowNum - 1);

			// (1) 저장이 모두 끝난 직후! 이벤트 발행
			eventPublisher.publishEvent(new EventHistorySavedEvent(csvLog.getFileId()));

			webSocketService.sendMessage(user.getUserId(), "업로드 및 저장 성공");
			

			// [10] Factory location 허용 여부 체크
			if (!factoryLocationIds.contains(member.getLocation().getLocationId())) {
				throw new BadRequestException("[업로드 실패] 회원의 location_id(" + member.getLocation().getLocationId()
						+ ")가 Factory business_step의 location_id와 일치하지 않습니다.");
			}

			// [11] 오류 리포트 출력 및 예외 처리
			if (!errorRows.isEmpty()) {
				StringBuilder report = new StringBuilder("[CSV 저장 전체 오류 요약]\n");
				errorRows.forEach((type, rows) -> {
					report.append("오류[").append(type).append("]: ").append(rows.size()).append("건 rows: ").append(rows)
							.append("\n");
				});
				throw new RuntimeException(report.toString());
			}

		} catch (Exception e) {
			log.error("[ERROR] CSV 처리 중 예외 발생 - 원인: {}", e.getMessage(), e);
			throw new RuntimeException(e);
		}
		return CompletableFuture.completedFuture(null);
	}

	// ■■■■■■■■■■■■■■■■■■■■■■■■■■■ [청크 단위 CSV 파싱 메서드] ■■■■■■■■■■■■■■■■■■■■■■■■
	// 하나의 chunk(List<String[]>)를 받아서 도메인 객체로 변환하고 중복 여부를 검사하는 메서드
	// 1. 필수 항목 누락 체크 및 오류 집계
	// 2. location_id, epc_product, epc_code의 중복 여부를 insertedXxx Set으로 관리
	// 3. 각 row에 대해 Location, Product, Epc, EventHistory 객체를 생성하고 리스트에 담음
	// 4. 날짜, boolean, double 등 타입 파싱은 tryParse 메서드에서 오류 감지 및 로그 기록 처리
	private void parseAndStoreChunk(List<String[]> chunk, Map<String, Integer> colIdx, Csv csvLog,
			DateTimeFormatter dtf, DateTimeFormatter ymdFormatter, Set<Long> insertedLocations,
			Set<Long> insertedProducts, Set<String> insertedEPCs, List<Location> locations, List<Product> products,
			List<Epc> epcs, List<EventHistory> eventHistories, Map<String, List<Integer>> errorRows, int startRowNum) {

		for (int i = 0; i < chunk.size(); i++) {
			String[] row = chunk.get(i);
			int currentRow = startRowNum + i;
			try {
				Long locId = parseLongSafe(getValue(colIdx, row, "location_id"));
				Long prodId = parseLongSafe(getValue(colIdx, row, "epc_product"));
				String epcCode = getValue(colIdx, row, "epc_code");

				// === [추가] 동기 방식과 동일: 필수값 파싱 오류 별도 집계
				if (locId == null) {
					errorRows.computeIfAbsent("location_id 파싱 오류", k -> new ArrayList<>()).add(currentRow);
					continue;
				}
				if (prodId == null) {
					errorRows.computeIfAbsent("epc_product 파싱 오류", k -> new ArrayList<>()).add(currentRow);
					continue;
				}
				if (isNullOrEmpty(epcCode)) {
					errorRows.computeIfAbsent("epc_code 파싱 오류", k -> new ArrayList<>()).add(currentRow);
					continue;
				}

				// === [중복 INSERT 방지: DB+파일 모두] ===
				if (!insertedLocations.contains(locId)) {
					locations.add(
							Location.builder().locationId(locId).scanLocation(getValue(colIdx, row, "scan_location"))
									.latitude(parseDoubleSafe(getValue(colIdx, row, "latitude")))
									.longitude(parseDoubleSafe(getValue(colIdx, row, "longitude"))).build());
					insertedLocations.add(locId);
				}
				if (!insertedProducts.contains(prodId)) {
					products.add(Product.builder().epcProduct(prodId).productName(getValue(colIdx, row, "product_name"))
							.build());
					insertedProducts.add(prodId);
				}
				if (!insertedEPCs.contains(epcCode)) {
					epcs.add(Epc.builder().epcCode(epcCode).epcHeader(getValue(colIdx, row, "epc_header"))
							.epcCompany(parseLongSafe(getValue(colIdx, row, "epc_company")))
							.epcLot(parseLongSafe(getValue(colIdx, row, "epc_lot")))
							.epcSerial(getValue(colIdx, row, "epc_serial"))
							.location(Location.builder().locationId(locId).build())
							.product(Product.builder().epcProduct(prodId).build()).build());
					insertedEPCs.add(epcCode);
				}

				// === 날짜 등 필드 파싱 오류 별도 집계 ===
				EventHistory.EventHistoryBuilder evBuilder = EventHistory.builder()
						.epc(Epc.builder().epcCode(epcCode).build())
						.location(Location.builder().locationId(locId).build())
						.hubType(getValue(colIdx, row, "hub_type")).eventType(getValue(colIdx, row, "event_type"))
						.businessOriginal(getValue(colIdx, row, "business_step"))
						.businessStep(normalizeBusinessStep(getValue(colIdx, row, "business_step"))).fileLog(csvLog);

				// 날짜 파싱
				evBuilder.eventTime(tryParseDateTime(getValue(colIdx, row, "event_time"), dtf, errorRows, currentRow,
						"event_time"));
				evBuilder.manufactureDate(tryParseDateTime(getValue(colIdx, row, "manufacture_date"), dtf, errorRows,
						currentRow, "manufacture_date"));
				evBuilder.expiryDate(tryParseDate(getValue(colIdx, row, "expiry_date"), ymdFormatter, errorRows,
						currentRow, "expiry_date"));

				// 기타 필드
				evBuilder.anomaly(parseBooleanSafe(getValue(colIdx, row, "anomaly")))
						.jump(parseBooleanSafe(getValue(colIdx, row, "jump")))
						.jumpScore(parseDoubleSafe(getValue(colIdx, row, "jump_score")))
						.evtOrderErr(parseBooleanSafe(getValue(colIdx, row, "evt_order_err")))
						.evtOrderErrScore(parseDoubleSafe(getValue(colIdx, row, "evt_order_err_score")))
						.epcFake(parseBooleanSafe(getValue(colIdx, row, "epc_fake")))
						.epcFakeScore(parseDoubleSafe(getValue(colIdx, row, "epc_fake_score")))
						.epcDup(parseBooleanSafe(getValue(colIdx, row, "epc_dup")))
						.epcDupScore(parseDoubleSafe(getValue(colIdx, row, "epc_dup_score")))
						.locErr(parseBooleanSafe(getValue(colIdx, row, "loc_err")))
						.locErrScore(parseDoubleSafe(getValue(colIdx, row, "loc_err_score")));

				eventHistories.add(evBuilder.build());

			} catch (Exception e) {
				errorRows.computeIfAbsent("파싱 오류", k -> new ArrayList<>()).add(currentRow);
			}
		}
	}

//	 ■■■■■■■■■■■■■■■■■■■■■■■■■■■ [ 헬퍼 메서드 ] ■■■■■■■■■■■■■■■■■■■■■■■■
//	 tryParseDateTime: 문자열을 LocalDateTime으로 안전하게 파싱
//	   - 실패 시 null 반환 및 errorRows에 "필드명 파싱 오류"로 row 번호 저장
//	
//	 tryParseDate: 문자열을 LocalDate로 안전하게 파싱
//	   - 위와 동일한 방식으로 오류 누적 

//	 컬럼명으로부터 값 추출 (index map 사용, index 범위 체크 포함)
	private String getValue(Map<String, Integer> colIdx, String[] row, String col) {
		Integer idx = colIdx.get(col);
		return (idx != null && idx < row.length) ? row[idx] : null;
	}

	// 문자열을 Long 타입으로 안전하게 변환 (빈 문자열 → null)
	private Long parseLongSafe(String s) {
		try {
			return (isNullOrEmpty(s)) ? null : Long.parseLong(s.trim());
		} catch (Exception e) {
			return null;
		}
	}

	// 문자열을 Double 타입으로 안전하게 변환 (빈 문자열 → 0.0)
	private Double parseDoubleSafe(String s) {
		try {
			return (isNullOrEmpty(s)) ? 0.0 : Double.parseDouble(s.trim());
		} catch (Exception e) {
			return 0.0;
		}
	}

	// 문자열을 Boolean 타입으로 변환 ("1" 또는 "true"일 경우 true 반환)
	private boolean parseBooleanSafe(String s) {
		if (isNullOrEmpty(s))
			return false;
		return "1".equals(s.trim()) || "true".equalsIgnoreCase(s.trim());
	}

	// 문자열이 null이거나 빈 문자열인지 확인
	private boolean isNullOrEmpty(String s) {
		return s == null || s.trim().isEmpty();
	}

	// 문자열을 LocalDateTime으로 파싱
	private LocalDateTime tryParseDateTime(String value, DateTimeFormatter formatter,
			Map<String, List<Integer>> errorRows, int rowNum, String fieldName) {
		try {
			return isNullOrEmpty(value) ? null : LocalDateTime.parse(value.trim(), formatter);
		} catch (Exception e) {
			errorRows.computeIfAbsent(fieldName + " 파싱 오류", k -> new ArrayList<>()).add(rowNum);
			return null;
		}
	}

	// 문자열을 LocalDate로 파싱
	private LocalDate tryParseDate(String value, DateTimeFormatter formatter, Map<String, List<Integer>> errorRows,
			int rowNum, String fieldName) {
		try {
			return isNullOrEmpty(value) ? null : LocalDate.parse(value.trim(), formatter);
		} catch (Exception e) {
			errorRows.computeIfAbsent(fieldName + " 파싱 오류", k -> new ArrayList<>()).add(rowNum);
			return null;
		}
	}

	private String normalizeBusinessStep(String input) {
		if (input == null)
			return null;
		input = input.trim().toLowerCase();
		if (input.contains("factory"))
			return "Factory";
		if (input.contains("wms"))
			return "WMS";
		if (input.contains("logistics_hub") || input.contains("logi") || input.contains("hub"))
			return "LogiHub";
		if (input.startsWith("w_stock"))
			return "Wholesaler";
		if (input.startsWith("r_stock"))
			return "Reseller";
		if (input.contains("pos"))
			return "POS";
		return null;
	}

}
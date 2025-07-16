//package edu.pnu.service.csv;
//
//import java.io.BufferedReader;
//import java.io.InputStreamReader;
//import java.nio.charset.StandardCharsets;
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.time.format.DateTimeFormatter;
//import java.util.ArrayList;
//import java.util.HashMap;
//import java.util.HashSet;
//import java.util.List;
//import java.util.Map;
//import java.util.Optional;
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
//public class CsvSaveService3 {
//
//	private final ProductRepository productRepo;
//	private final EpcRepository epcRepo;
//	private final LocationRepository locationRepo;
//	private final EventHistoryRepository eventHistoryRepo;
//	private final CsvRepository csvRepo;
//	private final MemberRepository memberRepo;
//
//	/**
//	 * CSV 업로드 동기 저장 (JPA 영속 참조 보장, 대용량 실무/에러 summary)
//	 */
//	public void postCsv(MultipartFile file, CustomUserDetails user) {
//		Map<String, List<Integer>> errorRows = new HashMap<>();
//		List<String> saveErrors = new ArrayList<>();
//
//		// [1] 유저/파일 검증
//		if (user == null)
//			throw new BadRequestException("[오류] : [CsvSaveService] 로그인 정보 없음");
//		String userId = user.getUserId();
//		Member member = memberRepo.findByUserId(userId).orElseThrow(() -> new RuntimeException("회원 정보 없음"));
//		if (file == null || file.isEmpty())
//			throw new CsvFileNotFoundException("[오류] : [CsvSaveService] 파일을 찾지 못했음");
//		if (!file.getOriginalFilename().endsWith(".csv"))
//			throw new FileUploadException("[오류] : [CsvSaveService] CSV 파일 아님");
//
//		// [2] 업로드 이력 저장
//		Csv csvLog = Csv.builder().fileName(file.getOriginalFilename()).filePath("c:/MainProject/save_csv")
//				.fileSize(file.getSize()).member(member).build();
//		csvLog = csvRepo.save(csvLog);
//
//		try (BufferedReader reader = new BufferedReader(
//				new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
//
//			// [3] 헤더 체크/필수 컬럼 확인/매핑
//			CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
//			CSVReader csv = new CSVReaderBuilder(reader).withCSVParser(parser).build();
//			String[] header = csv.readNext();
//			if (header == null)
//				throw new InvalidCsvFormatException(
//						"[오류] : [CsvSaveService] CSV 파일 header 없음 (fileName=" + csvLog.getFileName() + ")");
//			String[] requiredColumns = { "location_id", "epc_product", "epc_code", "epc_lot", "event_type",
//					"business_step", "event_time" };
//			for (String col : requiredColumns) {
//				boolean found = false;
//				for (String h : header)
//					if (col.equals(h))
//						found = true;
//				if (!found)
//					throw new InvalidCsvFormatException("[오류] : 필수 컬럼(" + col + ")이 없습니다.");
//			}
//			Map<String, Integer> colIdx = new HashMap<>();
//			for (int i = 0; i < header.length; i++)
//				colIdx.put(header[i], i);
//
//			// [4] 날짜 포맷
//			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//			DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
//
//			// [5] row별 파싱/오류 누적, 저장용 set/리스트 생성
//			List<String[]> allRows = new ArrayList<>();
//			Set<Long> allLocationIds = new HashSet<>();
//			Set<Long> allProductIds = new HashSet<>();
//			Set<String> allEpcCodes = new HashSet<>();
//			String[] row;
//			int rowNum = 1;
//			while ((row = csv.readNext()) != null) {
//				rowNum++;
//				Long locationId = parseLongSafe(getValue(colIdx, row, "location_id"));
//				Long epcProduct = parseLongSafe(getValue(colIdx, row, "epc_product"));
//				String epcCode = getValue(colIdx, row, "epc_code");
//				if (locationId == null) {
//					errorRows.computeIfAbsent("location_id 파싱 오류", k -> new ArrayList<>()).add(rowNum);
//					continue;
//				}
//				if (epcProduct == null) {
//					errorRows.computeIfAbsent("epc_product 파싱 오류", k -> new ArrayList<>()).add(rowNum);
//					continue;
//				}
//				if (isNullOrEmpty(epcCode)) {
//					errorRows.computeIfAbsent("epc_code 파싱 오류", k -> new ArrayList<>()).add(rowNum);
//					continue;
//				}
//				allRows.add(row);
//				allLocationIds.add(locationId);
//				allProductIds.add(epcProduct);
//				allEpcCodes.add(epcCode);
//			}
//
//			// [6] 미리 DB에 있는 id set 추출
//			Set<Long> existLocationIds = locationRepo.findAllById(allLocationIds).stream().map(Location::getLocationId)
//					.collect(Collectors.toSet());
//			Set<Long> existProductIds = productRepo.findAllById(allProductIds).stream().map(Product::getEpcProduct)
//					.collect(Collectors.toSet());
//			Set<String> existEpcCodes = epcRepo.findAllById(allEpcCodes).stream().map(Epc::getEpcCode)
//					.collect(Collectors.toSet());
//			Set<Long> insertedLocations = new HashSet<>(existLocationIds);
//			Set<Long> insertedProducts = new HashSet<>(existProductIds);
//			Set<String> insertedEPCs = new HashSet<>(existEpcCodes);
//
//			List<Location> locations = new ArrayList<>();
//			List<Product> products = new ArrayList<>();
//			List<Epc> epcs = new ArrayList<>();
//
//			int batchSize = 4000;
//			int totalRows = allRows.size();
//			int savedCount = 0;
//
//			// [7] 1차 loop: Location/Product/EPC 미존재만 리스트화
//			for (String[] dataRow : allRows) {
//				Long locationId = parseLongSafe(getValue(colIdx, dataRow, "location_id"));
//				Long epcProduct = parseLongSafe(getValue(colIdx, dataRow, "epc_product"));
//				String epcCode = getValue(colIdx, dataRow, "epc_code");
//
//				if (!insertedLocations.contains(locationId)) {
//					locations.add(Location.builder().locationId(locationId)
//							.scanLocation(getValue(colIdx, dataRow, "scan_location"))
//							.latitude(parseDoubleSafe(getValue(colIdx, dataRow, "latitude")))
//							.longitude(parseDoubleSafe(getValue(colIdx, dataRow, "longitude"))).build());
//					insertedLocations.add(locationId);
//				}
//				if (!insertedProducts.contains(epcProduct)) {
//					products.add(Product.builder().epcProduct(epcProduct)
//							.productName(getValue(colIdx, dataRow, "product_name")).build());
//					insertedProducts.add(epcProduct);
//				}
//				if (!insertedEPCs.contains(epcCode)) {
//					epcs.add(Epc.builder().epcCode(epcCode)
//							.epcCompany(parseLongSafe(getValue(colIdx, dataRow, "epc_company")))
//							.epcLot(parseLongSafe(getValue(colIdx, dataRow, "epc_lot")))
//							.epcSerial(getValue(colIdx, dataRow, "epc_serial"))
//							.location(Location.builder().locationId(locationId).build())
//							.product(Product.builder().epcProduct(epcProduct).build()).build());
//					insertedEPCs.add(epcCode);
//				}
//			}
//
//			// [8] 1차: Location/Product/EPC를 모두 먼저 저장/flush (순서 주의!)
//			try {
//				batchSave(locations, locationRepo, 1, "");
//			} catch (Exception e) {
//				saveErrors.add("Location: " + e.getMessage());
//			}
//			try {
//				batchSave(products, productRepo, 1, "");
//			} catch (Exception e) {
//				saveErrors.add("Product: " + e.getMessage());
//			}
//			try {
//				batchSave(epcs, epcRepo, 1, "");
//			} catch (Exception e) {
//				saveErrors.add("EPC: " + e.getMessage());
//			}
//
//			// [9] 2차: EventHistory는 반드시 "저장된" EPC/Location/Product를 참조!
//			List<EventHistory> events = new ArrayList<>();
//			for (int i = 0; i < allRows.size(); i++) {
//				String[] dataRow = allRows.get(i);
//				int currRowNum = i + 2;
//				Long locationId = parseLongSafe(getValue(colIdx, dataRow, "location_id"));
//				Long epcProduct = parseLongSafe(getValue(colIdx, dataRow, "epc_product"));
//				String epcCode = getValue(colIdx, dataRow, "epc_code");
//
//				// 반드시 DB에서 가져와야 함 (영속 객체)
//				Optional<Epc> epcOpt = epcRepo.findById(epcCode);
//				Optional<Location> locOpt = locationRepo.findById(locationId);
//				Optional<Product> prodOpt = productRepo.findById(epcProduct);
//
//				if (!epcOpt.isPresent() || !locOpt.isPresent() || !prodOpt.isPresent()) {
//					errorRows.computeIfAbsent("EventHistory 참조 엔티티 없음", k -> new ArrayList<>()).add(currRowNum);
//					continue;
//				}
//
//				EventHistory.EventHistoryBuilder eventBuilder = EventHistory.builder().epc(epcOpt.get())
//						.location(locOpt.get()).hubType(getValue(colIdx, dataRow, "hub_type"))
//						.eventType(getValue(colIdx, dataRow, "event_type")).fileLog(csvLog);
//
//				String businessRaw = getValue(colIdx, dataRow, "business_step");
//				String normalizedStep = normalizeBusinessStep(businessRaw);
//				if (!isNullOrEmpty(businessRaw) && normalizedStep == null) {
//					errorRows.computeIfAbsent("business_step 변환 오류", k -> new ArrayList<>()).add(currRowNum);
//				}
//				eventBuilder.businessOriginal(businessRaw).businessStep(normalizedStep);
//
//				if (!isNullOrEmpty(getValue(colIdx, dataRow, "event_time")))
//					eventBuilder.eventTime(LocalDateTime.parse(getValue(colIdx, dataRow, "event_time"), dtf));
//				if (!isNullOrEmpty(getValue(colIdx, dataRow, "manufacture_date")))
//					eventBuilder
//							.manufactureDate(LocalDateTime.parse(getValue(colIdx, dataRow, "manufacture_date"), dtf));
//				if (!isNullOrEmpty(getValue(colIdx, dataRow, "expiry_date")))
//					eventBuilder.expiryDate(LocalDate.parse(getValue(colIdx, dataRow, "expiry_date"), ymdFormatter));
//
//				events.add(eventBuilder.build());
//
//				// batch마다 저장 및 진행률 log
//				if (events.size() >= batchSize) {
//					try {
//						eventHistoryRepo.saveAll(events);
//						savedCount += events.size();
//						double percent = (savedCount * 100.0) / totalRows;
//						System.out.printf("[Batch] EventHistory 저장 성공: %d개, 진행률: %.2f%%\n", events.size(), percent);
//						events.clear();
//					} catch (Exception e) {
//						saveErrors.add("EventHistory: " + e.getMessage());
//					}
//				}
//			}
//
//			// [10] 마지막 남은 EventHistory 저장/진행률 log
//			try {
//				if (!events.isEmpty()) {
//					eventHistoryRepo.saveAll(events);
//					savedCount += events.size();
//					double percent = (savedCount * 100.0) / totalRows;
//					System.out.printf("[마지막] EventHistory 저장 성공: %d개, 진행률: %.2f%%\n", events.size(), percent);
//					events.clear();
//				}
//			} catch (Exception e) {
//				saveErrors.add("EventHistory: " + e.getMessage());
//			}
//
//			// [11] summary
//			if (!errorRows.isEmpty() || !saveErrors.isEmpty()) {
//				StringBuilder report = new StringBuilder("[CSV 저장 전체 오류 요약]\n");
//				errorRows.forEach((type, rows) -> {
//					report.append("오류[").append(type).append("]: ").append(rows.size()).append("건 rows: ").append(rows)
//							.append("\n");
//				});
//				if (!saveErrors.isEmpty()) {
//					report.append("DB 저장 실패: ").append(saveErrors).append("\n");
//				}
//				throw new RuntimeException(report.toString());
//			}
//		} catch (Exception e) {
//			throw new RuntimeException("[CsvSaveService] 파일 읽기/저장 중 오류(요약): " + e.getMessage(), e);
//		}
//	}
//
//	// ---- 공통 헬퍼 메서드 ----
//	private <T> void batchSave(List<T> list, org.springframework.data.jpa.repository.JpaRepository<T, ?> repo,
//			int batchSize, String logTitle) {
//		if (list.size() >= batchSize) {
//			repo.saveAll(list);
//			list.clear();
//		}
//	}
//
//	private String getValue(Map<String, Integer> colIdx, String[] row, String col) {
//		Integer idx = colIdx.get(col);
//		return (idx != null && idx < row.length) ? row[idx] : null;
//	}
//
//	private Long parseLongSafe(String s) {
//		try {
//			return (isNullOrEmpty(s)) ? null : Long.parseLong(s.trim());
//		} catch (Exception e) {
//			return null;
//		}
//	}
//
//	private Double parseDoubleSafe(String s) {
//		try {
//			return (isNullOrEmpty(s)) ? null : Double.parseDouble(s.trim());
//		} catch (Exception e) {
//			return null;
//		}
//	}
//
//	private boolean isNullOrEmpty(String s) {
//		return s == null || s.trim().isEmpty();
//	}
//
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
//		return null;
//	}
//}

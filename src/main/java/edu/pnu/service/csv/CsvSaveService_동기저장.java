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
//import lombok.extern.slf4j.Slf4j;
//
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class CsvSaveService_동기저장 {
//
//    private final ProductRepository productRepo;
//    private final EpcRepository epcRepo;
//    private final LocationRepository locationRepo;
//    private final EventHistoryRepository eventHistoryRepo;
//    private final CsvRepository csvRepo;
//    private final MemberRepository memberRepo;
//
//    public void postCsv(MultipartFile file, CustomUserDetails user) {
//
//        Map<String, List<Integer>> errorRows = new HashMap<>(); // ★ 오류 누적 집계용
//
//        // [1] 유저/파일 검증
//        if (user == null)
//            throw new BadRequestException("[오류] : [CsvSaveService] 로그인 정보 없음");
//        String userId = user.getUserId();
//        Member member = memberRepo.findByUserId(userId).orElseThrow(() -> new RuntimeException("회원 정보 없음"));
//
//        if (file == null || file.isEmpty())
//            throw new CsvFileNotFoundException("[오류] : [CsvSaveService] 파일을 찾지 못했음");
//
//        if (!file.getOriginalFilename().endsWith(".csv"))
//            throw new FileUploadException("[오류] : [CsvSaveService] CSV 파일 아님");
//
//        // [2] 업로드 파일 정보 저장 (Csv 로그 엔티티)
//        Csv csvLog = Csv.builder()
//                .fileName(file.getOriginalFilename())
//                .filePath("c:/MainProject/save_csv")
//                .fileSize(file.getSize())
//                .member(member).build();
//        csvLog = csvRepo.save(csvLog);
//
//        try (BufferedReader reader = new BufferedReader(
//                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
//
//            // (1) "\t(탭)" 구분자로 파싱할 CSVReader 설정
//            CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
//            CSVReader csv = new CSVReaderBuilder(reader).withCSVParser(parser).build();
//
//            // (2) 헤더 한 줄 읽어서, 컬럼 인덱스 매핑
//            String[] header = csv.readNext();
//            if (header == null)
//                throw new InvalidCsvFormatException(
//                        "[오류] : [CsvSaveService] CSV 파일 header 없음 (fileName=" + csvLog.getFileName() + ")");
//
//            // (3) 필수 컬럼 모두 포함되어 있는지 체크
//            String[] requiredColumns = { "location_id", "epc_product", "epc_code", "epc_lot", "event_type",
//                    "business_step", "event_time" };
//            for (String col : requiredColumns) {
//                boolean found = false;
//                for (String h : header) {
//                    if (col.equals(h))
//                        found = true;
//                }
//                if (!found)
//                    throw new InvalidCsvFormatException("[오류] : 필수 컬럼(" + col + ")이 없습니다.");
//            }
//
//            // (4) 컬럼명-인덱스 맵핑 생성 (가독성과 유지보수↑)
//            Map<String, Integer> colIdx = new HashMap<>();
//            for (int i = 0; i < header.length; i++) {
//                colIdx.put(header[i], i);
//            }
//
//            // (5) 날짜 포맷터 준비
//            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
//            DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");
//
//            // [4] 전체 Row 읽어서, 미리 ID 집합을 추출 (DB 중복 체크를 위해)
//            List<String[]> allRows = new ArrayList<>();
//            Set<Long> allLocationIds = new HashSet<>();
//            Set<Long> allProductIds = new HashSet<>();
//            Set<String> allEpcCodes = new HashSet<>();
//
//            String[] row;
//            int dataRowNum = 1; // CSV 1-based (헤더+1)
//            while ((row = csv.readNext()) != null) {
//                dataRowNum++;
//                // 각 엔티티별 ID 추출 (null이 아니고, 숫자만!)
//                Long locId = parseLongSafe(getValue(colIdx, row, "location_id"));
//                Long prodId = parseLongSafe(getValue(colIdx, row, "epc_product"));
//                String epcCode = getValue(colIdx, row, "epc_code");
//
//                if (locId == null) {
//                    log.error("[row {}] location_id 파싱 오류: 값이 null이거나 숫자 아님! 값={}", dataRowNum, getValue(colIdx, row, "location_id"));
//                    errorRows.computeIfAbsent("location_id 파싱 오류", k -> new ArrayList<>()).add(dataRowNum);
//                    continue;
//                }
//                if (prodId == null) {
//                    log.error("[row {}] epc_product 파싱 오류: 값이 null이거나 숫자 아님! 값={}", dataRowNum, getValue(colIdx, row, "epc_product"));
//                    errorRows.computeIfAbsent("epc_product 파싱 오류", k -> new ArrayList<>()).add(dataRowNum);
//                    continue;
//                }
//                if (isNullOrEmpty(epcCode)) {
//                    log.error("[row {}] epc_code 파싱 오류: 값이 null 또는 빈 문자열!", dataRowNum);
//                    errorRows.computeIfAbsent("epc_code 파싱 오류", k -> new ArrayList<>()).add(dataRowNum);
//                    continue;
//                }
//
//                allRows.add(row);
//                allLocationIds.add(locId);
//                allProductIds.add(prodId);
//                allEpcCodes.add(epcCode);
//            }
//
//            // [5] DB에 이미 존재하는 ID 전체 조회
//            Set<Long> existLocationIds = locationRepo.findAllById(allLocationIds)
//                    .stream().map(Location::getLocationId).collect(Collectors.toSet());
//            Set<Long> existProductIds = productRepo.findAllById(allProductIds)
//                    .stream().map(Product::getEpcProduct).collect(Collectors.toSet());
//            Set<String> existEpcCodes = epcRepo.findAllById(allEpcCodes)
//                    .stream().map(Epc::getEpcCode).collect(Collectors.toSet());
//
//            Set<Long> insertedLocations = new HashSet<>(existLocationIds);
//            Set<Long> insertedProducts = new HashSet<>(existProductIds);
//            Set<String> insertedEPCs = new HashSet<>(existEpcCodes);
//
//            List<Location> locations = new ArrayList<>();
//            List<Product> products = new ArrayList<>();
//            List<Epc> epcs = new ArrayList<>();
//            List<EventHistory> events = new ArrayList<>();
//
//            int batchSize = 3000;
//            int totalRows = allRows.size();
//            int savedCount = 0;
//
//            // [6] Row별로 객체 생성 및 배치 저장
//            for (int i = 0; i < allRows.size(); i++) {
//                String[] dataRow = allRows.get(i);
//                int rowNum = i + 2;
//                final String errorRow = "[row " + rowNum + "]";
//
//                try {
//                    // 1) 필수값 파싱 (아래도 한 번 더 체크, 안전하게!)
//                    Long locationId = parseLongSafe(getValue(colIdx, dataRow, "location_id"));
//                    Long epcProduct = parseLongSafe(getValue(colIdx, dataRow, "epc_product"));
//                    String epcCode = getValue(colIdx, dataRow, "epc_code");
//                    if (locationId == null || epcProduct == null || isNullOrEmpty(epcCode)) {
//                        log.error("{} 필수값 누락! locationId={}, epcProduct={}, epcCode={}", errorRow, locationId, epcProduct, epcCode);
//                        errorRows.computeIfAbsent("필수값 누락", k -> new ArrayList<>()).add(rowNum);
//                        continue;
//                    }
//
//                    // [A] Location 생성 및 중복 저장 방지
//                    if (!insertedLocations.contains(locationId)) {
//                        locations.add(Location.builder()
//                                .locationId(locationId)
//                                .scanLocation(getValue(colIdx, dataRow, "scan_location"))
//                                .latitude(parseDoubleSafe(getValue(colIdx, dataRow, "latitude")))
//                                .longitude(parseDoubleSafe(getValue(colIdx, dataRow, "longitude")))
//                                .build());
//                        insertedLocations.add(locationId);
//                    }
//
//                    // [B] Product 생성 및 중복 저장 방지
//                    if (!insertedProducts.contains(epcProduct)) {
//                        products.add(Product.builder()
//                                .epcProduct(epcProduct)
//                                .productName(getValue(colIdx, dataRow, "product_name"))
//                                .build());
//                        insertedProducts.add(epcProduct);
//                    }
//
//                    // [C] EPC 생성 및 중복 저장 방지
//                    if (!insertedEPCs.contains(epcCode)) {
//                        epcs.add(Epc.builder()
//                                .epcCode(epcCode)
//                                .epcCompany(parseLongSafe(getValue(colIdx, dataRow, "epc_company")))
//                                .epcLot(parseLongSafe(getValue(colIdx, dataRow, "epc_lot")))
//                                .epcSerial(getValue(colIdx, dataRow, "epc_serial"))
//                                .location(Location.builder().locationId(locationId).build())
//                                .product(Product.builder().epcProduct(epcProduct).build())
//                                .build());
//                        insertedEPCs.add(epcCode);
//                    }
//
//                    // [D] EventHistory(이벤트 기록)는 중복 없이 모두 저장
//                    EventHistory.EventHistoryBuilder eventBuilder = EventHistory.builder();
//                    eventBuilder
//                            .epc(Epc.builder().epcCode(epcCode).build())
//                            .location(Location.builder().locationId(locationId).build())
//                            .hubType(getValue(colIdx, dataRow, "hub_type"))
//                            .eventType(getValue(colIdx, dataRow, "event_type"))
//                            .fileLog(csvLog);
//
//                    String businessRaw = getValue(colIdx, dataRow, "business_step");
//                    if (!isNullOrEmpty(businessRaw)) {
//                        eventBuilder.businessOriginal(businessRaw)
//                                .businessStep(normalizeBusinessStep(businessRaw));
//                    }
//                    if (!isNullOrEmpty(getValue(colIdx, dataRow, "event_time")))
//                        eventBuilder.eventTime(LocalDateTime.parse(getValue(colIdx, dataRow, "event_time"), dtf));
//                    if (!isNullOrEmpty(getValue(colIdx, dataRow, "manufacture_date")))
//                        eventBuilder.manufactureDate(LocalDateTime.parse(getValue(colIdx, dataRow, "manufacture_date"), dtf));
//                    if (!isNullOrEmpty(getValue(colIdx, dataRow, "expiry_date")))
//                        eventBuilder.expiryDate(LocalDate.parse(getValue(colIdx, dataRow, "expiry_date"), ymdFormatter));
//
//                    events.add(eventBuilder.build());
//
//                    // [E] 배치 저장: 일정 개수마다 저장
//                    batchSave(locations, locationRepo, batchSize, errorRow + " Location");
//                    batchSave(products, productRepo, batchSize, errorRow + " Product");
//                    batchSave(epcs, epcRepo, batchSize, errorRow + " EPC");
//
//                    if (events.size() >= batchSize) {
//                        try {
//                            eventHistoryRepo.saveAll(events);
//                            savedCount += events.size();
//                            double percent = (savedCount * 100.0) / totalRows;
//                            log.info("[Batch] EventHistory 저장 성공: {}개, 진행률: {}%", events.size(), String.format("%.2f", percent));
//                            events.clear();
//                        } catch (Exception e) {
//                            log.error("{} EventHistory insert 오류: {}", errorRow, e.getMessage());
//                            errorRows.computeIfAbsent(e.getMessage(), k -> new ArrayList<>()).add(rowNum);
//                        }
//                    }
//                } catch (Exception ex) {
//                    log.error("{} Insert 오류: {}", errorRow, ex.getMessage());
//                    errorRows.computeIfAbsent(ex.getMessage(), k -> new ArrayList<>()).add(rowNum);
//                }
//            }
//
//            // [7] 마지막 남은 데이터 저장
//            batchSave(locations, locationRepo, 1, "마지막 Location");
//            batchSave(products, productRepo, 1, "마지막 Product");
//            batchSave(epcs, epcRepo, 1, "마지막 EPC");
//            if (!events.isEmpty()) {
//                try {
//                    eventHistoryRepo.saveAll(events);
//                    savedCount += events.size();
//                    double percent = (savedCount * 100.0) / totalRows;
//                    log.info("[마지막] EventHistory 저장 성공: {}개, 진행률: {}%", events.size(), String.format("%.2f", percent));
//                } catch (Exception e) {
//                    log.error("마지막 EventHistory insert 오류: {}", e.getMessage());
//                    errorRows.computeIfAbsent(e.getMessage(), k -> new ArrayList<>()).add(totalRows + 1);
//                }
//            }
//
//            // [8] 오류 요약 리포트
//            if (!errorRows.isEmpty()) {
//                log.warn("[CSV 저장] 전체 오류 요약 -------");
//                for (Map.Entry<String, List<Integer>> entry : errorRows.entrySet()) {
//                    log.warn("오류 [{}] : {}건 (rows: {})",
//                            entry.getKey(), entry.getValue().size(), entry.getValue());
//                }
//            }
//
//        } catch (Exception e) {
//            throw new RuntimeException("[CsvSaveService] 파일 읽기/저장 중 오류: " + e.getMessage(), e);
//        }
//
//    }
//
//    // ==================== 헬퍼 메서드 구간 ====================
//
//    /** 일정 개수(batchSize)마다 리스트를 DB에 저장하는 공통 메서드 */
//    private <T> void batchSave(List<T> list, org.springframework.data.jpa.repository.JpaRepository<T, ?> repo, int batchSize, String logTitle) {
//        if (list.size() >= batchSize) {
//            try {
//                repo.saveAll(list);
//                list.clear();
//            } catch (Exception e) {
//                System.err.println(logTitle + " insert 오류: " + e.getMessage());
//            }
//        }
//    }
//
//    /** 컬럼명으로 값 안전하게 추출 */
//    private String getValue(Map<String, Integer> colIdx, String[] row, String col) {
//        Integer idx = colIdx.get(col);
//        return (idx != null && idx < row.length) ? row[idx] : null;
//    }
//
//    /** String → Long 안전 변환(빈 값이면 null) */
//    private Long parseLongSafe(String s) {
//        try {
//            return (isNullOrEmpty(s)) ? null : Long.parseLong(s.trim());
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    /** String → Double 안전 변환(빈 값이면 null) */
//    private Double parseDoubleSafe(String s) {
//        try {
//            return (isNullOrEmpty(s)) ? null : Double.parseDouble(s.trim());
//        } catch (Exception e) {
//            return null;
//        }
//    }
//
//    /** null 또는 빈 문자열 체크 */
//    private boolean isNullOrEmpty(String s) {
//        return s == null || s.trim().isEmpty();
//    }
//
//    /** business_step 값 정규화 */
//    private String normalizeBusinessStep(String input) {
//        if (input == null) return null;
//        input = input.trim().toLowerCase();
//        if (input.contains("factory")) return "Factory";
//        if (input.contains("wms")) return "WMS";
//        if (input.contains("logistics_hub") || input.contains("logi") || input.contains("hub")) return "LogiHub";
//        if (input.startsWith("w_stock")) return "Wholesaler";
//        if (input.startsWith("r_stock")) return "Reseller";
//        if (input.contains("pos")) return "POS";
//        return null;
//    }
//}

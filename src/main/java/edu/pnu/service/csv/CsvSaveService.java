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
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import edu.pnu.Repo.CsvRepository;
import edu.pnu.Repo.EpcRepository;
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
    private final BatchService batchService; // JdbcTemplate batch insert

    /**
     * CSV 업로드 후 DB 일괄 저장 (최적화 버전)
     */
    public void postCsv(MultipartFile file, CustomUserDetails user) {
        // [1] 파일/사용자 유효성 검사
        if (user == null) throw new BadRequestException("[오류] : [CsvSaveService] 로그인 정보 없음");
        String userId = user.getUserId();
        Member member = memberRepo.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("회원 정보 없음"));
        if (file == null || file.isEmpty()) throw new CsvFileNotFoundException("[오류] : [CsvSaveService] 파일을 찾지 못했음");
        if (!file.getOriginalFilename().endsWith(".csv")) throw new FileUploadException("[오류] : [CsvSaveService] CSV 파일 아님");

        // [2] 업로드 파일 정보(Csv 엔티티) 저장 (업로드 이력)
        Csv csvLog = Csv.builder()
                .fileName(file.getOriginalFilename())
                .filePath("c:/MainProject/save_csv")
                .fileSize(file.getSize())
                .member(member)
                .build();
        csvLog = csvRepo.save(csvLog);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            log.info("[체크] CSV 파싱 진입: 파일명 = {}", file.getOriginalFilename());

            // [3] CSV 파일 파싱 (탭 구분자)
            com.opencsv.CSVParser parser = new com.opencsv.CSVParserBuilder().withSeparator('\t').build();
            com.opencsv.CSVReader csv = new com.opencsv.CSVReaderBuilder(reader).withCSVParser(parser).build();

            // [4] 헤더 파싱, 필수 컬럼 체크
            String[] header = csv.readNext();
            if (header == null) throw new InvalidCsvFormatException("[오류] : [CsvSaveService] CSV 파일 header 없음 (fileName=" + csvLog.getFileName() + ")");
            String[] requiredColumns = {
                    "location_id", "epc_product", "epc_code", "epc_lot", "event_type",
                    "business_step", "event_time"
            };
            for (String col : requiredColumns) {
                if (Arrays.stream(header).noneMatch(col::equals))
                    throw new InvalidCsvFormatException("[오류] : 필수 컬럼(" + col + ")이 없습니다.");
            }
            Map<String, Integer> colIdx = new HashMap<>();
            for (int i = 0; i < header.length; i++) colIdx.put(header[i], i);

            // [5] 날짜 포맷
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            // [6] 모든 row를 한 번에 읽고, 데이터만 추출 (중복 체크용 집합도 생성)
            List<String[]> allRows = new ArrayList<>();
            Set<Long> allLocationIds = new HashSet<>();
            Set<Long> allProductIds = new HashSet<>();
            Set<String> allEpcCodes = new HashSet<>();
            List<Integer> rowNumList = new ArrayList<>(); // 실제 row번호 추적

            String[] row;
            int currRowNum = 1; // 헤더 1, 데이터 2부터
            while ((row = csv.readNext()) != null) {
                currRowNum++;
                allRows.add(row);
                rowNumList.add(currRowNum);
                allLocationIds.add(parseLongSafe(getValue(colIdx, row, "location_id")));
                allProductIds.add(parseLongSafe(getValue(colIdx, row, "epc_product")));
                allEpcCodes.add(getValue(colIdx, row, "epc_code"));
            }

            // [7] DB에 이미 존재하는 데이터 미리 조회 (중복 insert 방지)
            Set<Long> existLocationIds = locationRepo.findAllById(allLocationIds)
                    .stream().map(Location::getLocationId).collect(Collectors.toSet());
            Set<Long> existProductIds = productRepo.findAllById(allProductIds)
                    .stream().map(Product::getEpcProduct).collect(Collectors.toSet());
            Set<String> existEpcCodes = epcRepo.findAllById(allEpcCodes)
                    .stream().map(Epc::getEpcCode).collect(Collectors.toSet());

            // 중복 체크용 집합
            Set<Long> insertedLocations = new HashSet<>(existLocationIds);
            Set<Long> insertedProducts = new HashSet<>(existProductIds);
            Set<String> insertedEPCs = new HashSet<>(existEpcCodes);

            // [8] 저장할 엔티티들을 리스트로 쭉~ 모음
            List<Location> locations = new ArrayList<>();
            List<Product> products = new ArrayList<>();
            List<Epc> epcs = new ArrayList<>();
            List<EventHistory> events = new ArrayList<>();

            // [9] row별 엔티티 변환만 수행 (중복 insert 방지, 진단/분석 컬럼 모두 반영)
            Map<String, List<Integer>> errorRows = new HashMap<>(); // row별 오류 추적
            for (int i = 0; i < allRows.size(); i++) {
                String[] dataRow = allRows.get(i);
                int rowNum = rowNumList.get(i);

                try {
                    // [A] Location (중복 저장 방지)
                    Long locationId = parseLongSafe(getValue(colIdx, dataRow, "location_id"));
                    if (locationId != null && !insertedLocations.contains(locationId)) {
                        locations.add(Location.builder()
                                .locationId(locationId)
                                .scanLocation(getValue(colIdx, dataRow, "scan_location"))
                                .latitude(parseDoubleSafe(getValue(colIdx, dataRow, "latitude")))
                                .longitude(parseDoubleSafe(getValue(colIdx, dataRow, "longitude")))
                                .build());
                        insertedLocations.add(locationId);
                    }
                    // [B] Product (중복 저장 방지)
                    Long epcProduct = parseLongSafe(getValue(colIdx, dataRow, "epc_product"));
                    if (epcProduct != null && !insertedProducts.contains(epcProduct)) {
                        products.add(Product.builder()
                                .epcProduct(epcProduct)
                                .productName(getValue(colIdx, dataRow, "product_name"))
                                .build());
                        insertedProducts.add(epcProduct);
                    }
                    // [C] Epc (중복 저장 방지)
                    String epcCode = getValue(colIdx, dataRow, "epc_code");
                    if (epcCode != null && !insertedEPCs.contains(epcCode)) {
                        epcs.add(Epc.builder()
                                .epcCode(epcCode)
                                .epcHeader(getValue(colIdx, dataRow, "epcHeader"))
                                .epcCompany(parseLongSafe(getValue(colIdx, dataRow, "epc_company")))
                                .epcLot(parseLongSafe(getValue(colIdx, dataRow, "epc_lot")))
                                .epcSerial(getValue(colIdx, dataRow, "epc_serial"))
                                .location(Location.builder().locationId(locationId).build())
                                .product(Product.builder().epcProduct(epcProduct).build())
                                .build());
                        insertedEPCs.add(epcCode);
                    }
                    // [D] EventHistory 생성 (진단/분석 컬럼, null-safe 변환)
                    EventHistory.EventHistoryBuilder eventBuilder = EventHistory.builder()
                        .epc(Epc.builder().epcCode(epcCode).build())
                        .location(Location.builder().locationId(locationId).build())
                        .hubType(getValue(colIdx, dataRow, "hub_type"))
                        .businessStep(getValue(colIdx, dataRow, "business_step"))
                        .businessOriginal(getValue(colIdx, dataRow, "business_original"))
                        .eventType(getValue(colIdx, dataRow, "event_type"))
                        .fileLog(csvLog);

                    try { // eventTime: LocalDateTime
                        String v = getValue(colIdx, dataRow, "event_time");
                        if (!isNullOrEmpty(v))
                            eventBuilder.eventTime(LocalDateTime.parse(v, dtf));
                    } catch (Exception ex) {
                        errorRows.computeIfAbsent("event_time 파싱 오류", k -> new ArrayList<>()).add(rowNum);
                    }
                    try { // manufactureDate: LocalDateTime
                        String v = getValue(colIdx, dataRow, "manufacture_date");
                        if (!isNullOrEmpty(v))
                            eventBuilder.manufactureDate(LocalDateTime.parse(v, dtf));
                    } catch (Exception ex) {
                        errorRows.computeIfAbsent("manufacture_date 파싱 오류", k -> new ArrayList<>()).add(rowNum);
                    }
                    try { // expiryDate: LocalDate
                        String v = getValue(colIdx, dataRow, "expiry_date");
                        if (!isNullOrEmpty(v))
                            eventBuilder.expiryDate(LocalDate.parse(v, ymdFormatter));
                    } catch (Exception ex) {
                        errorRows.computeIfAbsent("expiry_date 파싱 오류", k -> new ArrayList<>()).add(rowNum);
                    }

                    // 진단/분석 컬럼들 (값 없으면 false/0.0)
                    eventBuilder.anomaly(parseBooleanSafe(getValue(colIdx, dataRow, "anomaly")));
                    eventBuilder.jump(parseBooleanSafe(getValue(colIdx, dataRow, "jump")));
                    eventBuilder.jumpScore(parseDoubleSafe(getValue(colIdx, dataRow, "jumpScore")));
                    eventBuilder.evtOrderErr(parseBooleanSafe(getValue(colIdx, dataRow, "evtOrderErr")));
                    eventBuilder.evtOrderErrScore(parseDoubleSafe(getValue(colIdx, dataRow, "evtOrderErrScore")));
                    eventBuilder.epcFake(parseBooleanSafe(getValue(colIdx, dataRow, "epcFake")));
                    eventBuilder.epcFakeScore(parseDoubleSafe(getValue(colIdx, dataRow, "epcFakeScore")));
                    eventBuilder.epcDup(parseBooleanSafe(getValue(colIdx, dataRow, "epcDup")));
                    eventBuilder.epcDupScore(parseDoubleSafe(getValue(colIdx, dataRow, "epcDupScore")));
                    eventBuilder.locErr(parseBooleanSafe(getValue(colIdx, dataRow, "locErr")));
                    eventBuilder.locErrScore(parseDoubleSafe(getValue(colIdx, dataRow, "locErrScore")));

                    events.add(eventBuilder.build());

                } catch (Exception ex) {
                    errorRows.computeIfAbsent("row별 Insert 오류", k -> new ArrayList<>()).add(rowNum);
                    log.error("[row {}] Insert 오류: {}", rowNum, ex.getMessage());
                }
            }

            // =====================
            // [10] 모든 마스터 엔티티를 '딱 한 번' saveAll로 저장!
            // =====================
            locationRepo.saveAll(locations);
            productRepo.saveAll(products);
            epcRepo.saveAll(epcs);

            // [11] EventHistory는 JdbcTemplate batchInsert로 대량 저장 (빠름)
            int savedCount = 0;
            try {
                batchService.batchInsertEventHistories(events);
                savedCount = events.size();
            } catch (Exception e) {
                errorRows.computeIfAbsent("EventHistory batch insert 오류", k -> new ArrayList<>()).add(-1);
                log.error("EventHistory batch insert 오류: {}", e.getMessage());
            }

            // [12] 에러 summary
            if (!errorRows.isEmpty()) {
                StringBuilder report = new StringBuilder("[CSV 저장 전체 오류 요약]\n");
                errorRows.forEach((type, rows) -> {
                    report.append("오류[").append(type).append("]: ").append(rows.size()).append("건 rows: ").append(rows).append("\n");
                });
                throw new RuntimeException(report.toString());
            }

            log.info("[CSV 저장 성공] 전체 {}건", savedCount);
        } catch (Exception e) {
            log.error("[CsvSaveService] 파일 읽기/저장 중 오류", e);
            throw new RuntimeException("[CsvSaveService] 파일 읽기/저장 중 오류: " + e.getMessage(), e);
        }
    }

    // ----- 헬퍼 메서드 -----
    private String getValue(Map<String, Integer> colIdx, String[] row, String col) {
        Integer idx = colIdx.get(col);
        return (idx != null && idx < row.length) ? row[idx] : null;
    }
    private Long parseLongSafe(String s) {
        try { return (isNullOrEmpty(s)) ? null : Long.parseLong(s.trim()); }
        catch (Exception e) { return null; }
    }
    private Double parseDoubleSafe(String s) {
        try { return (isNullOrEmpty(s)) ? 0.0 : Double.parseDouble(s.trim()); }
        catch (Exception e) { return 0.0; }
    }
    private boolean parseBooleanSafe(String s) {
        if (isNullOrEmpty(s)) return false;
        return "1".equals(s.trim()) || "true".equalsIgnoreCase(s.trim());
    }
    private boolean isNullOrEmpty(String s) {
        return s == null || s.trim().isEmpty();
    }
}
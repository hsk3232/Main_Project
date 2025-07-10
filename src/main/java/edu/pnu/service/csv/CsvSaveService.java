package edu.pnu.service.csv;

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

import edu.pnu.Repo.CsvRepository;
import edu.pnu.Repo.EPCRepository;
import edu.pnu.Repo.EventHistoryRepository;
import edu.pnu.Repo.LocationRepository;
import edu.pnu.Repo.MemberRepository;
import edu.pnu.Repo.ProductRepository;
import edu.pnu.domain.Csv;
import edu.pnu.domain.EPC;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;
import edu.pnu.domain.Member;
import edu.pnu.domain.Product;
import edu.pnu.exception.BadRequestException;
import edu.pnu.exception.FileNotFoundException;
import edu.pnu.exception.FileUploadException;
import edu.pnu.exception.InvalidCsvFormatException;
import edu.pnu.service.member.CustomUserDetails;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CsvSaveService {

	// --- Repository 주입 (DB와 연동) ---
	private final ProductRepository productRepo;
	private final EPCRepository epcRepo;
	private final LocationRepository locationRepo;
	private final EventHistoryRepository eventHistoryRepo;
	private final CsvRepository csvRepo; // ✨추가: 파일 로그 저장용
	private final MemberRepository memberRepo;

	/**
     * [기능 요약]
     * - TSV(탭 구분) CSV 파일을 파싱하여 여러 테이블에 데이터 저장
     * - 파일 유효성, 사용자 정보 등 검증
     * - 각종 예외 발생시 RuntimeException 혹은 커스텀 예외로 throw
     */
    public void postCsv(MultipartFile file, CustomUserDetails user) {
    	
    	// 1. 로그인/유저 검증
        if (user == null) throw new BadRequestException("[오류] : [CsvSaveService] 로그인 정보 없음");
        String userId = user.getUserId();
        Member member = memberRepo.findByUserId(userId)
        		.orElseThrow(() -> new RuntimeException("회원 정보 없음"));

        // 2. 파일 존재/형식 체크
        if (file == null || file.isEmpty()) throw new FileNotFoundException("[오류] : [CsvSaveService] 파일을 찾지 못했음");
        if (!file.getOriginalFilename().endsWith(".csv")) throw new FileUploadException("[오류] : [CsvSaveService] CSV 파일 아님");
    	

        // 3. 파일 로그(Csv 엔티티) 저장
        Csv csvLog = Csv.builder()
            .fileName(file.getOriginalFilename())
            .filePath("c:/MainProject/save_csv") // 실제 경로 지정 필요
            .fileSize(file.getSize())
            .member(member)
            .build();
        csvLog = csvRepo.save(csvLog);

        // 4. 파일 파싱 및 데이터 저장 (checked exception 직접 처리!)
        try (
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))
        ) {
            CSVParser parser = new CSVParserBuilder().withSeparator('\t').build();
            CSVReader csv = new CSVReaderBuilder(reader).withCSVParser(parser).build();

            
            // 5. 헤더 검증
            String[] header = csv.readNext();
            if (header == null) 
            	throw new InvalidCsvFormatException("[오류] : [CsvSaveService] CSV 파일 header 없음");
            
            // 5-1. 필수 컬럼 체크
            String[] requiredColumns = {"location_id", "epc_product", "epc_code", "epc_lot", "event_type", "business_step", "event_time"};
            for (String col : requiredColumns) {
                boolean found = false;
                for (String h : header) {
                    if (col.equals(h)) found = true;
                }
                if (!found) throw new InvalidCsvFormatException("[오류] : 필수 컬럼(" + col + ")이 없습니다.");
            }
            
            Map<String, Integer> colIdx = new HashMap<>();
            for (int i = 0; i < header.length; i++) {
                colIdx.put(header[i], i);
            }

            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            DateTimeFormatter ymdFormatter = DateTimeFormatter.ofPattern("yyyyMMdd");

            Long userLocationId = user.getLocationId();
            
            
            // 6. 전체 row 읽어서 메모리에 보관
            List<String[]> allRows = new ArrayList<>();
            String[] row;
            while ((row = csv.readNext()) != null) {
                allRows.add(row);
            }
            
            // 7. row 유효성 검증 (필수값, 숫자, Factory-location_id)
            for (int i = 0; i < allRows.size(); i++) {
                String[] r = allRows.get(i);
                int rowNum = i + 2; // 헤더가 1번줄, 데이터는 2번줄부터 시작

                // ------ 1. 필수값 비어있는지 체크 ------
                for (String col : requiredColumns) {
                    if (r[colIdx.get(col)].isEmpty()) {
                        throw new InvalidCsvFormatException(
                            "[" + rowNum + "행] 필수값(" + col + ")이 비어 있습니다."
                        );
                    }
                }

            // ------ 2. 숫자 컬럼 타입 체크 ------
            try {
                Long.parseLong(r[colIdx.get("location_id")]);
            } catch (NumberFormatException e) {
                throw new InvalidCsvFormatException(
                    "[" + rowNum + "행] location_id가 숫자가 아닙니다: " + r[colIdx.get("location_id")]);
            }
            try {
                Long.parseLong(r[colIdx.get("epc_product")]);
            } catch (NumberFormatException e) {
                throw new InvalidCsvFormatException(
                    "[" + rowNum + "행] epc_product가 숫자가 아닙니다: " + r[colIdx.get("epc_product")]);
            }
            try {
                Long.parseLong(r[colIdx.get("epc_lot")]);
            } catch (NumberFormatException e) {
                throw new InvalidCsvFormatException(
                    "[" + rowNum + "행] epc_lot가 숫자가 아닙니다: " + r[colIdx.get("epc_lot")]);
            }

            // ------ 3. "Factory" business_step location_id 불일치 체크 ------
            String businessStep = r[colIdx.get("business_step")];
            Long locationId = Long.parseLong(r[colIdx.get("location_id")]);
            if ("Factory".equalsIgnoreCase(businessStep) && !locationId.equals(userLocationId)) {
                throw new FileUploadException(
                    "[오류] : [CsvSaveService] business_step이 'Factory'인 데이터의 location_id가 사용자와 다릅니다. " +
                    "(row " + rowNum + ", 파일 location_id: " + locationId + ", 사용자 location_id: " + userLocationId + ")"
                );
            }
        }

            

         // 3. 본격적인 데이터 저장 (원래대로)
            Set<Long> insertedLocations = new HashSet<>();
            Set<Long> insertedProducts = new HashSet<>();
            Set<String> insertedEPCs = new HashSet<>();
            
            List<Location> locations = new ArrayList<>();
            List<Product> products = new ArrayList<>();
            List<EPC> epcs = new ArrayList<>();
            List<EventHistory> events = new ArrayList<>();
            
         
            int batchSize = 1000;

            for (int i = 0; i < allRows.size(); i++) {
                String[] dataRow = allRows.get(i);
                int rowNum = i + 2;

                final String errorRow = "[row " + rowNum + "]";


                try {
                    // [1] Location (중복 insert 방지)
                    Long locationId = Long.parseLong(dataRow[colIdx.get("location_id")]);
                    if (!insertedLocations.contains(locationId)) {
                        Location location = new Location();
                        location.setLocationId(locationId);
                        if (colIdx.containsKey("scan_location"))
                            location.setScanLocation(dataRow[colIdx.get("scan_location")]);
                        locations.add(location);
                        insertedLocations.add(locationId);
                    }
                    

                    // [2] Product (중복 insert 방지)
                    Long epcProduct = Long.parseLong(dataRow[colIdx.get("epc_product")]);
                    if (!insertedProducts.contains(epcProduct)) {
                        Product product = new Product();
                        product.setEpcProduct(epcProduct);
                        if (colIdx.containsKey("product_name"))
                            product.setProductName(dataRow[colIdx.get("product_name")]);
                        products.add(product);
                        insertedProducts.add(epcProduct);
                    }
                    

                    // [3] EPC (중복 insert 방지)
                    String epcCode = dataRow[colIdx.get("epc_code")];
                    if (!insertedEPCs.contains(epcCode)) {
                        EPC epc = new EPC();
                        epc.setEpcCode(epcCode);
                        if (colIdx.containsKey("epc_company"))
                            epc.setEpcCompany(dataRow[colIdx.get("epc_company")]);
                        if (colIdx.containsKey("epc_lot"))
                            epc.setEpcLot(Long.parseLong(dataRow[colIdx.get("epc_lot")]));
                        if (colIdx.containsKey("epc_serial"))
                            epc.setEpcSerial(dataRow[colIdx.get("epc_serial")]);
                        // FK: Product 연결
                        Product prodFK = new Product();
                        prodFK.setEpcProduct(epcProduct);
                        epc.setProduct(prodFK);
                        epcs.add(epc);
                        insertedEPCs.add(epcCode);
                    }
                    

                    // [4] EventHistory (중복 없음)
                    EventHistory event = new EventHistory();
                    if (colIdx.containsKey("event_id") && !dataRow[colIdx.get("event_id")].isEmpty())
                        event.setEventId(Long.parseLong(dataRow[colIdx.get("event_id")]));
                    EPC epcFK = new EPC();
                    epcFK.setEpcCode(epcCode);
                    event.setEpc(epcFK);
                    Location locFK = new Location();
                    locFK.setLocationId(locationId);
                    event.setLocation(locFK);
                    if (colIdx.containsKey("hub_type"))
                        event.setHubType(dataRow[colIdx.get("hub_type")]);
                    if (colIdx.containsKey("business_step"))
                        event.setBusinessStep(dataRow[colIdx.get("business_step")]);
                    if (colIdx.containsKey("event_type"))
                        event.setEventType(dataRow[colIdx.get("event_type")]);
                    if (colIdx.containsKey("event_time") && !dataRow[colIdx.get("event_time")].isEmpty())
                        event.setEventTime(LocalDateTime.parse(dataRow[colIdx.get("event_time")], dtf));
                    if (colIdx.containsKey("manufacture_date") && !dataRow[colIdx.get("manufacture_date")].isEmpty())
                        event.setManufactureDate(
                            LocalDateTime.parse(dataRow[colIdx.get("manufacture_date")], dtf).toLocalDate());
                    if (colIdx.containsKey("expiry_date") && !dataRow[colIdx.get("expiry_date")].isEmpty())
                        event.setExpiryDate(LocalDate.parse(dataRow[colIdx.get("expiry_date")], ymdFormatter));
                    event.setFileLog(csvLog);
                    events.add(event);
                    

                    // [5] 배치 저장 (batchSize마다)
                    if (locations.size() >= batchSize) {
                        try { locationRepo.saveAll(locations); } catch (Exception e) {
                            System.err.println(errorRow + " Location insert 오류: " + e.getMessage());
                        } locations.clear();
                    }
                    if (products.size() >= batchSize) {
                        try { productRepo.saveAll(products); } catch (Exception e) {
                            System.err.println(errorRow + " Product insert 오류: " + e.getMessage());
                        } products.clear();
                    }
                    if (epcs.size() >= batchSize) {
                        try { epcRepo.saveAll(epcs); } catch (Exception e) {
                            System.err.println(errorRow + " EPC insert 오류: " + e.getMessage());
                        } epcs.clear();
                    }
                    if (events.size() >= batchSize) {
                        try { eventHistoryRepo.saveAll(events); } catch (Exception e) {
                            System.err.println(errorRow + " EventHistory insert 오류: " + e.getMessage());
                        } events.clear();
                    }
                } catch (Exception ex) {
                    System.err.println(errorRow + " Insert 오류: " + ex.getMessage());
                }
            }
            // [6] 남은 데이터 마지막 저장
            if (!locations.isEmpty()) {
                try { locationRepo.saveAll(locations); } catch (Exception e) {
                    System.err.println("마지막 Location insert 오류: " + e.getMessage());
                }
            }
            if (!products.isEmpty()) {
                try { productRepo.saveAll(products); } catch (Exception e) {
                    System.err.println("마지막 Product insert 오류: " + e.getMessage());
                }
            }
            if (!epcs.isEmpty()) {
                try { epcRepo.saveAll(epcs); } catch (Exception e) {
                    System.err.println("마지막 EPC insert 오류: " + e.getMessage());
                }
            }
            if (!events.isEmpty()) {
                try { eventHistoryRepo.saveAll(events); } catch (Exception e) {
                    System.err.println("마지막 EventHistory insert 오류: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            // 모든 파싱 및 IO 예외, 예측 불가 런타임 에러는 이 catch에서 한 번에 처리
            throw new RuntimeException("[CsvSaveService] 파일 읽기/저장 중 오류: " + e.getMessage(), e);
        }
    }
}
package edu.pnu.service.csv;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;
import edu.pnu.domain.Product;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class CsvSaveBatchService {

	private final JdbcTemplate jdbcTemplate;

	/**
	 * eventhistory 테이블에 맞춘 batch insert
	 */
    
    public void saveLocations(List<Location> locations) {
        if (locations.isEmpty()) return;
        String sql = "INSERT IGNORE INTO location (location_id, scan_location, latitude, longitude) VALUES (?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Location l = locations.get(i);
                ps.setLong(1, l.getLocationId());
                ps.setString(2, l.getScanLocation());
                ps.setDouble(3, l.getLatitude());
                ps.setDouble(4, l.getLongitude());
            }
            public int getBatchSize() { return locations.size(); }
        });
        log.info("Location batch insert 완료! 저장 건수: {}", locations.size());
    }

    /** Product batch insert */
   
    public void saveProducts(List<Product> products) {
        if (products.isEmpty()) return;
        String sql = "INSERT IGNORE INTO product (epc_product, product_name) VALUES (?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Product p = products.get(i);
                ps.setLong(1, p.getEpcProduct());
                ps.setString(2, p.getProductName());
            }
            public int getBatchSize() { return products.size(); }
        });
        log.info("Product batch insert 완료! 저장 건수: {}", products.size());
    }

    /** Epc batch insert */
  
    public void saveEpcs(List<Epc> epcs) {
        if (epcs.isEmpty()) return;
        String sql = "INSERT IGNORE INTO epc (epc_code, epc_header, epc_company, epc_lot, epc_serial, location_id, epc_product) VALUES (?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Epc e = epcs.get(i);
                ps.setString(1, e.getEpcCode());
                ps.setString(2, e.getEpcHeader());
                ps.setObject(3, e.getEpcCompany());
                ps.setObject(4, e.getEpcLot());
                ps.setString(5, e.getEpcSerial());
                ps.setObject(6, e.getLocation() != null ? e.getLocation().getLocationId() : null);
                ps.setObject(7, e.getProduct() != null ? e.getProduct().getEpcProduct() : null);
            }
            public int getBatchSize() { return epcs.size(); }
        });
        log.info("Epc batch insert 완료! 저장 건수: {}", epcs.size());
    }

    /** EventHistory batch insert */
    
    public void saveEventHistories(List<EventHistory> eventHistories) {
        if (eventHistories.isEmpty()) return;

        String sql = "INSERT INTO eventhistory (epc_code, location_id, hub_type, business_step, business_original, event_type, event_time, manufacture_date, expiry_date, file_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";


        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EventHistory ev = eventHistories.get(i);

                // [1] FK: epc_code
                ps.setString(1, ev.getEpc() != null ? ev.getEpc().getEpcCode() : null);

                // [2] FK: location_id
                ps.setObject(2, ev.getLocation() != null ? ev.getLocation().getLocationId() : null);

                // [3] hub_type
                ps.setString(3, ev.getHubType());

                // [4] business_step
                ps.setString(4, ev.getBusinessStep());

                // [5] business_original
                ps.setString(5, ev.getBusinessOriginal());

                // [6] event_type
                ps.setString(6, ev.getEventType());

                // [7] event_time
                if (ev.getEventTime() != null)
                    ps.setTimestamp(7, java.sql.Timestamp.valueOf(ev.getEventTime()));
                else
                    ps.setNull(7, Types.TIMESTAMP);

                // [8] manufacture_date
                if (ev.getManufactureDate() != null)
                    ps.setTimestamp(8, java.sql.Timestamp.valueOf(ev.getManufactureDate()));
                else
                    ps.setNull(8, Types.TIMESTAMP);

                // [9] expiry_date
                if (ev.getExpiryDate() != null)
                    ps.setDate(9, java.sql.Date.valueOf(ev.getExpiryDate()));
                else
                    ps.setNull(9, Types.DATE);

                // [10] FK: file_id
                ps.setObject(10, ev.getCsv() != null ? ev.getCsv().getFileId() : null);
            }

            public int getBatchSize() {
                return eventHistories.size();
            }
        });

        log.info("✅ EventHistory batch insert 완료! 저장 건수: {}", eventHistories.size());
    }

}

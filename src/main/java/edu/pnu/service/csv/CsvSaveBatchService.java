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
    @Transactional
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
    @Transactional
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
    @Transactional
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
    @Transactional
    public void saveEventHistories(List<EventHistory> eventHistories) {
        if (eventHistories.isEmpty()) return;
        String sql = "INSERT INTO eventhistory (epc_code, location_id, hub_type, business_step, business_original, event_type, event_time, manufacture_date, expiry_date, file_id, anomaly, jump, jump_score, evt_order_err, evt_order_err_score, epc_fake, epc_fake_score, epc_dup, epc_dup_score, loc_err, loc_err_score) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EventHistory ev = eventHistories.get(i);
                ps.setString(1, ev.getEpc() != null ? ev.getEpc().getEpcCode() : null);
                ps.setObject(2, ev.getLocation() != null ? ev.getLocation().getLocationId() : null);
                ps.setString(3, ev.getHubType());
                ps.setString(4, ev.getBusinessStep());
                ps.setString(5, ev.getBusinessOriginal());
                ps.setString(6, ev.getEventType());
                if (ev.getEventTime() != null)
                    ps.setTimestamp(7, java.sql.Timestamp.valueOf(ev.getEventTime()));
                else
                    ps.setNull(7, Types.TIMESTAMP);
                if (ev.getManufactureDate() != null)
                    ps.setTimestamp(8, java.sql.Timestamp.valueOf(ev.getManufactureDate()));
                else
                    ps.setNull(8, Types.TIMESTAMP);
                if (ev.getExpiryDate() != null)
                    ps.setDate(9, java.sql.Date.valueOf(ev.getExpiryDate()));
                else
                    ps.setNull(9, Types.DATE);
                ps.setObject(10, ev.getFileLog() != null ? ev.getFileLog().getFileId() : null);
                ps.setBoolean(11, ev.isAnomaly());
                ps.setBoolean(12, ev.isJump());
                ps.setDouble(13, ev.getJumpScore());
                ps.setBoolean(14, ev.isEvtOrderErr());
                ps.setDouble(15, ev.getEvtOrderErrScore());
                ps.setBoolean(16, ev.isEpcFake());
                ps.setDouble(17, ev.getEpcFakeScore());
                ps.setBoolean(18, ev.isEpcDup());
                ps.setDouble(19, ev.getEpcDupScore());
                ps.setBoolean(20, ev.isLocErr());
                ps.setDouble(21, ev.getLocErrScore());
            }
            public int getBatchSize() { return eventHistories.size(); }
        });
        log.info("EventHistory batch insert 완료! 저장 건수: {}", eventHistories.size());
    }
}

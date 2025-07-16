package edu.pnu.service.csv;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import edu.pnu.domain.EventHistory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * EventHistory 대량 저장 (kum.csv, eventhistory 테이블 컬럼 100% 일치)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchService {

    private final JdbcTemplate jdbcTemplate;

    /**
     * eventhistory 테이블에 맞춘 batch insert
     */
    public void batchInsertEventHistories(List<EventHistory> eventHistories) {
        final String sql = "INSERT INTO eventhistory " +
                "(epc_code, location_id, hub_type, business_step, business_original, event_type, event_time, manufacture_date, expiry_date, file_id, " +
                "anomaly, jump, jump_score, evt_order_err, evt_order_err_score, epc_fake, epc_fake_score, epc_dup, epc_dup_score, loc_err, loc_err_score) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                EventHistory ev = eventHistories.get(i);
                // 연관 엔티티의 PK만 추출 (null-safe)
                ps.setString(1,  ev.getEpc() != null ? ev.getEpc().getEpcCode() : null);
                ps.setObject(2,  ev.getLocation() != null ? ev.getLocation().getLocationId() : null);

                ps.setString(3,  ev.getHubType());
                ps.setString(4,  ev.getBusinessStep());
                ps.setString(5,  ev.getBusinessOriginal());
                ps.setString(6,  ev.getEventType());

                // LocalDateTime → Timestamp, LocalDate → Date, null-safe
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

                // 분석/진단 컬럼 (boolean/double, null-safe)
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
            @Override
            public int getBatchSize() {
                return eventHistories.size();
            }
        });
        log.info("EventHistory batch insert 완료! 저장 건수: {}", eventHistories.size());
    }
}

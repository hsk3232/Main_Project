package edu.pnu.service.csv;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import edu.pnu.domain.AiData;
import edu.pnu.domain.Epc;
import edu.pnu.domain.EventHistory;
import edu.pnu.domain.Location;
import edu.pnu.domain.Product;
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
		
		log.info("[진입] : [CsvSaveBatchService] saveLocation 진입");
		
		if (locations.isEmpty())
			return;
		String sql = "INSERT INTO location (location_id, scan_location, latitude, longitude) VALUES (?, ?, ?, ?)";
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				Location l = locations.get(i);
				ps.setLong(1, l.getLocationId());
				ps.setString(2, l.getScanLocation());
				ps.setDouble(3, l.getLatitude());
				ps.setDouble(4, l.getLongitude());
			}

			public int getBatchSize() {
				return locations.size();
			}
		});
		log.info("[성공] : [CsvSaveBatchService] Location batch insert 완료! 저장 건수: {}", locations.size());
	}

	/** Product batch insert */

	public void saveProducts(List<Product> products) {
		
		log.info("[진입] : [CsvSaveBatchService] saveProducts 진입");
		
		if (products.isEmpty())
			return;
		String sql = "INSERT INTO product (epc_product, product_name) VALUES (?, ?)";
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			public void setValues(PreparedStatement ps, int i) throws SQLException {
				Product p = products.get(i);
				ps.setLong(1, p.getEpcProduct());
				ps.setString(2, p.getProductName());
			}

			public int getBatchSize() {
				return products.size();
			}
		});
		log.info("[성공] : [CsvSaveBatchService] Product batch insert 완료! 저장 건수: {}", products.size());
	}

	/** Epc batch insert */

	public void saveEpcs(List<Epc> epcs) {
		log.info("[진입] : [CsvSaveBatchService] saveEpcs 진입");
		
		if (epcs.isEmpty())
			return;
		String sql = "INSERT INTO epc (epc_code, epc_header, epc_company, epc_lot, epc_serial, location_id, epc_product) VALUES (?, ?, ?, ?, ?, ?, ?)";
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

			public int getBatchSize() {
				return epcs.size();
			}
		});
		log.info("[성공] : [CsvSaveBatchService] Epc batch insert 완료! 저장 건수: {}", epcs.size());
	}


	/** EventHistory batch insert with KeyHolder - ID를 다시 객체에 설정 */
	public void saveEvent(List<EventHistory> event) {
		log.info("[진입] : [CsvSaveBatchService] saveEvent 진입");
		
		if (event.isEmpty())
			return;

		String sql = "INSERT INTO eventhistory (epc_code, location_id, hub_type, business_step, business_original, event_type, event_time, manufacture_date, expiry_date, file_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

		try {
			
//			// KeyHolder 생성 - 생성된 ID들을 받기 위함
//	        GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
			
			jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
				public void setValues(PreparedStatement ps, int i) throws SQLException {
					EventHistory ev = event.get(i);

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
					return event.size();
				}
			});

			log.info("[성공] : [CsvSaveBatchService] EventHistory batch insert 완료! 저장 건수: {}, ID 설정 완료", event.size());    

		}

		catch (Exception e) {
			log.error("[오류] : [CsvSaveBatchService] [EventHistory 저장 실패]", e);
		}
	}
	
	public void saveAiDataBatch(List<AiData> aiDataList) {
		String sql = "INSERT INTO aidata (anomaly, epc_dup, epc_dup_score, epc_fake, epc_fake_score, evt_order_err, evt_order_err_score, jump, jump_score, loc_err, loc_err_score, event_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) ";
		jdbcTemplate.batchUpdate(sql, new BatchPreparedStatementSetter() {
			 public void setValues(PreparedStatement ps, int i) throws SQLException {
				 AiData a = aiDataList.get(i);
				 ps.setBoolean(1, a.isAnomaly());
				 ps.setBoolean(2, a.isEpcDup());
				 ps.setDouble(3, a.getEpcDupScore());
				 ps.setBoolean(4, a.isEpcFake());
				 ps.setDouble(5, a.getEpcFakeScore());
				 ps.setBoolean(6, a.isEvtOrderErr());
				 ps.setDouble(7, a.getEvtOrderErrScore());
				 ps.setBoolean(8, a.isJump());
				 ps.setDouble(9, a.getJumpScore());
				 ps.setBoolean(10, a.isLocErr());
				 ps.setDouble(11, a.getLocErrScore());
		         ps.setObject(12, a.getEventHistory().getEventId());

			 }
			 public int getBatchSize() {
				 return aiDataList.size();
			 }
		});
	}

}

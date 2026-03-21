package com.company.platform.shared.idempotency;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/** Repository for idempotency records. */
@Repository
public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecord, String> {

  /** Find a record by idempotency key. */
  Optional<IdempotencyRecord> findByIdempotencyKey(String idempotencyKey);

  /** Find expired records for cleanup. */
  @Query("SELECT i FROM IdempotencyRecord i WHERE i.expiresAt < :now")
  List<IdempotencyRecord> findExpiredRecords(Instant now, Pageable pageable);
}

package com.company.platform.shared.repository;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

@NoRepositoryBean
public interface FullAuditRepository<T, ID extends Serializable>
    extends SoftDeleteRepository<T, ID> {

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.createdBy = :createdBy AND e.deleted = false ORDER BY e.createdAt DESC")
  List<T> findActiveByCreatedBy(@Param("createdBy") UUID createdBy);

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.updatedBy = :updatedBy AND e.deleted = false ORDER BY e.updatedAt DESC")
  List<T> findActiveByUpdatedBy(@Param("updatedBy") UUID updatedBy);

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.createdAt BETWEEN :startDate AND :endDate AND e.deleted = false ORDER BY e.createdAt DESC")
  List<T> findActiveByCreatedAtBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.updatedAt BETWEEN :startDate AND :endDate AND e.deleted = false ORDER BY e.updatedAt DESC")
  List<T> findActiveByUpdatedAtBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.updatedAt IS NOT NULL AND e.updatedAt != e.createdAt AND e.deleted = false ORDER BY e.updatedAt DESC")
  List<T> findActiveModifiedEntities();

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.createdBy = :createdBy AND e.deleted = true ORDER BY e.deletedAt DESC")
  List<T> findDeletedByCreatedBy(@Param("createdBy") UUID createdBy);

  @Query(
      "SELECT COUNT(e) FROM #{#entityName} e WHERE e.createdBy = :createdBy AND e.deleted = false")
  long countActiveByCreatedBy(@Param("createdBy") UUID createdBy);

  @Query(
      "SELECT COUNT(e) FROM #{#entityName} e WHERE e.updatedBy = :updatedBy AND e.deleted = false")
  long countActiveByUpdatedBy(@Param("updatedBy") UUID updatedBy);
}

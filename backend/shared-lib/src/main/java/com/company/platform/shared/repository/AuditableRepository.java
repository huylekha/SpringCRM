package com.company.platform.shared.repository;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

@NoRepositoryBean
public interface AuditableRepository<T, ID extends Serializable> extends BaseRepository<T, ID> {

  @Query("SELECT e FROM #{#entityName} e WHERE e.createdBy = :createdBy ORDER BY e.createdAt DESC")
  List<T> findByCreatedBy(@Param("createdBy") UUID createdBy);

  @Query("SELECT e FROM #{#entityName} e WHERE e.updatedBy = :updatedBy ORDER BY e.updatedAt DESC")
  List<T> findByUpdatedBy(@Param("updatedBy") UUID updatedBy);

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.createdAt BETWEEN :startDate AND :endDate ORDER BY e.createdAt DESC")
  List<T> findByCreatedAtBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.updatedAt BETWEEN :startDate AND :endDate ORDER BY e.updatedAt DESC")
  List<T> findByUpdatedAtBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  @Query("SELECT e FROM #{#entityName} e WHERE e.createdAt > :date ORDER BY e.createdAt DESC")
  List<T> findByCreatedAtAfter(@Param("date") Instant date);

  @Query("SELECT e FROM #{#entityName} e WHERE e.updatedAt > :date ORDER BY e.updatedAt DESC")
  List<T> findByUpdatedAtAfter(@Param("date") Instant date);

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.updatedAt IS NOT NULL AND e.updatedAt != e.createdAt ORDER BY e.updatedAt DESC")
  List<T> findModifiedEntities();

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.updatedAt IS NULL OR e.updatedAt = e.createdAt ORDER BY e.createdAt DESC")
  List<T> findUnmodifiedEntities();

  @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.createdBy = :createdBy")
  long countByCreatedBy(@Param("createdBy") UUID createdBy);

  @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.updatedBy = :updatedBy")
  long countByUpdatedBy(@Param("updatedBy") UUID updatedBy);

  @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.createdAt BETWEEN :startDate AND :endDate")
  long countByCreatedAtBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  @Query("SELECT e FROM #{#entityName} e ORDER BY e.createdAt DESC LIMIT 1")
  Optional<T> findMostRecentlyCreated();

  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.updatedAt IS NOT NULL ORDER BY e.updatedAt DESC LIMIT 1")
  Optional<T> findMostRecentlyModified();
}

package com.company.platform.shared.repository;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository interface for soft deletable entities that extends AuditableRepository with soft
 * delete-specific query methods.
 *
 * @param <T> the soft deletable entity type
 * @param <ID> the ID type
 */
@NoRepositoryBean
public interface SoftDeleteRepository<T, ID extends Serializable>
    extends AuditableRepository<T, ID> {

  /**
   * Finds all active (non-deleted) entities.
   *
   * @return the list of active entities
   */
  @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = false ORDER BY e.createdAt DESC")
  List<T> findAllActive();

  /**
   * Finds all deleted entities.
   *
   * @return the list of deleted entities
   */
  @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = true ORDER BY e.deletedAt DESC")
  List<T> findAllDeleted();

  /**
   * Finds an active entity by ID.
   *
   * @param id the entity ID
   * @return the active entity, or empty if not found or deleted
   */
  @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
  Optional<T> findActiveById(@Param("id") ID id);

  /**
   * Finds an active entity by ID, ensuring it exists.
   *
   * @param id the entity ID
   * @return the active entity
   * @throws org.springframework.dao.EmptyResultDataAccessException if not found or deleted
   */
  default T findActiveByIdRequired(ID id) {
    return findActiveById(id)
        .orElseThrow(
            () ->
                new org.springframework.dao.EmptyResultDataAccessException(
                    "Active entity with ID " + id + " not found", 1));
  }

  /**
   * Finds a deleted entity by ID.
   *
   * @param id the entity ID
   * @return the deleted entity, or empty if not found or not deleted
   */
  @Query("SELECT e FROM #{#entityName} e WHERE e.id = :id AND e.deleted = true")
  Optional<T> findDeletedById(@Param("id") ID id);

  /**
   * Finds active entities by a list of IDs.
   *
   * @param ids the list of IDs
   * @return the list of active entities
   */
  @Query("SELECT e FROM #{#entityName} e WHERE e.id IN :ids AND e.deleted = false")
  List<T> findActiveByIdIn(@Param("ids") List<ID> ids);

  /**
   * Finds entities deleted within a date range.
   *
   * @param startDate the start date (inclusive)
   * @param endDate the end date (inclusive)
   * @return the list of entities deleted within the date range
   */
  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.deleted = true AND e.deletedAt BETWEEN :startDate AND :endDate ORDER BY e.deletedAt DESC")
  List<T> findDeletedBetween(
      @Param("startDate") Instant startDate, @Param("endDate") Instant endDate);

  /**
   * Finds entities deleted after a specific date.
   *
   * @param date the date threshold
   * @return the list of entities deleted after the date
   */
  @Query(
      "SELECT e FROM #{#entityName} e WHERE e.deleted = true AND e.deletedAt > :date ORDER BY e.deletedAt DESC")
  List<T> findDeletedAfter(@Param("date") Instant date);

  /**
   * Counts active entities.
   *
   * @return the count of active entities
   */
  @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deleted = false")
  long countActive();

  /**
   * Counts deleted entities.
   *
   * @return the count of deleted entities
   */
  @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.deleted = true")
  long countDeleted();

  /**
   * Checks if an active entity exists by ID.
   *
   * @param id the entity ID
   * @return true if an active entity exists, false otherwise
   */
  @Query("SELECT COUNT(e) > 0 FROM #{#entityName} e WHERE e.id = :id AND e.deleted = false")
  boolean existsActiveById(@Param("id") ID id);

  /**
   * Checks if a deleted entity exists by ID.
   *
   * @param id the entity ID
   * @return true if a deleted entity exists, false otherwise
   */
  @Query("SELECT COUNT(e) > 0 FROM #{#entityName} e WHERE e.id = :id AND e.deleted = true")
  boolean existsDeletedById(@Param("id") ID id);

  /**
   * Soft deletes an entity by ID. This method updates the deleted flag and sets the deletion
   * timestamp.
   *
   * @param id the entity ID
   * @return the number of entities updated (should be 1 if successful)
   */
  @Modifying
  @Transactional
  @Query(
      "UPDATE #{#entityName} e SET e.deleted = true, e.deletedAt = :deletedAt WHERE e.id = :id AND e.deleted = false")
  int softDeleteById(@Param("id") ID id, @Param("deletedAt") Instant deletedAt);

  /**
   * Soft deletes entities by a list of IDs.
   *
   * @param ids the list of IDs
   * @return the number of entities updated
   */
  @Modifying
  @Transactional
  @Query(
      "UPDATE #{#entityName} e SET e.deleted = true, e.deletedAt = :deletedAt WHERE e.id IN :ids AND e.deleted = false")
  int softDeleteByIdIn(@Param("ids") List<ID> ids, @Param("deletedAt") Instant deletedAt);

  /**
   * Restores a soft deleted entity by ID. This method clears the deleted flag and deletion
   * timestamp.
   *
   * @param id the entity ID
   * @return the number of entities updated (should be 1 if successful)
   */
  @Modifying
  @Transactional
  @Query(
      "UPDATE #{#entityName} e SET e.deleted = false, e.deletedAt = null WHERE e.id = :id AND e.deleted = true")
  int restoreById(@Param("id") ID id);

  /**
   * Restores soft deleted entities by a list of IDs.
   *
   * @param ids the list of IDs
   * @return the number of entities updated
   */
  @Modifying
  @Transactional
  @Query(
      "UPDATE #{#entityName} e SET e.deleted = false, e.deletedAt = null WHERE e.id IN :ids AND e.deleted = true")
  int restoreByIdIn(@Param("ids") List<ID> ids);

  /**
   * Permanently deletes soft deleted entities older than the specified date. This is a hard delete
   * operation that cannot be undone.
   *
   * @param olderThan the date threshold
   * @return the number of entities permanently deleted
   */
  @Modifying
  @Transactional
  @Query("DELETE FROM #{#entityName} e WHERE e.deleted = true AND e.deletedAt < :olderThan")
  int permanentlyDeleteOlderThan(@Param("olderThan") Instant olderThan);

  /**
   * Finds the most recently deleted entity.
   *
   * @return the most recently deleted entity, or empty if no deleted entities exist
   */
  @Query("SELECT e FROM #{#entityName} e WHERE e.deleted = true ORDER BY e.deletedAt DESC LIMIT 1")
  Optional<T> findMostRecentlyDeleted();
}

package com.company.platform.shared.repository;

import java.io.Serializable;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

/**
 * Base repository interface providing type-safe CRUD operations for entities. This interface
 * extends JpaRepository and adds common query methods that are useful across all entity types.
 *
 * @param <T> the entity type
 * @param <ID> the ID type
 */
@NoRepositoryBean
public interface BaseRepository<T, ID extends Serializable> extends JpaRepository<T, ID> {

  /**
   * Finds an entity by ID, ensuring it exists.
   *
   * @param id the entity ID
   * @return the entity
   * @throws org.springframework.dao.EmptyResultDataAccessException if not found
   */
  default T findByIdRequired(ID id) {
    return findById(id)
        .orElseThrow(
            () ->
                new org.springframework.dao.EmptyResultDataAccessException(
                    "Entity with ID " + id + " not found", 1));
  }

  /**
   * Checks if an entity exists by ID.
   *
   * @param id the entity ID
   * @return true if the entity exists, false otherwise
   */
  boolean existsById(ID id);

  /**
   * Finds entities by a list of IDs.
   *
   * @param ids the list of IDs
   * @return the list of entities (may be smaller than the input list if some IDs don't exist)
   */
  @Query("SELECT e FROM #{#entityName} e WHERE e.id IN :ids")
  List<T> findByIdIn(@Param("ids") List<ID> ids);

  /**
   * Counts entities by a list of IDs.
   *
   * @param ids the list of IDs
   * @return the count of existing entities
   */
  @Query("SELECT COUNT(e) FROM #{#entityName} e WHERE e.id IN :ids")
  long countByIdIn(@Param("ids") List<ID> ids);

  /**
   * Finds the first N entities ordered by ID.
   *
   * @param limit the maximum number of entities to return
   * @return the list of entities
   */
  @Query("SELECT e FROM #{#entityName} e ORDER BY e.id")
  List<T> findFirstNOrderById(@Param("limit") int limit);

  /**
   * Deletes entities by a list of IDs.
   *
   * @param ids the list of IDs to delete
   * @return the number of entities deleted
   */
  @Query("DELETE FROM #{#entityName} e WHERE e.id IN :ids")
  int deleteByIdIn(@Param("ids") List<ID> ids);
}

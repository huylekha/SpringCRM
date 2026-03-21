package com.company.platform.shared.cqrs;

/**
 * Marker interface for all queries in the CQRS architecture. Queries represent read operations that
 * do not change system state.
 *
 * @param <TResponse> The type of response returned by the query handler
 */
public interface Query<TResponse> {}

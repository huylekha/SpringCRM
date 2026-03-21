package com.company.platform.shared.cqrs;

/**
 * Marker interface for all commands in the CQRS architecture. Commands represent write operations
 * that change system state.
 *
 * @param <TResponse> The type of response returned by the command handler
 */
public interface Command<TResponse> {}

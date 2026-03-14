package com.company.platform.auth.claim.controller;

import com.company.platform.auth.claim.dto.request.CreateClaimRequest;
import com.company.platform.auth.claim.dto.response.ClaimResponse;
import com.company.platform.auth.claim.service.ClaimService;
import com.company.platform.shared.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/claims")
@RequiredArgsConstructor
public class ClaimController {

  private final ClaimService claimService;

  @PostMapping
  @PreAuthorize("@perm.has('claim:create')")
  public ResponseEntity<ClaimResponse> createClaim(@Valid @RequestBody CreateClaimRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(claimService.createClaim(request));
  }

  @GetMapping
  @PreAuthorize("@perm.has('claim:read')")
  public ResponseEntity<PageResponse<ClaimResponse>> listClaims(
      @PageableDefault(size = 50) Pageable pageable) {
    return ResponseEntity.ok(claimService.listClaims(pageable));
  }
}

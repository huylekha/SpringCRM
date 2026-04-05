package com.company.platform.auth.claim.service;

import com.company.platform.auth.auth.service.PermissionEvaluator;
import com.company.platform.auth.claim.domain.AuthClaim;
import com.company.platform.auth.claim.dto.request.CreateClaimRequest;
import com.company.platform.auth.claim.dto.response.ClaimResponse;
import com.company.platform.auth.claim.repository.AuthClaimRepository;
import com.company.platform.shared.exception.DuplicateResourceException;
import com.company.platform.shared.exception.ErrorCode;
import com.company.platform.shared.response.PageResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClaimService {

  private final AuthClaimRepository claimRepository;
  private final PermissionEvaluator permissionEvaluator;

  @Transactional
  public ClaimResponse createClaim(CreateClaimRequest request) {
    if (claimRepository.existsByClaimCodeAndDeletedFalse(request.getClaimCode())) {
      throw new DuplicateResourceException(ErrorCode.AUTH_CLAIM_ALREADY_EXISTS);
    }
    AuthClaim claim =
        AuthClaim.builder()
            .claimCode(request.getClaimCode())
            .claimName(request.getClaimName())
            .build();
    claim = claimRepository.save(claim);
    return toResponse(claim);
  }

  @Transactional(readOnly = true)
  public PageResponse<ClaimResponse> listClaims(Pageable pageable) {
    Page<AuthClaim> page = claimRepository.findAllByDeletedFalse(pageable);
    List<ClaimResponse> content = page.getContent().stream().map(this::toResponse).toList();
    return PageResponse.<ClaimResponse>builder()
        .content(content)
        .page(page.getNumber())
        .size(page.getSize())
        .totalElements(page.getTotalElements())
        .totalPages(page.getTotalPages())
        .build();
  }

  private ClaimResponse toResponse(AuthClaim c) {
    return ClaimResponse.builder()
        .id(c.getId() != null ? c.getId().toString() : null)
        .claimCode(c.getClaimCode())
        .claimName(c.getClaimName())
        .build();
  }
}

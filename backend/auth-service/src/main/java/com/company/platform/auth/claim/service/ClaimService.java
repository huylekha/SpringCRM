package com.company.platform.auth.claim.service;

import com.company.platform.auth.auth.service.PermissionEvaluator;
import com.company.platform.auth.claim.domain.AuthClaim;
import com.company.platform.auth.claim.dto.request.CreateClaimRequest;
import com.company.platform.auth.claim.dto.response.ClaimResponse;
import com.company.platform.auth.claim.repository.AuthClaimRepository;
import com.company.platform.shared.exception.DuplicateResourceException;
import com.company.platform.shared.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ClaimService {

    private final AuthClaimRepository claimRepository;
    private final PermissionEvaluator permissionEvaluator;

    @Transactional
    public ClaimResponse createClaim(CreateClaimRequest request) {
        if (claimRepository.existsByClaimCodeAndDeletedFalse(request.getClaimCode())) {
            throw new DuplicateResourceException("AUTH_DUPLICATE_CLAIM_CODE",
                    "Claim code '" + request.getClaimCode() + "' already exists");
        }
        AuthClaim claim = AuthClaim.builder()
                .claimCode(request.getClaimCode())
                .claimName(request.getClaimName())
                .createdBy(permissionEvaluator.currentUserId())
                .build();
        claim = claimRepository.save(claim);
        return toResponse(claim);
    }

    @Transactional(readOnly = true)
    public PageResponse<ClaimResponse> listClaims(Pageable pageable) {
        Page<AuthClaim> page = claimRepository.findAllByDeletedFalse(pageable);
        List<ClaimResponse> content = page.getContent().stream()
                .map(this::toResponse).toList();
        return PageResponse.<ClaimResponse>builder()
                .content(content).page(page.getNumber()).size(page.getSize())
                .totalElements(page.getTotalElements()).totalPages(page.getTotalPages())
                .build();
    }

    private ClaimResponse toResponse(AuthClaim c) {
        return ClaimResponse.builder()
                .id(c.getId()).claimCode(c.getClaimCode()).claimName(c.getClaimName())
                .build();
    }
}

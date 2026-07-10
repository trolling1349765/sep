package fpt.capstone.service.impl;

import fpt.capstone.dto.request.SponsorRequest;
import fpt.capstone.dto.response.SponsorResponse;
import fpt.capstone.entity.Sponsor;
import fpt.capstone.repository.SponsorRepository;
import fpt.capstone.service.SponsorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SponsorServiceImpl implements SponsorService {

    private final SponsorRepository sponsorRepository;

    @Override
    @Transactional
    public SponsorResponse createSponsor(SponsorRequest request) {
        Sponsor sponsor = Sponsor.builder()
                .name(request.getName())
                .sponsorType(request.getSponsorType() != null ? request.getSponsorType() : "TO_CHUC")
                .contactInfo(request.getContactInfo())
                .phone(request.getPhone())
                .email(request.getEmail())
                .address(request.getAddress())
                .representative(request.getRepresentative())
                .taxCode(request.getTaxCode())
                .status(request.getStatus() != null ? request.getStatus() : "ACTIVE")
                .createAt(LocalDate.now())
                .build();
        sponsor = sponsorRepository.save(sponsor);
        return toResponse(sponsor);
    }

    @Override
    @Transactional
    public SponsorResponse updateSponsor(String id, SponsorRequest request) {
        Sponsor sponsor = sponsorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhà tài trợ"));

        sponsor.setName(request.getName());
        sponsor.setSponsorType(request.getSponsorType());
        sponsor.setContactInfo(request.getContactInfo());
        sponsor.setPhone(request.getPhone());
        sponsor.setEmail(request.getEmail());
        sponsor.setAddress(request.getAddress());
        sponsor.setRepresentative(request.getRepresentative());
        sponsor.setTaxCode(request.getTaxCode());
        sponsor.setStatus(request.getStatus());
        sponsor.setUpdateAt(LocalDate.now());

        sponsor = sponsorRepository.save(sponsor);
        return toResponse(sponsor);
    }

    @Override
    public SponsorResponse getSponsorById(String id) {
        Sponsor sponsor = sponsorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhà tài trợ"));
        return toResponse(sponsor);
    }

    @Override
    public List<SponsorResponse> getAllSponsors() {
        return sponsorRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SponsorResponse> searchSponsors(String keyword) {
        return sponsorRepository.findByNameContainingIgnoreCase(keyword).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<SponsorResponse> getSponsorsByType(String type) {
        return sponsorRepository.findBySponsorType(type).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteSponsor(String id) {
        Sponsor sponsor = sponsorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhà tài trợ"));
        sponsor.setIsDelete(true);
        sponsor.setUpdateAt(LocalDate.now());
        sponsorRepository.save(sponsor);
    }

    private SponsorResponse toResponse(Sponsor sponsor) {
        return SponsorResponse.builder()
                .id(sponsor.getId())
                .name(sponsor.getName())
                .sponsorType(sponsor.getSponsorType())
                .contactInfo(sponsor.getContactInfo())
                .phone(sponsor.getPhone())
                .email(sponsor.getEmail())
                .address(sponsor.getAddress())
                .representative(sponsor.getRepresentative())
                .taxCode(sponsor.getTaxCode())
                .status(sponsor.getStatus())
                .createAt(sponsor.getCreateAt())
                .updateAt(sponsor.getUpdateAt())
                .build();
    }
}
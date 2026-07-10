package fpt.capstone.service.impl;

import fpt.capstone.dto.request.DonationRequest;
import fpt.capstone.dto.response.DonationResponse;
import fpt.capstone.entity.Donation;
import fpt.capstone.entity.Sponsor;
import fpt.capstone.repository.DonationRepository;
import fpt.capstone.repository.SponsorRepository;
import fpt.capstone.service.DonationService;
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
public class DonationServiceImpl implements DonationService {

    private final DonationRepository donationRepository;
    private final SponsorRepository sponsorRepository;

    @Override
    @Transactional
    public DonationResponse createDonation(DonationRequest request) {
        Sponsor sponsor = null;
        if (request.getSponsorId() != null) {
            sponsor = sponsorRepository.findById(request.getSponsorId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy nhà tài trợ"));
        }

        Donation donation = Donation.builder()
                .sponsor(sponsor)
                .amount(request.getAmount())
                .transferDate(request.getTransferDate() != null ? request.getTransferDate() : LocalDate.now())
                .purpose(request.getPurpose())
                .paymentMethod(request.getPaymentMethod())
                .receiptStatus(request.getReceiptStatus() != null ? request.getReceiptStatus() : "PENDING")
                .evidenceUrl(request.getEvidenceUrl())
                .notes(request.getNotes())
                .createAt(LocalDate.now())
                .build();
        donation = donationRepository.save(donation);
        return toResponse(donation);
    }

    @Override
    public DonationResponse getDonationById(int id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoản tài trợ"));
        return toResponse(donation);
    }

    @Override
    public List<DonationResponse> getAllDonations() {
        return donationRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<DonationResponse> getDonationsBySponsor(String sponsorId) {
        return donationRepository.findBySponsorId(sponsorId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deleteDonation(int id) {
        Donation donation = donationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy khoản tài trợ"));
        donation.setIsDelete(true);
        donation.setUpdateAt(LocalDate.now());
        donationRepository.save(donation);
    }

    private DonationResponse toResponse(Donation donation) {
        return DonationResponse.builder()
                .id(donation.getId())
                .sponsorId(donation.getSponsor() != null ? donation.getSponsor().getId() : null)
                .sponsorName(donation.getSponsor() != null ? donation.getSponsor().getName() : null)
                .amount(donation.getAmount())
                .transferDate(donation.getTransferDate())
                .purpose(donation.getPurpose())
                .paymentMethod(donation.getPaymentMethod())
                .receiptStatus(donation.getReceiptStatus())
                .evidenceUrl(donation.getEvidenceUrl())
                .notes(donation.getNotes())
                .createAt(donation.getCreateAt())
                .build();
    }
}
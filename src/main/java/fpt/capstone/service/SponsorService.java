package fpt.capstone.service;

import fpt.capstone.dto.request.SponsorRequest;
import fpt.capstone.dto.response.SponsorResponse;

import java.util.List;

public interface SponsorService {
    SponsorResponse createSponsor(SponsorRequest request);

    SponsorResponse updateSponsor(String id, SponsorRequest request);

    SponsorResponse getSponsorById(String id);

    List<SponsorResponse> getAllSponsors();

    List<SponsorResponse> searchSponsors(String keyword);

    List<SponsorResponse> getSponsorsByType(String type);

    void deleteSponsor(String id);
}
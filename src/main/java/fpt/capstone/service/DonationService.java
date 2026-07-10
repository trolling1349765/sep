package fpt.capstone.service;

import fpt.capstone.dto.request.DonationRequest;
import fpt.capstone.dto.response.DonationResponse;

import java.util.List;

public interface DonationService {
    DonationResponse createDonation(DonationRequest request);

    DonationResponse getDonationById(int id);

    List<DonationResponse> getAllDonations();

    List<DonationResponse> getDonationsBySponsor(String sponsorId);

    void deleteDonation(int id);
}
package fpt.capstone.dto.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KrakenOcrResponse {
    private String status;
    private int lineDetected;
}

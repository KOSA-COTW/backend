package cotw.server.domain.donation.controller;

import cotw.server.domain.donation.service.DonationCounterService;
import cotw.server.domain.donation.service.DonationRebuildService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/public")
public class PublicDonationController {
    private final DonationCounterService counters;
    private final DonationRebuildService rebuild;

    @GetMapping("/donation-total")
    public Map<String, Long> total() {
        if(!counters.hasTotalKey()){
            rebuild.rebuildAll();
        }
        return Map.of("totalWon", counters.getTotal());
    }
}

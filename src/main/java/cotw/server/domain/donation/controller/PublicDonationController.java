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
@RequestMapping("/public")
public class PublicDonationController {
    private final DonationCounterService counters;
    private final DonationRebuildService rebuild;

    @GetMapping("/donation-total")
    public Map<String, Long> total() {
//        long v = counters.getTotal();
//        if (v == 0L) { // 초기 Redis 비었을 때 복구
//            rebuild.rebuildAll();
//            v = counters.getTotal();
//        }
        if(!counters.hasTotalKey()){
            rebuild.rebuildAll();
//            v = counters.getTotal();
        }
        return Map.of("totalWon", counters.getTotal());
    }
}

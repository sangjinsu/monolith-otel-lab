package com.sangjinsu.monolithotellab.inventory;

import com.sangjinsu.monolithotellab.order.dto.CreateOrderRequest;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * Fake inventory service. Always succeeds; simulates a small amount of work.
 * The {@code @Observed} annotation creates the "InventoryService.reserve" span.
 */
@Service
public class InventoryService {

    @Observed(name = "inventory.reserve", contextualName = "InventoryService.reserve")
    public void reserve(List<CreateOrderRequest.Item> items) {
        // Pretend to reserve stock for each line item.
        sleep(10);
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}

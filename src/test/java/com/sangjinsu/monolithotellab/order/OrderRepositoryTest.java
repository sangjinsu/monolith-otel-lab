package com.sangjinsu.monolithotellab.order;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

/**
 * Layer 3: @DataJpaTest slice — JPA mapping/persistence on an embedded H2 database
 * for fast feedback; real PostgreSQL behaviour is exercised via the docker-compose
 * stack (make up). Shows @EntityGraph fetching (findWithItemsById) round-tripping a
 * parent aggregate with its children.
 */
@DataJpaTest
class OrderRepositoryTest {

    @Autowired
    OrderRepository orderRepository;

    @Test
    void saveAndFindWithItems() {
        Order order = new Order("order-1", "user-1", OrderStatus.CREATED, Instant.now());
        order.addItem("item-1", 2);
        order.addItem("item-2", 3);
        orderRepository.saveAndFlush(order);

        Optional<Order> found = orderRepository.findWithItemsById("order-1");

        assertThat(found).isPresent();
        assertThat(found.get().getUserId()).isEqualTo("user-1");
        assertThat(found.get().getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(found.get().getItems()).hasSize(2);
        assertThat(found.get().getItems()).extracting(OrderItem::getSku)
                .containsExactlyInAnyOrder("item-1", "item-2");
    }

    @Test
    void findWithItemsById_missing_returnsEmpty() {
        assertThat(orderRepository.findWithItemsById("does-not-exist")).isEmpty();
    }
}

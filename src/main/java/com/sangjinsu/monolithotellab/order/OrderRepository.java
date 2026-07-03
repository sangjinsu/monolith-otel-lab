package com.sangjinsu.monolithotellab.order;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, String> {

    @EntityGraph(attributePaths = "items")
    Optional<Order> findWithItemsById(String id);
}

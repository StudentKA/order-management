package com.shop.service;

import com.shop.api.swagger.models.FullOrderDto;
import com.shop.api.swagger.models.OrderDto;
import com.shop.api.swagger.models.OrderStatus;
import com.shop.client.ProductServiceClient;
import com.shop.client.ProductServiceSender;
import com.shop.exception.OrderNotFoundException;
import com.shop.model.Order;
import com.shop.repository.OrderRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class OrderService {
    private final OrderRepository repository;
    private final ProductServiceClient client;
    private final ProductServiceSender productServiceSender;;


    @Autowired
    public OrderService(OrderRepository repository, ProductServiceClient client, ProductServiceSender productServiceSender) {
        this.repository = repository;
        this.client = client;
        this.productServiceSender = productServiceSender;
    }

    public FullOrderDto getOrder(String id) {
        log.info("Get order by id {}", id);
        Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        return Converter.convertToFullOrderDto(order, client.getProducts(order.getProducts().keySet()));
    }

    public OrderDto createOrder(OrderDto orderDto) {
        Order order = Converter.convertToEntity(orderDto);
        return Converter.convertToDto(repository.save(order));
    }

    public void deleteOrder(String id) {
        log.info("Delete order with id {}", id);
        Order order = repository.findById(id).orElseThrow(() -> new OrderNotFoundException(id));
        repository.delete(order);
    }


    public List<OrderDto> getAllOrders() {
        log.info("Get all orders");
        return repository.findAll().stream().map(Converter::convertToDto).collect(Collectors.toList());
    }

    public OrderDto updateOrder(OrderDto orderDto) {
        log.info("Update order with id {}", orderDto.getId());
        Order order = repository.findById(orderDto.getId()).orElseThrow(() -> new OrderNotFoundException(orderDto.getId()));
        return Converter.convertToDto(repository.save(Converter.convertToEntity(orderDto)));
    }

    public Double getTotal(String id) {
        Optional<Order> order = repository.findById(id);
        if (order.isPresent()) {
            return order.get().getTotal();
        }
        throw new OrderNotFoundException(id);
    }

    public void paidOrder(String orderId, Integer paymentId) {
        Order order = repository.findById(orderId).orElseThrow(() -> new OrderNotFoundException(orderId));
        order.setPaymentId(paymentId);
        order.setStatus(OrderStatus.PAID);
        repository.save(order);
        productServiceSender.sendMessage(order.getProducts());
    }
}

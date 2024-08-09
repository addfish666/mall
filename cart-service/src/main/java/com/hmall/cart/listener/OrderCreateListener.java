package com.hmall.cart.listener;

import com.hmall.cart.service.ICartService;
import com.hmall.common.utils.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
@Slf4j
@RequiredArgsConstructor
public class OrderCreateListener {
    private final ICartService cartService;

    @RabbitListener(bindings = @QueueBinding(
                    value = @Queue(name = "cart.clear.queue", durable = "true"),
                    exchange = @Exchange(name = "trade.topic"),
                    key = "order.create"
            ))
    public void listenPaySuccess(@Payload Set<Long> itemIds, @Header("user-info") Long userId) {
        UserContext.setUser(userId);
        cartService.removeByItemIds(itemIds);
    }
}

package com.hmall.search.listener;

import cn.hutool.json.JSONUtil;
import com.hmall.api.client.ItemClient;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.utils.BeanUtils;
import com.hmall.search.domain.po.ItemDoc;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentType;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class ItemListener {
    private final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
            HttpHost.create("http:192.168.189.129")
    ));

    private final ItemClient itemClient;

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "search.item.index.queue", durable = "true"),
            exchange = @Exchange(name = "search.direct", type = ExchangeTypes.DIRECT),
            key = "item.index"
    ))
    public void listenItemIndex(Long id) throws IOException {
        // 1.转化文档数据
        ItemDTO itemDTO = itemClient.queryItemById(id);
        ItemDoc itemDoc = BeanUtils.copyProperties(itemDTO, ItemDoc.class);
        // 2.准备Request
        IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
        // 3.准备请求参数
        request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
        // 4.发送请求
        IndexResponse resp = client.index(request, RequestOptions.DEFAULT);
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "search.item.status.queue", durable = "true"),
            exchange = @Exchange(name = "search.direct", type = ExchangeTypes.DIRECT),
            key = "item.updateStatus"
    ))
    public void listenItemStatus(Long id) throws IOException {
        // TODO 具体实现es更新状态
        // 0.转化文档数据
        ItemDTO itemDTO = itemClient.queryItemById(id);
        ItemDoc itemDoc = BeanUtils.copyProperties(itemDTO, ItemDoc.class);
        // 1.如果状态为下架，在es中删除该文档
        if(itemDTO.getStatus() == 2) {
            // 1.1.准备Request
            DeleteRequest request = new DeleteRequest("items", itemDoc.getId().toString());
            // 1.2.发送请求
            client.delete(request, RequestOptions.DEFAULT);
        }
        // 2.如果状态为上架，在es中新增该文档
        if(itemDTO.getStatus() == 1) {
            // 2.1.准备Request
            IndexRequest request = new IndexRequest("items").id(itemDoc.getId());
            // 2.2.准备请求参数
            request.source(JSONUtil.toJsonStr(itemDoc), XContentType.JSON);
            // 2.3.发送请求
            IndexResponse resp = client.index(request, RequestOptions.DEFAULT);
        }
    }

    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(name = "search.item.delete.queue", durable = "true"),
            exchange = @Exchange(name = "search.direct", type = ExchangeTypes.DIRECT),
            key = "item.delete"
    ))
    public void listenItemDelete(Long id) throws IOException {
        // TODO 具体实现es删除文档
        // 1.准备Request
        DeleteRequest request = new DeleteRequest("items", id.toString());
        // 2.发送请求
        client.delete(request, RequestOptions.DEFAULT);
    }
}

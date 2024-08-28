package com.hmall.search.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmall.api.dto.ItemDTO;
import com.hmall.common.domain.PageDTO;
import com.hmall.common.utils.BeanUtils;
import com.hmall.search.domain.po.ItemDoc;
import com.hmall.search.domain.query.ItemPageQuery;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.apache.http.HttpHost;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Api(tags = "搜索相关接口")
@RestController
@RequestMapping("/search")
@RequiredArgsConstructor
public class SearchController {

    private final RestHighLevelClient client = new RestHighLevelClient(RestClient.builder(
                    HttpHost.create("http:192.168.189.129:9200")
            ));
    @ApiOperation("搜索商品")
    @GetMapping("/{id}")
    public ItemDTO search(@PathVariable("id") Long id) throws IOException {
        //1.准备Request对象
        GetRequest request = new GetRequest("items").id(id.toString());
        //2.发送请求
        GetResponse response = client.get(request, RequestOptions.DEFAULT);
        //3.解析响应结果
        String json = response.getSourceAsString();
        ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
        //4.返回
        return BeanUtils.copyProperties(itemDoc, ItemDTO.class);
    }

    @ApiOperation("搜索商品")
    @GetMapping("/list")
    public PageDTO<ItemDTO> search(ItemPageQuery query) throws IOException {
        // 1.搜索

        // 1.1准备Request对象
        SearchRequest request = new SearchRequest("items");

        // 1.2组织DSL参数
        BoolQueryBuilder bool = QueryBuilders.boolQuery();

        // 1.2.1 关键字搜索
        if(StrUtil.isNotBlank(query.getKey())) {
            bool.must(QueryBuilders.matchQuery("name", query.getKey()));
        }

        // 1.2.2 分类过滤
        if(StrUtil.isNotBlank(query.getCategory())) {
            bool.filter(QueryBuilders.termQuery("category", query.getCategory()));
        }

        // 1.2.3 品牌过滤
        if(StrUtil.isNotBlank(query.getBrand())) {
            bool.filter(QueryBuilders.termQuery("category", query.getBrand()));
        }

        // 1.2.4 价格最大值过滤
        if(query.getMaxPrice() != null) {
            bool.filter(QueryBuilders.rangeQuery("price").lte(query.getMaxPrice()));
        }

        // 1.2.5 价格最小值过滤
        if(query.getMinPrice() != null) {
            bool.filter(QueryBuilders.rangeQuery("price").gte(query.getMinPrice()));
        }

        request.source().query(bool);

        // 1.3分页
        request.source().from((query.getPageNo() - 1) * query.getPageSize()).size(query.getPageSize());

        // 1.4排序
        if(StrUtil.isNotBlank(query.getSortBy())) {
            request.source().sort(query.getSortBy(), query.getIsAsc()? SortOrder.ASC : SortOrder.DESC);
        } else {
            request.source().sort("updateTime", query.getIsAsc()? SortOrder.ASC : SortOrder.DESC);
        }

        // 2.发送请求
        SearchResponse response = client.search(request, RequestOptions.DEFAULT);

        // 3.解析结果
        PageDTO<ItemDTO> itemDTOPageDTO = parseResponseResult(response, query);
        return itemDTOPageDTO;
    }

    private PageDTO<ItemDTO> parseResponseResult(SearchResponse response, ItemPageQuery query) {
        List<ItemDoc> itemDocsList = new ArrayList<>();

        SearchHits searchHitshits = response.getHits();
        // 1.总条数
        long total = searchHitshits.getTotalHits().value;

        // 2.命中的数据
        SearchHit[] hits = searchHitshits.getHits();

        for (SearchHit hit : hits) {
            // 2.1 获取source结果
            String json = hit.getSourceAsString();

            // 2.2 转化为ItemDoc
            ItemDoc itemDoc = JSONUtil.toBean(json, ItemDoc.class);
            itemDocsList.add(itemDoc);
        }

        // 3.转化为ItemDTO集合
        List<ItemDTO> itemDTOS = BeanUtils.copyToList(itemDocsList, ItemDTO.class);

        // 3.1页面大小
        long pageSize = query.getPageSize().longValue();

        // 3.2返回结果
        PageDTO<ItemDTO> itemDTOPageDTO = new PageDTO<>(total, pageSize, itemDTOS);
        return itemDTOPageDTO;
    }
}

package com.monitor.service.parameter;

import lombok.Builder;
import lombok.Getter;

import java.security.InvalidParameterException;

@Builder
@Getter
public class QueryParam {
    private String chain;
    private String address;
    private String contractAddress;
    private String startBlock;
    private String endBlock;
    private Integer page;
    private Integer offset;
    private Integer limit;
    private String method;
    private String sort;
    private String topic;
    private String startTime;
    private String endTime;

    public static String optionalParam(String paramName, Object paramValue) {
        if (paramValue != null) {
            return "&" + paramName + "=" + paramValue;
        }
        return "";
    }

    public static String requiredParam(String paramName, Object paramValue) {
        if (paramValue == null) {
            throw new InvalidParameterException(String.format("%s is null.", paramName));
        }
        return "&" + paramName + "=" + paramValue;
    }

    public static QueryParamBuilder builderFromQueryParam(QueryParam queryParam) {
        QueryParamBuilder builder = new QueryParamBuilder();
        builder.chain(queryParam.getChain())
                .address(queryParam.getAddress())
                .contractAddress(queryParam.getContractAddress())
                .startBlock(queryParam.getStartBlock())
                .endBlock(queryParam.getEndBlock())
                .page(queryParam.getPage())
                .offset(queryParam.getOffset())
                .limit(queryParam.getLimit())
                .method(queryParam.getMethod())
                .sort(queryParam.getSort())
                .topic(queryParam.getTopic())
                .startTime(queryParam.getStartTime())
                .endTime(queryParam.getEndTime());
        return builder;
    }
}
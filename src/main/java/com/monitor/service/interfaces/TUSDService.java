package com.monitor.service.interfaces;

import com.monitor.constants.Token;
import com.monitor.constants.Web3Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

import java.util.Map;

public abstract class TUSDService {

    @Autowired
    public Token.TUSD token;

    @Autowired
    public Web3Provider web3Provider;

    @Value("${web3.provider.url.privatekey.eth}")
    public String privateKey;

    public abstract String queryContract(String chain, String field);

    public abstract String funcContract(String chain, String funcName, Map<Class, Object> fields);

    public abstract String queryController(String chain, String field);
}

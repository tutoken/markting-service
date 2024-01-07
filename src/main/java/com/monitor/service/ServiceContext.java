package com.monitor.service;

import com.monitor.service.interfaces.TUSDService;
import com.monitor.service.interfaces.chain.BlockchainService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component("web3ServiceContext")
public class ServiceContext {
    private static final String DEFAULT = "default";

    @Autowired
    private final Map<String, BlockchainService> web3ServiceMap = new HashMap<>(3);

    @Autowired
    private final Map<String, TUSDService> tusdServiceMap = new HashMap<>(2);

    public BlockchainService chainServiceOf(String chain) {
        return web3ServiceMap.getOrDefault(String.format("%sChainService", chain), web3ServiceMap.get(DEFAULT + "ChainService"));
    }

    public TUSDService tusdServiceOf(String chain) {
        return tusdServiceMap.getOrDefault(String.format("%sTUSDService", chain), tusdServiceMap.get(DEFAULT + "TUSDService"));
    }
}

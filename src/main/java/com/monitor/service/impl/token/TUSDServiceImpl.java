package com.monitor.service.impl.token;

import com.monitor.abi.TUSD;
import com.monitor.abi.TUSDController;
import com.monitor.service.interfaces.TUSDService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.Contract;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.DefaultGasProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.util.Map;

import static com.monitor.constants.Monitor.DECIMAL18;

@Service("defaultTUSDService")
@Slf4j
public class TUSDServiceImpl extends TUSDService {

    @Override
    public String queryContract(String chain, String field) {
        try {
            return this.fromContract(chain, field, token.getContract(chain), TUSD.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "N/A";
        }
    }

    @Override
    public String funcContract(String chain, String funcName, Map<Class, Object> fields) {
        try {
            Method method = this.invokeMethod(TUSD.class, funcName, fields.keySet().toArray(new Class[]{}));
            Object object = this.getContractObject(chain, token.getContract(chain), TUSD.class);

            Object response = this.callMethod(object, method, fields.values().toArray(new Object[]{})).send();

            return response.toString();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "N/A";
        }
    }

    @Override
    public String queryController(String chain, String field) {
        try {
            return this.fromContract(chain, field, token.getController(chain), TUSDController.class);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return "N/A";
        }
    }

    private String fromContract(String chain, String field, String address, Class<? extends Contract> clazz) throws Exception {

        Object object = this.getContractObject(chain, address, clazz);
        Object response = this.callMethod(object, clazz, field).send();

        if (response instanceof BigInteger) {
            return new BigDecimal((BigInteger) response).divide(DECIMAL18, 2, RoundingMode.HALF_UP).toString();
        } else if (response instanceof BigDecimal) {
            return ((BigDecimal) response).divide(DECIMAL18, 2, RoundingMode.HALF_UP).toString();
        }

        return response.toString();
    }

    private Object getContractObject(String chain, String address, Class<? extends Contract> clazz) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if ("".equals(address)) {
            return "";
        }
        Credentials credentials = Credentials.create(this.privateKey);

        Web3j client = Web3j.build(new HttpService(web3Provider.getRpcUrl(chain)));
        DefaultGasProvider gasProvider = new DefaultGasProvider() {
            @Override
            public BigInteger getGasPrice() {
                try {
                    return client.ethGasPrice().send().getGasPrice();
                } catch (Exception exception) {
                    log.warn(exception.getMessage(), exception);
                    return BigInteger.ZERO;
                }
            }
        };
        return clazz.getMethod("load", String.class, Web3j.class, Credentials.class, ContractGasProvider.class).invoke(null, address, client, credentials, gasProvider);
    }

    private RemoteFunctionCall<Object> callMethod(Object contract, Class<? extends Contract> clazz, String name) throws Exception {
        Method method = clazz.getMethod(name);
        Object object = method.invoke(contract);
        return (RemoteFunctionCall<Object>) object;
    }

    private Method invokeMethod(Class<? extends Contract> clazz, String name, Class... types) {
        try {
            return clazz.getMethod(name, types);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    private RemoteFunctionCall<Object> callMethod(Object contract, Method method, Object... values) throws Exception {
        Object object = method.invoke(contract, values);
        return (RemoteFunctionCall<Object>) object;
    }
}

package com.monitor.schedule.definition;

import com.monitor.database.model.Transaction;
import com.monitor.database.repository.TransactionRepository;
import com.monitor.schedule.base.ScheduleJobDefinition;
import com.monitor.service.parameter.QueryParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service("SyncTransactionsJob")
@Slf4j
public class SyncTransactionsJob extends ScheduleJobDefinition {

    @Autowired
    private TransactionRepository transactionRepository;

    @Override
    protected void run() {
        int offset = 8000;

        for (String chain : monitor.getNativelyChains()) {

            String lastBlockKey = chain + "_total_transaction_last_time";

//            BigInteger startBlock = new BigInteger(redisUtil.getStringValueOrDefault(lastBlockKey, "0x0")
//                    .substring(2), 16);
            String endBlock = serviceContext.chainServiceOf(chain).getLatestBlockNumber(chain);

            QueryParam queryParam = QueryParam.builder().contractAddress(token.getContract(chain)).startBlock(redisUtil.getStringValueOrDefault(lastBlockKey, "0x0"))
//                    .topic("TRANSFER")
                    .endBlock(endBlock).offset(offset).build();

            Map<String, Map<String, String>> result = serviceContext.chainServiceOf(chain).getTransactionsByEvent(queryParam);

            log.info(String.format("%s sum is %d", chain, result.size()));

            Set<Transaction> set = new HashSet<>();
            for (Map<String, String> info : result.values()) {
                Transaction.TransactionBuilder builder = Transaction.builder();
                Class<Transaction.TransactionBuilder> clazz = Transaction.TransactionBuilder.class;
                Method[] methods = clazz.getDeclaredMethods();
                for (Method method : methods) {
                    try {
                        method.invoke(builder, info.get(method.getName()));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        throw new RuntimeException(e);
                    }
                }
                set.add(builder.build());
            }

            transactionRepository.saveAll(set);

            redisUtil.saveStringValue(lastBlockKey, endBlock, 0, null);
        }
    }
}

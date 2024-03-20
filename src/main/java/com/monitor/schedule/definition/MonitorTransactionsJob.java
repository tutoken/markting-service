package com.monitor.schedule.definition;

import com.monitor.constants.Token;
import com.monitor.database.model.Transaction;
import com.monitor.database.repository.TransactionRepository;
import com.monitor.schedule.base.ScheduleJobDefinition;
import com.monitor.service.parameter.Message;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Transaction type to be monitor：
 * REQUEST_MINT token controller
 * RATIFIED_MINT token controller
 * BURN(REDEMPTION) operation on tron chain
 */
@Service("MonitorTransactionsJob")
@Slf4j
public class MonitorTransactionsJob extends ScheduleJobDefinition {

    private static final String[] title = new String[]{"transactionHash", "type", "status", "amount", "createdAt"};

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private Token.TUSD token;

    @Override
    public void run() {
        long fromTime = this.currentTime;
        long toTime = fromTime - (1000 * 60 * 60);

        List<Transaction> transactions = transactionRepository.findByCreatedAtBetween(fromTime, toTime);
        if (CollectionUtils.isEmpty(transactions)) {
            log.info("No transactions found.");
            return;
        }

        alarm(transactions, transaction -> transaction.getType().equals("REQUEST_MINT"), "Request mint in the past one hour.");
        alarm(transactions, transaction -> transaction.getType().equals("RATIFIED_MINT"), "Ratified mint in the past one hour.");
        alarm(transactions, transaction -> transaction.getChain().equals("tron") && transaction.getType().equals("REDEEM"), "Tron burn in the past one hour.");
        alarm(transactions,
                transaction ->
                        transaction.getContractAddress().equals(token.getController(transaction.getChain())) && transaction.getStatus().equals("failed"),
                "Failed transactions in the past one hour.");
    }

    private void alarm(List<Transaction> transactions, Function<Transaction, Boolean> function, String alarm) {
        Map<String, Map<String, String>> transactionInfo = transactions.stream().filter(function::apply).collect(Collectors.toMap(Transaction::getTransactionHash, this::toFieldMap, (existingMap, newMap) -> {
            existingMap.putAll(newMap);
            return existingMap;
        }));

        if (!CollectionUtils.isEmpty(transactionInfo)) {
            Message message = new Message();
            message.addDirectMessage(alarm);
            message.addTable(title, transactionInfo);
//            message.addWaring("Hosea", "Lily");
            slackService.sendMessage(getDefaultChannel(), message);
            noticeRecipients();
        }
    }

    private Map<String, String> toFieldMap(Transaction transaction) {
        return Map.of(
                "transactionHash", transaction.getTransactionHash(),
                "type", transaction.getType(),
                "amount", transaction.getAmount().toString(),
                "status", transaction.getStatus(),
                "createdAt", DateUtils.formatDate(transaction.getCreatedAt())
        );
    }
}

package com.monitor.schedule.definition;

import com.monitor.constants.Slack;
import com.monitor.service.parameter.QueryParam;
import com.monitor.service.parameter.QueryParam.QueryParamBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static java.lang.String.format;

@Service("MonitorTransactionsJob")
@Slf4j
public class MonitorTransactionsJob extends ScheduleJobDefinition {

    @Override
    public void run() {
        this.start();

        slackService.init();

        QueryParamBuilder queryParamBuilder = QueryParam.builder().startTime(currentTimeStamp).endTime(lastTimeStamp);

        for (String chain : monitor.getNativelyChains()) {
            try {
                queryParamBuilder.chain(chain).startBlock(serviceContext.chainServiceOf(chain).getBlockByTimestamp(currentTimeStamp))
                        .endBlock(serviceContext.chainServiceOf(chain).getBlockByTimestamp(lastTimeStamp));

                // set address to controller
                QueryParam queryParam = queryParamBuilder.address(token.getController(chain)).build();
                this.controllerTransactions(queryParam);

                queryParamBuilder.method("eth_getLogs");

                // TODO 根据旧运行日志，确认是否查询controller下的相关transaction
                queryParam = queryParamBuilder.topic("REQUEST_MINT").build();
                this.specifiedTransactions(queryParam);

                queryParam = queryParamBuilder.topic("RATIFIED_MINT").build();
                this.specifiedTransactions(queryParam);

                if ("tron".equals(chain)) {
                    queryParam = queryParamBuilder.address(token.getContract(chain)).topic("TRANSFER").build();
                    this.burnTransactions(queryParam);
                }

            } catch (Exception e) {
                log.error(e.getMessage(), e);

                slackService.sendNotice("test", format("%s transaction exception, lastTimeStamp: %s, currentTimeStamp: %s"
                        , chain, lastTimeStamp, currentTimeStamp));
                slackService.sendNotice("test", e.getMessage());
                slackService.sendNotice("test", Slack.WARNING + slack.getID("Hosea"));
            }
        }

        slackService.sendAsTable("tusd");

        this.end();
    }

    /**
     * failed transactions of controller
     *
     * @param queryParam
     */
    private void controllerTransactions(QueryParam queryParam) {
        Set<Map<String, String>> transactions = web3Service.getTransactionsByAddress(queryParam);
        if (transactions.isEmpty()) {
            return;
        }

        int failedAccount = 0;
        for (Map<String, String> transaction : transactions) {
            if (!serviceContext.chainServiceOf(queryParam.getChain()).getTransactionStatus(transaction.get("hash"))) {
                slackService.addDetail(Slack.ICO.get(queryParam.getChain()) + web3Provider.chainName(queryParam.getChain()), transaction);
                failedAccount++;
            }
        }

        if (failedAccount > 0) {
            slackService.addWarning(Slack.WARNING + slack.getID("Lily") + slack.getID("Hosea"));
        }
    }

    /**
     * request mint and ratified mint
     *
     * @param queryParam
     */
    private void specifiedTransactions(QueryParam queryParam) {
        Map<String, Map<String, String>> result = web3Service.getSpecifiedTransactions(queryParam);

        if (!CollectionUtils.isEmpty(result)) {
            for (Map<String, String> transaction : result.values()) {
                Map<String, String> detail = new HashMap<>();
                detail.put(transaction.get("hash"), transaction.get("topic"));
                slackService.addDetail(Slack.ICO.get(queryParam.getChain()) + web3Provider.chainName(queryParam.getChain()), detail);
            }
            slackService.addWarning(Slack.WARNING + slack.getID("Tahoe") + slack.getID("Lily") + slack.getID("Hosea"));
        }
    }

    /**
     * xBurn transaction
     *
     * @param queryParam
     */
    private void burnTransactions(QueryParam queryParam) {
        Map<String, Map<String, String>> transactions = web3Service.getRedemptionTransactions(queryParam);

        for (Map<String, String> transaction : transactions.values()) {
            String hash = transaction.get("transactionHash");

            if (!serviceContext.chainServiceOf(queryParam.getChain()).getTransactionStatus(hash)) {
                Map<String, String> map = new HashMap<>();
                map.put(hash, "Burn");
                slackService.addDetail(Slack.ICO.get(queryParam.getChain()) + web3Provider.chainName(queryParam.getChain()), map);
                slackService.addWarning(Slack.WARNING + slack.getID("Lily") + slack.getID("Hosea"));
            }
        }
    }
}

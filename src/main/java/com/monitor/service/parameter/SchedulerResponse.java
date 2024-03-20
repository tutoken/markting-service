package com.monitor.service.parameter;

import com.monitor.database.model.SchedulerJobDetail;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Data
public class SchedulerResponse implements Serializable {

    boolean started;
    List<MonitorTaskDetail> monitorTaskDetails = new ArrayList<>();

    public void add(String name, List<SchedulerJobDetail> schedulerJobDetails, String nextRunTime) {
        monitorTaskDetails.add(MonitorTaskDetail.builder().name(name).schedulerJobDetails(schedulerJobDetails).nextRunTime(nextRunTime).build());
    }

    @Builder
    @Data
    static class MonitorTaskDetail implements Serializable {
        String name;
        List<SchedulerJobDetail> schedulerJobDetails;
        String nextRunTime;
    }

}

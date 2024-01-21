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
    List<SchedulerDetail> schedulerDetails = new ArrayList<>();

    public void add(String name, List<SchedulerJobDetail> schedulerJobDetails, String nextRunTime) {
        schedulerDetails.add(SchedulerDetail.builder().name(name).schedulerJobDetails(schedulerJobDetails).nextRunTime(nextRunTime).build());
    }

    @Builder
    @Data
    static class SchedulerDetail implements Serializable {
        String name;
        List<SchedulerJobDetail> schedulerJobDetails;
        String nextRunTime;
    }

}

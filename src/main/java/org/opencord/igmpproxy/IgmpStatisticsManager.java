/*
 * Copyright 2018-present Open Networking Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencord.igmpproxy;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.onlab.util.SafeRecurringTask;
import org.onlab.util.Tools;
import org.onosproject.cfg.ComponentConfigService;
import org.onosproject.event.AbstractListenerManager;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.Dictionary;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;

import com.google.common.base.Strings;

import static org.opencord.igmpproxy.OsgiPropertyConstants.STATISTICS_GENERATION_PERIOD;
import static org.opencord.igmpproxy.OsgiPropertyConstants.STATISTICS_GENERATION_PERIOD_DEFAULT;


/**
 *
 * Process the stats collected in Igmp proxy application. Publish to kafka onos.
 *
 */
@Component(immediate = true, property = {
        STATISTICS_GENERATION_PERIOD + ":Integer=" + STATISTICS_GENERATION_PERIOD_DEFAULT,
})
public class IgmpStatisticsManager extends
                 AbstractListenerManager<IgmpStatisticsEvent, IgmpStatisticsEventListener>
                         implements IgmpStatisticsService {
    private final Logger log = getLogger(getClass());
    private IgmpStatistics igmpStats;

    ScheduledExecutorService executorForIgmp;
    private ScheduledFuture<?> publisherTask;

    private int statisticsGenerationPeriodInSeconds = STATISTICS_GENERATION_PERIOD_DEFAULT;

    @Reference(cardinality = ReferenceCardinality.MANDATORY)
    protected ComponentConfigService cfgService;

    @Override
    public IgmpStatistics getIgmpStats() {
        return igmpStats;
    }

    @Activate
    public void activate() {
        igmpStats = new IgmpStatistics();
        eventDispatcher.addSink(IgmpStatisticsEvent.class, listenerRegistry);
        executorForIgmp = Executors.newScheduledThreadPool(1);
        cfgService.registerProperties(getClass());
        publisherTask = executorForIgmp.scheduleAtFixedRate(SafeRecurringTask.wrap(this::publishStats),
                0, statisticsGenerationPeriodInSeconds, TimeUnit.SECONDS);
        log.info("IgmpStatisticsManager Activated");
    }

    @Modified
    public void modified(ComponentContext context) {
        Dictionary<String, Object> properties = context.getProperties();
        try {
            String s = Tools.get(properties, STATISTICS_GENERATION_PERIOD);
            statisticsGenerationPeriodInSeconds = Strings.isNullOrEmpty(s) ?
                Integer.parseInt(STATISTICS_GENERATION_PERIOD)
                    : Integer.parseInt(s.trim());
        } catch (NumberFormatException ne) {
            log.error("Unable to parse configuration parameter for eventGenerationPeriodInSeconds", ne);
            statisticsGenerationPeriodInSeconds = STATISTICS_GENERATION_PERIOD_DEFAULT;
        }
        publisherTask.cancel(true);
        publisherTask = executorForIgmp.scheduleAtFixedRate(SafeRecurringTask.wrap(this::publishStats),
                0, statisticsGenerationPeriodInSeconds, TimeUnit.SECONDS);
    }

    @Deactivate
    public void deactivate() {
        eventDispatcher.removeSink(IgmpStatisticsEvent.class);
        igmpStats = null;
        publisherTask.cancel(true);
        executorForIgmp.shutdown();
        cfgService.unregisterProperties(getClass(), false);
        log.info("IgmpStatisticsManager Deactivated");
    }

    /**
     * Publishes stats.
     */
    private void publishStats() {

        if (log.isDebugEnabled()) {
            log.debug("Notifying stats: {}", igmpStats);
            log.debug("--IgmpDisconnect--" + igmpStats.getIgmpDisconnect());
            log.debug("--IgmpFailJoinReq--" + igmpStats.getIgmpFailJoinReq());
            log.debug("--IgmpJoinReq--" + igmpStats.getIgmpJoinReq());
            log.debug("--IgmpLeaveReq--" + igmpStats.getIgmpLeaveReq());
            log.debug("--IgmpMsgReceived--" + igmpStats.getIgmpMsgReceived());
            log.debug("--IgmpSuccessJoinRejoinReq--" + igmpStats.getIgmpSuccessJoinRejoinReq());
            log.debug("--Igmpv1MemershipReport--" + igmpStats.getIgmpv1MemershipReport());
            log.debug("--Igmpv2LeaveGroup--" + igmpStats.getIgmpv2LeaveGroup());
            log.debug("--Igmpv2MembershipReport--" + igmpStats.getIgmpv2MembershipReport());
            log.debug("--Igmpv3MembershipQuery--" + igmpStats.getIgmpv3MembershipQuery());
            log.debug("--Igmpv3MembershipReport--" + igmpStats.getIgmpv3MembershipReport());
            log.debug("--InvalidIgmpMsgReceived--" + igmpStats.getInvalidIgmpMsgReceived());
            log.debug("--TotalMsgReceived--  " + igmpStats.getTotalMsgReceived());

            log.debug("--TotalMsgReceived--  " + igmpStats.getTotalMsgReceived());
            log.debug("--TotalMsgReceived--  " + igmpStats.getTotalMsgReceived());
            log.debug("--TotalMsgReceived--  " + igmpStats.getTotalMsgReceived());
        }

        post(new IgmpStatisticsEvent(IgmpStatisticsEvent.Type.STATS_UPDATE, igmpStats));
    }

}

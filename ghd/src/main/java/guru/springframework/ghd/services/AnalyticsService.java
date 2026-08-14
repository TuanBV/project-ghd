package guru.springframework.ghd.services;

import guru.springframework.ghd.dto.analytics.*;
import guru.springframework.ghd.events.PageViewEvent;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

public interface AnalyticsService {

    void track(PageViewTrackRequest request, HttpServletRequest httpRequest);

    void persist(PageViewEvent event);

    AnalyticsOverviewResponse getOverview(String range, String from, String to);

    List<TimeSeriesPointResponse> getTimeSeries(String range, String from, String to);

    List<DeviceBreakdownResponse> getDeviceBreakdown(String range, String from, String to);

    List<TopPageResponse> getTopPages(String range, String from, String to, int limit);

    List<TopReferrerResponse> getTopReferrers(String range, String from, String to, int limit);

    long getOnlineNow();
}

package guru.springframework.ghd.repositories;

import guru.springframework.ghd.entities.PageView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PageViewRepository extends JpaRepository<PageView, UUID> {

    long countByCreatedDateBetween(LocalDateTime from, LocalDateTime to);

    @Query("select count(distinct p.visitorId) from PageView p where p.createdDate between :from and :to")
    long countDistinctVisitorsBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT DATE(created_date) AS day,
                   COUNT(*) AS pageviews,
                   COUNT(DISTINCT visitor_id) AS uniqueVisitors
            FROM page_view
            WHERE created_date BETWEEN :from AND :to
            GROUP BY DATE(created_date)
            ORDER BY DATE(created_date)
            """, nativeQuery = true)
    List<DailyStatProjection> findDailyStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT device_type AS deviceType, COUNT(*) AS count
            FROM page_view
            WHERE created_date BETWEEN :from AND :to
            GROUP BY device_type
            ORDER BY COUNT(*) DESC
            """, nativeQuery = true)
    List<DeviceCountProjection> findDeviceBreakdown(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query(value = """
            SELECT url AS url, COUNT(*) AS count
            FROM page_view
            WHERE created_date BETWEEN :from AND :to
            GROUP BY url
            ORDER BY COUNT(*) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<UrlCountProjection> findTopPages(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                          @Param("limit") int limit);

    @Query(value = """
            SELECT COALESCE(NULLIF(referrer_host, ''), 'Direct') AS referrerHost, COUNT(*) AS count
            FROM page_view
            WHERE created_date BETWEEN :from AND :to
            GROUP BY COALESCE(NULLIF(referrer_host, ''), 'Direct')
            ORDER BY COUNT(*) DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<ReferrerCountProjection> findTopReferrers(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to,
                                                    @Param("limit") int limit);

    @Query(value = """
            SELECT COUNT(*) AS sessionCount,
                   SUM(CASE WHEN cnt = 1 THEN 1 ELSE 0 END) AS bouncedCount,
                   AVG(duration) AS avgDurationSeconds
            FROM (
                SELECT session_id,
                       COUNT(*) AS cnt,
                       TIMESTAMPDIFF(SECOND, MIN(created_date), MAX(created_date)) AS duration
                FROM page_view
                WHERE created_date BETWEEN :from AND :to
                GROUP BY session_id
            ) session_agg
            """, nativeQuery = true)
    SessionStatsProjection findSessionStats(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    interface DailyStatProjection {
        java.sql.Date getDay();
        Long getPageviews();
        Long getUniqueVisitors();
    }

    interface DeviceCountProjection {
        String getDeviceType();
        Long getCount();
    }

    interface UrlCountProjection {
        String getUrl();
        Long getCount();
    }

    interface ReferrerCountProjection {
        String getReferrerHost();
        Long getCount();
    }

    interface SessionStatsProjection {
        Long getSessionCount();
        Long getBouncedCount();
        Double getAvgDurationSeconds();
    }
}

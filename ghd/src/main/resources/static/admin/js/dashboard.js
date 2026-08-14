(function () {
  const dashboard = document.getElementById('analytics-dashboard');
  if (!dashboard) return;

  // Categorical palette (validated for CVD + contrast - see dataviz skill palette.md)
  const COLOR_BLUE = '#2a78d6';
  const COLOR_ORANGE = '#eb6834';
  const COLOR_AQUA = '#1baf7a';
  const INK_SECONDARY = '#52514e';
  const GRIDLINE = '#e1e0d9';

  const DEVICE_COLORS = { DESKTOP: COLOR_BLUE, MOBILE: COLOR_ORANGE, TABLET: COLOR_AQUA };
  const DEVICE_LABELS = { DESKTOP: 'Máy tính', MOBILE: 'Điện thoại', TABLET: 'Máy tính bảng' };

  const API_BASE = '/admin/v1/analytics';
  const REALTIME_POLL_MS = 15000;

  let currentRange = '7d';
  let trafficChart = null;
  let deviceChart = null;

  const els = {
    filterButtons: dashboard.querySelectorAll('.filter-btn'),
    kpiPageviews: document.getElementById('kpi-pageviews'),
    kpiVisitors: document.getElementById('kpi-visitors'),
    kpiOnline: document.getElementById('kpi-online'),
    kpiBounce: document.getElementById('kpi-bounce'),
    kpiDuration: document.getElementById('kpi-duration'),
    deviceLegend: document.getElementById('deviceLegend'),
    topPagesBody: document.getElementById('topPagesBody'),
    topReferrersBody: document.getElementById('topReferrersBody'),
  };

  function fetchJson(path) {
    return fetch(path, { credentials: 'same-origin' })
      .then((res) => {
        if (!res.ok) throw new Error('HTTP ' + res.status);
        return res.json();
      })
      .then((json) => json.body);
  }

  function formatNumber(value) {
    return new Intl.NumberFormat('vi-VN').format(value || 0);
  }

  function formatDuration(seconds) {
    const s = Math.round(seconds || 0);
    const m = Math.floor(s / 60);
    const rem = s % 60;
    return m > 0 ? `${m}p ${rem}s` : `${rem}s`;
  }

  function formatDateLabel(isoDate) {
    const [y, m, d] = isoDate.split('-');
    return `${d}/${m}`;
  }

  function escapeHtml(value) {
    const div = document.createElement('div');
    div.textContent = value == null ? '' : String(value);
    return div.innerHTML;
  }

  function loadOverview(range) {
    fetchJson(`${API_BASE}/overview?range=${range}`)
      .then((data) => {
        els.kpiPageviews.textContent = formatNumber(data.totalPageviews);
        els.kpiVisitors.textContent = formatNumber(data.uniqueVisitors);
        els.kpiBounce.textContent = `${data.bounceRate}%`;
        if (els.kpiDuration) els.kpiDuration.textContent = formatDuration(data.avgDurationSeconds);
      })
      .catch((err) => console.error('Không tải được overview:', err));
  }

  function loadRealtime() {
    fetchJson(`${API_BASE}/realtime`)
      .then((data) => {
        els.kpiOnline.textContent = formatNumber(data.onlineNow);
      })
      .catch((err) => console.error('Không tải được realtime:', err));
  }

  function loadTimeSeries(range) {
    fetchJson(`${API_BASE}/timeseries?range=${range}`)
      .then((points) => {
        const labels = points.map((p) => formatDateLabel(p.date));
        const pageviews = points.map((p) => p.pageviews);
        const visitors = points.map((p) => p.uniqueVisitors);

        if (trafficChart) {
          trafficChart.data.labels = labels;
          trafficChart.data.datasets[0].data = pageviews;
          trafficChart.data.datasets[1].data = visitors;
          trafficChart.update();
          return;
        }

        const ctx = document.getElementById('trafficChart').getContext('2d');
        trafficChart = new Chart(ctx, {
          type: 'line',
          data: {
            labels: labels,
            datasets: [
              {
                label: 'Lượt xem',
                data: pageviews,
                borderColor: COLOR_BLUE,
                backgroundColor: COLOR_BLUE,
                borderWidth: 2,
                pointRadius: 3,
                tension: 0.25,
              },
              {
                label: 'Người dùng duy nhất',
                data: visitors,
                borderColor: COLOR_AQUA,
                backgroundColor: COLOR_AQUA,
                borderWidth: 2,
                pointRadius: 3,
                tension: 0.25,
              },
            ],
          },
          options: {
            responsive: true,
            interaction: { mode: 'index', intersect: false },
            plugins: { legend: { position: 'bottom', labels: { color: INK_SECONDARY } } },
            scales: {
              x: { grid: { display: false }, ticks: { color: INK_SECONDARY } },
              y: { beginAtZero: true, grid: { color: GRIDLINE }, ticks: { color: INK_SECONDARY, precision: 0 } },
            },
          },
        });
      })
      .catch((err) => console.error('Không tải được timeseries:', err));
  }

  function loadDevices(range) {
    fetchJson(`${API_BASE}/devices?range=${range}`)
      .then((rows) => {
        const labels = rows.map((r) => DEVICE_LABELS[r.deviceType] || r.deviceType);
        const data = rows.map((r) => r.count);
        const colors = rows.map((r) => DEVICE_COLORS[r.deviceType] || INK_SECONDARY);

        if (deviceChart) {
          deviceChart.data.labels = labels;
          deviceChart.data.datasets[0].data = data;
          deviceChart.data.datasets[0].backgroundColor = colors;
          deviceChart.update();
        } else {
          const ctx = document.getElementById('deviceChart').getContext('2d');
          deviceChart = new Chart(ctx, {
            type: 'doughnut',
            data: { labels: labels, datasets: [{ data: data, backgroundColor: colors, borderWidth: 2, borderColor: '#fcfcfb' }] },
            options: { responsive: true, plugins: { legend: { display: false } } },
          });
        }

        if (els.deviceLegend) {
          els.deviceLegend.innerHTML = rows
            .map(
              (r) => `<li><span class="legend-dot" style="background:${DEVICE_COLORS[r.deviceType] || INK_SECONDARY}"></span>${escapeHtml(DEVICE_LABELS[r.deviceType] || r.deviceType)} <b>${r.percentage}%</b></li>`
            )
            .join('');
        }
      })
      .catch((err) => console.error('Không tải được devices:', err));
  }

  function loadTopPages(range) {
    fetchJson(`${API_BASE}/top-pages?range=${range}&limit=5`)
      .then((rows) => {
        els.topPagesBody.innerHTML = rows.length
          ? rows.map((r) => `<tr><td>${escapeHtml(r.url)}</td><td>${formatNumber(r.views)}</td></tr>`).join('')
          : '<tr><td colspan="2">Chưa có dữ liệu</td></tr>';
      })
      .catch((err) => console.error('Không tải được top pages:', err));
  }

  function loadTopReferrers(range) {
    fetchJson(`${API_BASE}/top-referrers?range=${range}&limit=5`)
      .then((rows) => {
        els.topReferrersBody.innerHTML = rows.length
          ? rows.map((r) => `<tr><td>${escapeHtml(r.referrer)}</td><td>${formatNumber(r.views)}</td></tr>`).join('')
          : '<tr><td colspan="2">Chưa có dữ liệu</td></tr>';
      })
      .catch((err) => console.error('Không tải được top referrers:', err));
  }

  function loadAll(range) {
    loadOverview(range);
    loadTimeSeries(range);
    loadDevices(range);
    loadTopPages(range);
    loadTopReferrers(range);
  }

  els.filterButtons.forEach((btn) => {
    btn.addEventListener('click', () => {
      if (btn.dataset.range === currentRange) return;
      els.filterButtons.forEach((b) => b.classList.remove('active'));
      btn.classList.add('active');
      currentRange = btn.dataset.range;
      loadAll(currentRange);
    });
  });

  loadAll(currentRange);
  loadRealtime();
  setInterval(loadRealtime, REALTIME_POLL_MS);
})();

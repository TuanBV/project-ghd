package com.example.mcprice.domain;

/** Trang thai chung cho import_runs, crawl_runs, job_runs, website_publish_runs, merchant_sync_runs. */
public enum RunStatus {
    QUEUED, RUNNING, SUCCESS, PARTIAL_SUCCESS, FAILED, CANCELLED
}

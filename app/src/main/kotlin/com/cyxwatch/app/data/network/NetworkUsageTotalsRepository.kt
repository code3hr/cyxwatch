package com.cyxwatch.app.data.network

import com.cyxwatch.app.domain.model.NetworkUsageDailyTotals

interface NetworkUsageTotalsRepository {
    fun readLatestTotals(): NetworkUsageDailyTotals?
    fun writeTotals(totals: NetworkUsageDailyTotals)
}


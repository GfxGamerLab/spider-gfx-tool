package com.example.spidergfxtool

import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Advanced Adaptive DNS Latency Manager.
 * Tests multiple providers in parallel and identifies the fastest route.
 */
object SmartDnsManager {
    private const val TAG = "SmartDnsManager"

    data class DnsProvider(
        val name: String,
        val ip: String,
        val hostname: String? = null,
        val isDefault: Boolean = false
    )

    data class DnsResult(
        val best: DnsProvider,
        val details: Map<String, Long>
    )

    val providers = listOf(
        DnsProvider("Cloudflare", "1.1.1.1", "one.one.one.one"),
        DnsProvider("Google DNS", "8.8.8.8", "dns.google"),
        DnsProvider("Quad9", "9.9.9.9", "dns.quad9.net"),
        DnsProvider("AdGuard", "94.140.14.14", "dns.adguard-dns.com"),
        DnsProvider("ISP Baseline", "google.com", isDefault = true)
    )

    suspend fun findBestDns(): DnsResult = withContext(Dispatchers.IO) {
        val results = mutableMapOf<String, Long>()
        
        val deferreds = providers.map { provider ->
            async {
                val latency = measureLatency(provider.ip)
                provider.name to latency
            }
        }

        deferreds.awaitAll().forEach { (name, latency) ->
            results[name] = latency
        }

        // Find provider with lowest latency (ignoring 999/failed results)
        val bestEntry = results.filter { it.value < 999 }
            .minByOrNull { it.value } 
            ?: results.entries.first() // Fallback to first if all failed

        val bestProvider = providers.first { it.name == bestEntry.key }

        DnsResult(bestProvider, results)
    }

    /**
     * Executes a hardware-level ping command.
     * Logic: ping -c 1 -w 1 <target>
     */
    private fun measureLatency(target: String): Long {
        return try {
            val process = Runtime.getRuntime().exec("ping -c 1 -w 1 $target")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var latency = 999L
            
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("time=") == true) {
                    val timePart = line!!.substringAfter("time=").substringBefore(" ms")
                    latency = timePart.toDouble().toLong()
                    break
                }
            }
            process.waitFor()
            latency
        } catch (e: Exception) {
            Log.e(TAG, "Ping failed for $target", e)
            999L
        }
    }
}

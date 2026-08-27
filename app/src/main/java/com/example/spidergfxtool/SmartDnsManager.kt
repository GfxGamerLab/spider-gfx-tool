package com.example.spidergfxtool

import android.util.Log
import kotlinx.coroutines.*
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Advanced DNS Latency Manager.
 * Tests multiple providers in parallel and identifies the fastest route.
 */
object SmartDnsManager {
    private const val TAG = "SmartDnsManager"

    data class DnsProvider(
        val name: String,
        val ip: String,
        val hostname: String
    )

    data class DnsResult(
        val best: DnsProvider,
        val details: Map<String, Long>
    )

    private val providers = listOf(
        DnsProvider("Cloudflare", "1.1.1.1", "1dot1dot1dot1.cloudflare-dns.com"),
        DnsProvider("Google", "8.8.8.8", "dns.google"),
        DnsProvider("Quad9", "9.9.9.9", "dns.quad9.net")
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

        // Find provider with lowest latency (ignoring 0/failed results)
        val bestEntry = results.filter { it.value > 0 }
            .minByOrNull { it.value } 
            ?: results.entries.first() // Fallback to first if all failed

        val bestProvider = providers.first { it.name == bestEntry.key }

        DnsResult(bestProvider, results)
    }

    private fun measureLatency(ip: String): Long {
        return try {
            val process = Runtime.getRuntime().exec("ping -c 2 -W 1 $ip")
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            var totalTime = 0L
            var count = 0
            
            while (reader.readLine().also { line = it } != null) {
                if (line?.contains("time=") == true) {
                    val timePart = line!!.substringAfter("time=").substringBefore(" ms")
                    totalTime += timePart.toDouble().toLong()
                    count++
                }
            }
            process.waitFor()
            if (count > 0) totalTime / count else 999L
        } catch (e: Exception) {
            Log.e(TAG, "Ping failed for $ip", e)
            999L
        }
    }
}

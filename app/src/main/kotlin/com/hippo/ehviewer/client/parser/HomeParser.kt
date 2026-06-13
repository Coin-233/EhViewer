package com.hippo.ehviewer.client.parser

import com.hippo.ehviewer.client.exception.InsufficientFundsException
import com.hippo.ehviewer.client.exception.ParseException
import com.hippo.ehviewer.client.parseAs
import java.nio.ByteBuffer
import kotlin.random.Random
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

object HomeParser {
    private val TorrentKeyRegex = Regex("Your current key is: <[^>]*>([^<]*)<")
    private val FundsRegex = Regex("Available: ([\\d,]+) Credits.*Available: ([\\d,]+) kGP", RegexOption.DOT_MATCHES_ALL)
    private const val INSUFFICIENT_FUNDS = "Insufficient funds."

    fun parse(body: ByteBuffer) = runCatching {
        unmarshalParsingAs<Limits>(body, ::parseLimit)
    }.getOrElse { throw ParseException("Parse image limits error", it) }

    fun parseResetLimits(body: String) {
        if (body.contains(INSUFFICIENT_FUNDS)) {
            throw InsufficientFundsException()
        }
    }

    fun parseTorrentKey(body: String) = TorrentKeyRegex.find(body)?.run { groupValues[1] }
        ?: throw ParseException("Parse torrent key error")

    fun parseFunds(body: String): Funds {
        FundsRegex.find(body)?.run {
            val fundsC = ParserUtils.parseInt(groupValues[1], 0)
            val fundsGP = ParserUtils.parseInt(groupValues[2], 0)
            return Funds(fundsGP, fundsC)
        }
        throw ParseException("Parse funds error")
    }

    fun parseProxyQuota(body: String): Result {
        val quota = runCatching { body.parseAs<ProxyQuotaResponse>() }
            .getOrElse { throw ParseException("Parse proxy quota error", it) }
        val limits = Limits(
            current = quota.quotaUsed.replace(",", "").toIntOrNull() ?: 0,
            maximum = quota.quotaLimit.replace(",", "").toIntOrNull() ?: 0,
            resetCost = 0,
        )
        val funds = Funds(
            gp = quota.kgp.replace(",", "").toIntOrNull() ?: 0,
            credit = quota.credits.replace(",", "").toIntOrNull() ?: 0,
        )
        return Result(limits, funds)
    }

    data class Result(val limits: Limits, val funds: Funds, private val id: Int = Random.nextInt())
}

@Serializable
data class Funds(val gp: Int, val credit: Int)

@Serializable
data class Limits(val current: Int, val maximum: Int, val resetCost: Int)

@Serializable
private data class ProxyQuotaResponse(
    val credits: String,
    val kgp: String,
    @SerialName("quota_limit") val quotaLimit: String,
    @SerialName("quota_used") val quotaUsed: String,
)

private external fun parseLimit(body: ByteBuffer, limit: Int = body.limit()): Int

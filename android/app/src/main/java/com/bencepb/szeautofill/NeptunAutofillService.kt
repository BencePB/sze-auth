package com.bencepb.szeautofill

import android.app.assist.AssistStructure
import android.os.CancellationSignal
import android.service.autofill.AutofillService
import android.service.autofill.Dataset
import android.service.autofill.FillCallback
import android.service.autofill.FillRequest
import android.service.autofill.FillResponse
import android.service.autofill.SaveCallback
import android.service.autofill.SaveRequest
import android.view.autofill.AutofillId
import android.view.autofill.AutofillValue
import android.widget.RemoteViews

/**
 * Android Autofill Service scoped to a single purpose: recognising the
 * Neptun (neptun-hweb.sze.hu) 2FA code field and offering the current
 * TOTP code as a fill suggestion. It ignores every other field/app.
 */
class NeptunAutofillService : AutofillService() {

    private val targetDomain = "neptun-hweb.sze.hu"

    private val codeFieldHints = listOf("otp", "token", "code", "totp", "2fa")

    override fun onFillRequest(
        request: FillRequest,
        cancellationSignal: CancellationSignal,
        callback: FillCallback
    ) {
        val structure = request.fillContexts.lastOrNull()?.structure
        if (structure == null) {
            callback.onSuccess(null)
            return
        }

        if (!structureMatchesNeptun(structure)) {
            callback.onSuccess(null)
            return
        }

        val secret = SecretStore.load(applicationContext)
        if (secret.isNullOrBlank()) {
            callback.onSuccess(null)
            return
        }

        val otpFieldId = findOtpFieldId(structure)
        if (otpFieldId == null) {
            callback.onSuccess(null)
            return
        }

        val code = Totp.generate(secret)

        val presentation = RemoteViews(packageName, android.R.layout.simple_list_item_1).apply {
            setTextViewText(android.R.id.text1, "Neptun code: $code")
        }

        val dataset = Dataset.Builder()
            .setValue(otpFieldId, AutofillValue.forText(code), presentation)
            .build()

        val response = FillResponse.Builder()
            .addDataset(dataset)
            .build()

        callback.onSuccess(response)
    }

    override fun onSaveRequest(request: SaveRequest, callback: SaveCallback) {
        // This service never learns/saves values -- it only ever supplies
        // computed TOTP codes, never persists anything typed by the user.
        callback.onSuccess()
    }

    private fun structureMatchesNeptun(structure: AssistStructure): Boolean {
        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode
            if (nodeMatchesDomain(root)) return true
        }
        return false
    }

    private fun nodeMatchesDomain(node: AssistStructure.ViewNode): Boolean {
        if (node.webDomain?.contains(targetDomain, ignoreCase = true) == true) return true
        for (i in 0 until node.childCount) {
            if (nodeMatchesDomain(node.getChildAt(i))) return true
        }
        return false
    }

    private fun findOtpFieldId(structure: AssistStructure): AutofillId? {
        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode
            val found = searchNode(root)
            if (found != null) return found
        }
        return null
    }

    private fun searchNode(node: AssistStructure.ViewNode): AutofillId? {
        val idEntry = node.idEntry?.lowercase() ?: ""
        val hint = node.hint?.lowercase() ?: ""
        val htmlAttrs = node.htmlInfo?.attributes?.joinToString(" ") { "${it.first}=${it.second}" }?.lowercase() ?: ""

        val isCandidate = codeFieldHints.any {
            idEntry.contains(it) || hint.contains(it) || htmlAttrs.contains(it)
        } || node.autofillHints?.any { it.contains("otp", ignoreCase = true) } == true

        if (isCandidate && node.autofillId != null) {
            return node.autofillId
        }

        for (i in 0 until node.childCount) {
            val result = searchNode(node.getChildAt(i))
            if (result != null) return result
        }
        return null
    }
}

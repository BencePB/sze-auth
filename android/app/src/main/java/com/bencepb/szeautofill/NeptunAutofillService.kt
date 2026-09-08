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
import android.text.InputType
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

    // English + Hungarian keywords that might appear in id/hint/html attrs
    // of the Neptun 2FA field ("kod"/"k\u00f3d" = code, "hitelesito" = authenticator,
    // "azonosito" = identification).
    private val codeFieldHints = listOf(
        "otp", "token", "code", "totp", "2fa", "onetimecode", "smsotp",
        "kod", "k\u00f3d", "hitelesito", "hiteles\u00edt\u0151", "azonosito", "azonos\u00edt\u00f3", "pin"
    )

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
        val editableNodes = mutableListOf<AssistStructure.ViewNode>()
        for (i in 0 until structure.windowNodeCount) {
            val root = structure.getWindowNodeAt(i).rootViewNode
            collectEditableNodes(root, editableNodes)
        }

        // Pass 1: keyword match against id/hint/html attributes/autofill hints
        for (node in editableNodes) {
            if (nodeMatchesKeyword(node) && node.autofillId != null) {
                return node.autofillId
            }
        }

        // Pass 2: fallback -- if the whole page has exactly one editable/text
        // field (typical for a dedicated 2FA screen), assume that's the one.
        if (editableNodes.size == 1) {
            return editableNodes[0].autofillId
        }

        // Pass 3: fallback -- prefer a numeric-input field with a short max
        // length (6-digit codes), if there's exactly one such field.
        val numericShortFields = editableNodes.filter { node ->
            val isNumeric = (node.inputType and InputType.TYPE_CLASS_NUMBER) != 0
            val shortLength = node.autofillHints == null || true // maxLength not always reported
            isNumeric
        }
        if (numericShortFields.size == 1) {
            return numericShortFields[0].autofillId
        }

        return null
    }

    private fun nodeMatchesKeyword(node: AssistStructure.ViewNode): Boolean {
        val idEntry = node.idEntry?.lowercase() ?: ""
        val hint = node.hint?.lowercase() ?: ""
        val htmlAttrs = node.htmlInfo?.attributes
            ?.joinToString(" ") { "${it.first}=${it.second}" }
            ?.lowercase() ?: ""
        val autofillHintsMatch = node.autofillHints?.any {
            it.lowercase().contains("otp") || it.lowercase().contains("onetime")
        } == true

        return autofillHintsMatch || codeFieldHints.any {
            idEntry.contains(it) || hint.contains(it) || htmlAttrs.contains(it)
        }
    }

    private fun collectEditableNodes(
        node: AssistStructure.ViewNode,
        out: MutableList<AssistStructure.ViewNode>
    ) {
        val isEditableHtmlInput = node.htmlInfo?.tag?.equals("input", ignoreCase = true) == true
        val hasAutofillId = node.autofillId != null
        val looksEditable = node.isEnabled && hasAutofillId &&
            (isEditableHtmlInput || node.className?.contains("EditText") == true || node.inputType != 0)

        if (looksEditable) {
            out.add(node)
        }

        for (i in 0 until node.childCount) {
            collectEditableNodes(node.getChildAt(i), out)
        }
    }
}

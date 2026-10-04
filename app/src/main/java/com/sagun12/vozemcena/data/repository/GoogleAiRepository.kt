package com.sagun12.vozemcena.data.repository

import com.sagun12.vozemcena.data.remote.GoogleAiClient

/**
 * Repositório de consultas à API Google AI (Gemini) para recursos de roteiro,
 * sugestão de cena e nome de voz do aplicativo.
 */
class GoogleAiRepository(
    private val googleAiClient: GoogleAiClient
) {

    val isConfigured: Boolean
        get() = googleAiClient.isConfigured

    /** Envia um prompt livre ao Gemini e devolve o texto gerado. */
    suspend fun generateText(prompt: String): Result<String> =
        googleAiClient.generateText(prompt)

    /**
     * Sugere um roteiro de dublagem a partir de um tema/tópico, no idioma escolhido.
     * Retorna apenas o texto do roteiro, sem comentários adicionais.
     */
    suspend fun suggestDubbingScript(topic: String, language: String): Result<String> {
        val idioma = when (language.lowercase()) {
            "en" -> "inglês"
            "es" -> "espanhol"
            else -> "português do Brasil"
        }
        val prompt = """
            |Crie um roteiro curto de dublagem em $idioma (2 a 4 frases) sobre o seguinte tema:
            |"$topic"
            |
            |Regras:
            |- Retorne APENAS o texto do roteiro, pronto para ser lido em voz alta.
            |- Não inclua títulos, marcadores, aspas ou instruções.
            |- Mantenha um tom envolvente e natural para narração.
        """.trimMargin()

        return googleAiClient.generateText(prompt = prompt, temperature = 0.8f)
    }

    /**
     * Sugere uma descrição curta de cena para um trecho de roteiro já existente.
     */
    suspend fun suggestSceneDescription(sceneText: String, language: String = "pt"): Result<String> {
        val prompt = """
            |Descreva em uma única frase, em ${if (language == "en") "inglês" else "português"},
            |a cena e o tom de entonação adequados ao seguinte trecho de narração:
            |
            |"$sceneText"
            |
            |Retorne apenas a descrição, sem títulos ou listas.
        """.trimMargin()

        return googleAiClient.generateText(prompt = prompt, temperature = 0.6f)
    }

    /**
     * Sugere um nome curto e um tom para um novo perfil de voz clonada.
     */
    suspend fun suggestVoiceName(sampleDescription: String): Result<String> {
        val prompt = """
            |Com base na descrição de uma amostra de voz a seguir, sugira um nome curto
            |(máximo 3 palavras) e um tom para o perfil de voz.
            |
            |Amostra: "$sampleDescription"
            |
            |Responda no formato: "Nome — tom".
        """.trimMargin()

        return googleAiClient.generateText(prompt = prompt, temperature = 0.9f)
    }
}

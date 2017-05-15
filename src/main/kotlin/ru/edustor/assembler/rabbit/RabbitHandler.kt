package ru.edustor.assembler.rabbit

import com.itextpdf.kernel.pdf.PdfDocument
import com.itextpdf.kernel.pdf.PdfReader
import com.itextpdf.kernel.pdf.PdfWriter
import com.itextpdf.kernel.utils.PdfMerger
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.*
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import ru.edustor.assembler.exception.PageFileNotFoundException
import ru.edustor.commons.models.rabbit.processing.documents.DocumentAssembleRequest
import ru.edustor.commons.models.rabbit.processing.documents.DocumentAssembledEvent
import ru.edustor.commons.storage.service.BinaryObjectStorageService
import ru.edustor.commons.storage.service.BinaryObjectStorageService.ObjectType.ASSEMBLED_DOCUMENT
import ru.edustor.commons.storage.service.BinaryObjectStorageService.ObjectType.PAGE
import java.io.ByteArrayOutputStream
import java.time.Instant

@Component
open class RabbitHandler(var storage: BinaryObjectStorageService,
                         val rabbitTemplate: RabbitTemplate) {
    val logger: Logger = LoggerFactory.getLogger(RabbitHandler::class.java)

    @RabbitListener(bindings = arrayOf(QueueBinding(
            value = Queue("assembler.edustor/inbox", durable = "true", arguments = arrayOf(
                    Argument(name = "x-dead-letter-exchange", value = "reject.edustor")
            )),
            exchange = Exchange("internal.edustor", type = ExchangeTypes.TOPIC,
                    ignoreDeclarationExceptions = "true",
                    durable = "true"),
            key = "requested.assemble.documents.processing"
    )))
    fun processRequest(request: DocumentAssembleRequest) {
        logger.info("Assembling document ${request.documentId} for request ${request.requestId}")

        if (request.pages.isEmpty()) {
            storage.delete(ASSEMBLED_DOCUMENT, request.documentId)
            return
        }

        val outputStream = ByteArrayOutputStream()
        val result = PdfDocument(PdfWriter(outputStream))
        val merger = PdfMerger(result)

        request.pages.forEachIndexed { i, page ->
            storage.get(PAGE, page.fileId)?.use { pageInputStream ->
                PdfDocument(PdfReader(pageInputStream)).use { pageDocument ->
                    merger.merge(pageDocument, 1, 1)
                }
            } ?: throw PageFileNotFoundException("Failed to find ${page.fileId} file " +
                    "used by ${request.documentId} document (request ${request.requestId})")
            logger.info("[${i + 1}/${request.pages.size}] processed page file ${page.fileId} for request ${request.requestId}:")
        }

        result.close()
        val resultBytes = outputStream.toByteArray()
        storage.put(ASSEMBLED_DOCUMENT, request.documentId, resultBytes.inputStream(), resultBytes.size.toLong())

        val assembledEvent = DocumentAssembledEvent(requestId = request.requestId, documentId = request.documentId,
                timestamp = Instant.now(), succeed = true)

        rabbitTemplate.convertAndSend(
                "internal.edustor",
                "finished.assemble.documents.processing",
                assembledEvent
        )

        logger.info("Document assembled: ${request.documentId}")
    }
}
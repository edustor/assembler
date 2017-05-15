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
import ru.edustor.commons.storage.service.BinaryObjectStorageService
import ru.edustor.commons.storage.service.BinaryObjectStorageService.ObjectType.ASSEMBLED_DOCUMENT
import ru.edustor.commons.storage.service.BinaryObjectStorageService.ObjectType.PAGE
import java.io.ByteArrayOutputStream

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
        logger.info("Assembling document ${request.documentId}")

        if (request.pages.isEmpty()) {
            storage.delete(ASSEMBLED_DOCUMENT, request.documentId)
            return
        }

        val outputStream = ByteArrayOutputStream()
        val result = PdfDocument(PdfWriter(outputStream))
        val merger = PdfMerger(result)

        request.pages.forEach { page ->
            storage.get(PAGE, page.fileId)?.use { pageInputStream ->
                PdfDocument(PdfReader(pageInputStream)).use { pageDocument ->
                    merger.merge(pageDocument, 1, 1)
                }
            } ?: throw PageFileNotFoundException("Failed to find ${page.fileId} file used by ${request.documentId} document")
            logger.info("Document ${request.documentId}: processed page file ${page.fileId}")
        }

        result.close()
        val resultBytes = outputStream.toByteArray()
        storage.put(ASSEMBLED_DOCUMENT, request.documentId, resultBytes.inputStream(), resultBytes.size.toLong())

        logger.info("File processing finished: ${request.documentId}")
    }
}
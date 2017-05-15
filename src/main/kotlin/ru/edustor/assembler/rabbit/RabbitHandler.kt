package ru.edustor.assembler.rabbit

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.amqp.core.ExchangeTypes
import org.springframework.amqp.rabbit.annotation.*
import org.springframework.amqp.rabbit.annotation.Queue
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Component
import ru.edustor.commons.storage.service.BinaryObjectStorageService
import ru.edustor.commons.models.rabbit.processing.documents.DocumentAssembleRequest

@Component
open class RabbitHandler(var storage: BinaryObjectStorageService, val rabbitTemplate: RabbitTemplate) {
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

        logger.info("File processing finished: ${request.documentId}")
    }
}
package ru.edustor.assembler.exception

import org.springframework.amqp.AmqpRejectAndDontRequeueException

class PageFileNotFoundException(msg: String) : AmqpRejectAndDontRequeueException(msg)
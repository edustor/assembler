package ru.edustor.assembler.model

import java.time.Instant
import javax.persistence.Entity
import javax.persistence.Id

@Entity
data class DocumentStatus(
        @Id val documentId: String,
        var lastAssembleRequestTimestamp: Instant? = null
)
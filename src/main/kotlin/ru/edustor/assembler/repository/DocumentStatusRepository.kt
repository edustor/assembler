package ru.edustor.assembler.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import ru.edustor.assembler.model.DocumentStatus

@Repository
interface DocumentStatusRepository : JpaRepository<DocumentStatus, String>

fun DocumentStatusRepository.getForAccountId(id: String): DocumentStatus {
    return this.findOne(id) ?: let {
        val a = DocumentStatus(id)
        this.save(a)
        a
    }
}
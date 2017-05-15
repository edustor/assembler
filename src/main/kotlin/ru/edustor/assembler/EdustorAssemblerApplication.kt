package ru.edustor.assembler

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.data.jpa.convert.threeten.Jsr310JpaConverters

@SpringBootApplication
@EntityScan(basePackageClasses = arrayOf(EdustorAssemblerApplication::class, Jsr310JpaConverters::class))
open class EdustorAssemblerApplication
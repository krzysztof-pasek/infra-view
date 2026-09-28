package com.infraView.device.database

import org.springframework.data.jpa.repository.JpaRepository

interface SpringDataThermalFrameRepository : JpaRepository<ThermalFrameJpaEntity, Long>

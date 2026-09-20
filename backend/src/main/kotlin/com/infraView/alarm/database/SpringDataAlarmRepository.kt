package com.infraView.alarm.database

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface SpringDataAlarmRepository : JpaRepository<AlarmJpaEntity, Int>

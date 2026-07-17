package com.infraView.alarm

import org.springframework.stereotype.Repository
import org.springframework.data.jpa.repository.JpaRepository


@Repository
interface AlarmRepository : JpaRepository<AlarmEntity, Long> {

}
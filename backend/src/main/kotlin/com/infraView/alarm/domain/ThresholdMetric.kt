package com.infraView.alarm.domain

enum class ThresholdMetric(val alarmType: AlarmType) {
    TEMPERATURE(AlarmType.HIGH_TEMPERATURE),
    CO2_PPM(AlarmType.HIGH_CO2),
    TVOC_PPB(AlarmType.HIGH_TVOC)
}

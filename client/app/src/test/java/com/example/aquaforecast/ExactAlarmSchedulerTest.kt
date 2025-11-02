package com.example.aquaforecast

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.aquaforecast.data.workers.ExactAlarmScheduler
import com.example.aquaforecast.data.workers.FeedingAlarmReceiver
import com.example.aquaforecast.domain.model.FeedingSchedule
import com.example.aquaforecast.domain.model.Species
import io.mockk.*
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.LocalTime

/**
 * Unit tests for ExactAlarmScheduler
 * Tests alarm scheduling logic, timing calculations, and edge cases
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.S]) // Test on Android 12
class ExactAlarmSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var scheduler: ExactAlarmScheduler

    @Before
    fun setup() {
        context = mockk(relaxed = true)
        alarmManager = mockk(relaxed = true)

        every { context.getSystemService(Context.ALARM_SERVICE) } returns alarmManager
        every { context.packageName } returns "com.example.aquaforecast"

        scheduler = ExactAlarmScheduler(context)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `scheduleNextAlarm schedules alarm for active schedule`() {
        // Given an active feeding schedule
        val schedule = createTestSchedule(
            isActive = true,
            startDate = LocalDate.now().minusDays(1),
            endDate = LocalDate.now().plusDays(30),
            feedingTime = LocalTime.of(14, 30) // 2:30 PM
        )

        // Mock Android 12+ permission
        every { alarmManager.canScheduleExactAlarms() } returns true

        // When scheduling
        scheduler.scheduleNextAlarm(schedule)

        // Then exact alarm should be set
        verify {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                any(),
                any()
            )
        }
    }

    @Test
    fun `scheduleNextAlarm does not schedule for inactive schedule`() {
        // Given an inactive schedule
        val schedule = createTestSchedule(isActive = false)

        // When scheduling
        scheduler.scheduleNextAlarm(schedule)

        // Then no alarm should be set
        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
    }

    @Test
    fun `scheduleNextAlarm does not schedule for ended schedule`() {
        // Given a schedule that has ended
        val schedule = createTestSchedule(
            isActive = true,
            startDate = LocalDate.now().minusDays(30),
            endDate = LocalDate.now().minusDays(1)
        )

        // When scheduling
        scheduler.scheduleNextAlarm(schedule)

        // Then no alarm should be set
        verify(exactly = 0) {
            alarmManager.setExactAndAllowWhileIdle(any(), any(), any())
        }
    }

    @Test
    fun `scheduleNextAlarm uses correct intent extras`() {
        // Given an active schedule
        val schedule = createTestSchedule(
            id = 123L,
            name = "Morning Feed",
            pondName = "Pond A",
            isActive = true
        )

        every { alarmManager.canScheduleExactAlarms() } returns true

        // Mock static PendingIntent
        val capturedIntent = slot<Intent>()
        val mockPendingIntent = mockk<PendingIntent>(relaxed = true)
        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(
                any(),
                any(),
                capture(capturedIntent),
                any()
            )
        } returns mockPendingIntent

        // When scheduling
        scheduler.scheduleNextAlarm(schedule)

        // Then intent should have correct extras
        assert(capturedIntent.captured.action == FeedingAlarmReceiver.ACTION_FEEDING_ALARM)
        assert(capturedIntent.captured.getLongExtra(FeedingAlarmReceiver.EXTRA_SCHEDULE_ID, -1) == 123L)
        assert(capturedIntent.captured.getStringExtra(FeedingAlarmReceiver.EXTRA_SCHEDULE_NAME) == "Morning Feed")
        assert(capturedIntent.captured.getStringExtra(FeedingAlarmReceiver.EXTRA_POND_NAME) == "Pond A")
    }

    @Test
    fun `scheduleNextAlarm handles future feeding time today`() {
        // Given schedule with feeding time later today
        val futureTime = LocalTime.now().plusHours(2)
        val schedule = createTestSchedule(
            isActive = true,
            feedingTime = futureTime
        )

        every { alarmManager.canScheduleExactAlarms() } returns true

        val capturedTriggerTime = slot<Long>()
        every {
            alarmManager.setExactAndAllowWhileIdle(
                any(),
                capture(capturedTriggerTime),
                any()
            )
        } just Runs

        // When scheduling
        scheduler.scheduleNextAlarm(schedule)

        // Then trigger time should be today
        val triggerTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(capturedTriggerTime.captured),
            java.time.ZoneId.systemDefault()
        )

        assert(triggerTime.toLocalDate() == LocalDate.now())
        assert(triggerTime.toLocalTime().hour == futureTime.hour)
        assert(triggerTime.toLocalTime().minute == futureTime.minute)
    }

    @Test
    fun `scheduleNextAlarm handles past feeding time schedules for tomorrow`() {
        // Given schedule with feeding time in the past today
        val pastTime = LocalTime.of(6, 0) // 6 AM (assuming tests run after 6 AM)
        val schedule = createTestSchedule(
            isActive = true,
            feedingTime = pastTime,
            startDate = LocalDate.now().minusDays(1),
            endDate = LocalDate.now().plusDays(30)
        )

        every { alarmManager.canScheduleExactAlarms() } returns true

        val capturedTriggerTime = slot<Long>()
        every {
            alarmManager.setExactAndAllowWhileIdle(
                any(),
                capture(capturedTriggerTime),
                any()
            )
        } just Runs

        // When scheduling
        scheduler.scheduleNextAlarm(schedule)

        // Then trigger time should be tomorrow if current time is after 6 AM
        val triggerTime = java.time.LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(capturedTriggerTime.captured),
            java.time.ZoneId.systemDefault()
        )

        if (LocalTime.now().isAfter(pastTime)) {
            assert(triggerTime.toLocalDate() == LocalDate.now().plusDays(1))
        } else {
            assert(triggerTime.toLocalDate() == LocalDate.now())
        }
    }

    @Test
    fun `cancelAlarm cancels existing alarm`() {
        // Given a schedule ID
        val scheduleId = 456L

        val mockPendingIntent = mockk<PendingIntent>(relaxed = true)
        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(
                any(),
                scheduleId.toInt(),
                any(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
        } returns mockPendingIntent

        // When canceling
        scheduler.cancelAlarm(scheduleId)

        // Then alarm and pending intent should be canceled
        verify { alarmManager.cancel(mockPendingIntent) }
        verify { mockPendingIntent.cancel() }
    }

    @Test
    fun `cancelAlarm handles non-existent alarm gracefully`() {
        // Given a schedule ID with no existing alarm
        val scheduleId = 789L

        mockkStatic(PendingIntent::class)
        every {
            PendingIntent.getBroadcast(
                any(),
                scheduleId.toInt(),
                any(),
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
            )
        } returns null

        // When canceling
        scheduler.cancelAlarm(scheduleId)

        // Then no exception should be thrown
        verify(exactly = 0) { alarmManager.cancel(any<PendingIntent>()) }
    }

    @Test
    fun `scheduleNextAlarm uses fallback for devices without exact alarm permission`() {
        // Given Android 12+ without exact alarm permission
        val schedule = createTestSchedule(isActive = true)

        every { alarmManager.canScheduleExactAlarms() } returns false

        // When scheduling
        scheduler.scheduleNextAlarm(schedule)

        // Then fallback alarm should be used
        verify {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                any(),
                any()
            )
        }
    }

    // Helper function to create test schedules
    private fun createTestSchedule(
        id: Long = 1L,
        name: String = "Test Schedule",
        pondName: String = "Test Pond",
        isActive: Boolean = true,
        startDate: LocalDate = LocalDate.now(),
        endDate: LocalDate = LocalDate.now().plusDays(30),
        feedingTime: LocalTime = LocalTime.of(12, 0)
    ): FeedingSchedule {
        return FeedingSchedule(
            id = id,
            name = name,
            pondId = 1L,
            pondName = pondName,
            startDate = startDate,
            endDate = endDate,
            feedingTime = feedingTime,
            isActive = isActive,
            createdAt = System.currentTimeMillis()
        )
    }
}

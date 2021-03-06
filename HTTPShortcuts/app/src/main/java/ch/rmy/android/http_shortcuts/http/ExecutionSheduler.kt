package ch.rmy.android.http_shortcuts.http

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.support.annotation.RequiresApi
import ch.rmy.android.http_shortcuts.realm.Controller
import ch.rmy.android.http_shortcuts.utils.logException
import ch.rmy.android.http_shortcuts.utils.mapIf
import java.util.*

object ExecutionScheduler {

    fun schedule(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                scheduleService(context)
            }
        } catch (e: Exception) {
            logException(e)
        }
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    private fun scheduleService(context: Context) {
        Controller().use { controller ->
            val pendingExecutions = controller.getShortcutsPendingExecution()
            pendingExecutions.forEach { pendingExecution ->
                val jobInfo = JobInfo.Builder(pendingExecution.shortcutId.toInt(), ComponentName(context, ExecutionService::class.java))
                        .mapIf(pendingExecution.waitUntil != null) {
                            val now = Calendar.getInstance().time
                            val then = pendingExecution.waitUntil!!
                            val latency = then.time - now.time
                            it.mapIf(latency > 0) {
                                it.setMinimumLatency(latency)
                            }
                        }
                        .mapIf(pendingExecution.waitForNetwork) {
                            it.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
                        }
                        .build()
                (context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler).schedule(jobInfo)
            }
        }
    }

}
package org.jetbrains.bazel.progress

import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.platform.util.progress.SequentialProgressReporter
import org.jetbrains.annotations.ApiStatus
import org.jetbrains.bsp.protocol.TaskId

val Project.syncConsole: TaskConsole
  @ApiStatus.Internal
  get() = service<ConsoleService>().syncConsole

@ApiStatus.Internal
suspend fun <T> TaskConsole.withSubtask(
  subtaskId: TaskId,
  message: String,
  block: suspend (subtaskId: TaskId) -> T,
): T {
  startSubtask(subtaskId, message)
  try {
    val result = block(subtaskId)
    finishSubtask(subtaskId)
    return result
  }
  catch (ex: Throwable) {
    finishSubtask(subtaskId, result = FailureResultImpl(ex))
    throw ex
  }
}

@ApiStatus.Internal
suspend fun <T> Project.withSubtask(
  reporter: SequentialProgressReporter,
  subtaskId: TaskId,
  text: String,
  block: suspend (subtaskId: TaskId) -> T,
) {
  reporter.indeterminateStep(text) {
    syncConsole.withSubtask(
      subtaskId = subtaskId,
      message = text,
      block = block,
    )
  }
}

package com.forseti

/**
 * One splash/bootstrap per process. Avoids re-loading the FRCP PDF after language
 * selection or configuration changes that used to call [android.app.Activity.recreate].
 */
object ForsetiBootstrap {
    @Volatile
    var hasCompleted: Boolean = false
}

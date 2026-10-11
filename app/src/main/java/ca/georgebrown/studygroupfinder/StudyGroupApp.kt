package ca.georgebrown.studygroupfinder

import android.app.Application
import ca.georgebrown.studygroupfinder.di.AppContainer

/** Application class: creates the [AppContainer] once for the whole app. */
class StudyGroupApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}

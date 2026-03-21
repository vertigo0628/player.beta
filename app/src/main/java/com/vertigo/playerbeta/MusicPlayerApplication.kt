package com.vertigo.playerbeta

import android.app.Application
import android.net.Uri

//application class -runs once when starts
//currently empty but needed for some android features to function

class MusicPlayerApplication : Application() {
    // Used to store a URI picked by the user across configuration changes or between activities
    var pickedUri: Uri? = null
}

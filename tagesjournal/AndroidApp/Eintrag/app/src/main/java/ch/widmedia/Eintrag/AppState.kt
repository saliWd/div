package ch.widmedia.Eintrag

object AppState {
    var dbPassword: CharArray? = null

    fun clear() {
        dbPassword?.fill('0')
        dbPassword = null
        DatabaseHelper.clearInstance()
    }
}

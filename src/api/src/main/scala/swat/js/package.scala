package swat

package object js {

    /**
     * The specified JavaScript code is directly outputted to the compiled code in the place same place.
     * @param javaScriptCode The native JavaScript code, it has to be a compile time constant.
     */
    def native(javaScriptCode: String): Nothing = ???
}

package ni.edu.uca.mediaupload

import com.google.gson.annotations.SerializedName

data class ImgFile(
    @SerializedName("name") val name: String?,
    @SerializedName("is_dir") val isDir: Boolean?
) {
    override fun toString(): String {
        return "{ name: $name, is_dir: $isDir }"
    }
}
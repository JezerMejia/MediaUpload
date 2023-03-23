package ni.edu.uca.mediaupload

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
import androidx.core.graphics.drawable.toBitmap
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import ni.edu.uca.mediaupload.databinding.ImageItemBinding
import java.io.IOException


class ImageAdapter(private val files: List<ImgFile>) :
    RecyclerView.Adapter<ImageAdapter.ImageHolder>() {

    inner class ImageHolder(private val binding: ImageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun load(file: ImgFile) {
            val imgView = binding.imgView
            val txtName = binding.txtName

            val fileName = file.name?.split("/")?.last() ?: "desconocido"

            txtName.text = fileName

            if (file.isDir == true) {
                imgView.setImageResource(R.drawable.baseline_folder_24)
                return
            }

            val regex = Regex("(\\S+(\\.(?i)(jpe?g|png|gif|bmp))$)")
            if (regex.matches(fileName)) {
                imgView.setOnClickListener {
                    onClickImage(file)
                }
                Picasso.get().load(file.name).into(imgView)
                Log.d("myapp", "Load image: $file")
            } else {
                imgView.setImageResource(R.drawable.baseline_insert_drive_file_24)
            }
        }

        @Throws(IOException::class)
        fun saveBitmap(
            context: Context, bitmap: Bitmap, format: Bitmap.CompressFormat,
            mimeType: String, displayName: String
        ): Uri {

            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
                put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DCIM)
            }

            val resolver = context.contentResolver
            var uri: Uri? = null

            try {
                uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IOException("Failed to create new MediaStore record.")

                resolver.openOutputStream(uri)?.use {
                    if (!bitmap.compress(format, 95, it))
                        throw IOException("Failed to save bitmap.")
                } ?: throw IOException("Failed to open output stream.")

                return uri

            } catch (e: IOException) {

                uri?.let { orphanUri ->
                    // Don't leave an orphan entry in the MediaStore
                    resolver.delete(orphanUri, null, null)
                }

                throw e
            }
        }

        fun onClickImage(file: ImgFile) {
            val bitmap = binding.imgView.drawable.toBitmap()

            val uri = saveBitmap(
                binding.root.context,
                bitmap,
                Bitmap.CompressFormat.PNG,
                "image/*",
                file.name!!
            )

            val shareIntent: Intent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_STREAM, uri)
                type = "image/jpeg"
            }
            startActivity(
                binding.root.context,
                Intent.createChooser(shareIntent, "Compartir"),
                null
            )
        }
    }

    override fun onBindViewHolder(holder: ImageHolder, position: Int) {
        holder.load(files[position])
    }

    override fun getItemCount(): Int {
        return files.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageHolder {
        val binding = ImageItemBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ImageHolder(binding)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }
}
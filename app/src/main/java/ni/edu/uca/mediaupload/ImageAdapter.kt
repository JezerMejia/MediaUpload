package ni.edu.uca.mediaupload

import android.content.Intent
import android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
import android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
import android.graphics.Color
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ni.edu.uca.mediaupload.MainActivity.Companion.mainActivity
import ni.edu.uca.mediaupload.databinding.ImageItemBinding
import java.io.File
import java.net.URL


class ImageAdapter(private val files: List<ImgFile>) :
    RecyclerView.Adapter<ImageAdapter.ImageHolder>() {

    val selectedItems = ArrayList<ImgFile>()

    inner class ImageHolder(private val binding: ImageItemBinding) :
        RecyclerView.ViewHolder(binding.root) {
        private lateinit var imgFile: ImgFile

        private fun selectItem() {
            if (selectedItems.contains(imgFile)) {
                selectedItems.remove(imgFile)
                binding.root.setBackgroundColor(Color.WHITE)
            } else {
                selectedItems.add(imgFile)
                binding.root.setBackgroundColor(Color.LTGRAY)
            }
            if (selectedItems.size == 0) {
                mainActivity.setEditMode(false)
            }
        }

        fun load(file: ImgFile) {
            this.imgFile = file

            val imgView = binding.imgView
            val txtName = binding.txtName

            val fileName = file.name?.split("/")?.last() ?: "desconocido"
            txtName.text = fileName

            binding.root.setOnLongClickListener { view ->
                if (selectedItems.size == 0) {
                    mainActivity.setEditMode(true)
                    selectItem()
                }
                true
            }

            // Checks if the file is an image
            val regex = Regex("(\\S+(\\.(?i)(jpe?g|png|gif|bmp))$)")
            val isImage = regex.matches(fileName)

            binding.root.setOnClickListener {
                if (selectedItems.size > 0) {
                    selectItem()
                } else if (file.isDir == false) {
                    showFile(file)
                }
            }

            if (isImage) {
                Picasso.get().load(file.name).into(imgView)
                Log.d("myapp", "Load image: $file")
            } else if (file.isDir == true) {
                imgView.setImageResource(R.drawable.baseline_folder_24)
            } else {
                imgView.setImageResource(R.drawable.baseline_insert_drive_file_24)
            }
        }

        private fun showFile(imgFile: ImgFile) {
            val mainActivity = MainActivity.mainActivity

            runBlocking {
                launch(Dispatchers.IO) {
                    val data = URL(imgFile.name).readBytes()
                    val fileUri = saveFile(imgFile.path!!, data)
                    Log.d("myapp", "fileUri: $fileUri")
                    val intent = Intent(Intent.ACTION_VIEW).also {
                        it.setDataAndType(fileUri, imgFile.mimeType)
                        it.addFlags(FLAG_GRANT_READ_URI_PERMISSION or FLAG_GRANT_WRITE_URI_PERMISSION)
                    }
                    try {
                        mainActivity.startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(binding.root.context, "No hay aplicación para abrir el archivo", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        private fun saveFile(fileName: String, data: ByteArray): Uri {
            val filesDir = binding.root.context.getExternalFilesDir(null)
            val file = File(filesDir, fileName)
            file.writeBytes(data)

            Log.d("myapp", "File path: ${file.path}")
            val fileUri = FileProvider.getUriForFile(
                binding.root.context,
                mainActivity.packageName + ".provider",
                file
            )
            return fileUri
        }
    }

    override fun onBindViewHolder(holder: ImageAdapter.ImageHolder, position: Int) {
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
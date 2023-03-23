package ni.edu.uca.mediaupload

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.navigation.ui.AppBarConfiguration
import androidx.recyclerview.widget.GridLayoutManager
import com.google.gson.GsonBuilder
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import ni.edu.uca.mediaupload.databinding.ActivityMainBinding
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.File


class MainActivity : AppCompatActivity() {

    companion object {
        val BASE_URL = "http://192.168.1.7:8080/~jezerm/filedb/"
        const val REQUEST_CODE_PICK_IMAGE = 101
        lateinit var mainActivity: MainActivity
    }

    private lateinit var binding: ActivityMainBinding

    private var selectedImageUri: Uri? = null

    private val retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .addConverterFactory(GsonConverterFactory.create(GsonBuilder().setLenient().create()))
        .build()

    private val apiService = retrofit.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mainActivity = this

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        verifyStoragePermissions(this)

        binding.fab.setOnClickListener { view ->
            if (!doEdit) {
                openImageChooser()
            } else {
                val selectedItems = (binding.rcvImages.adapter as ImageAdapter).selectedItems
                for (item in selectedItems) {
                    runBlocking {
                        launch {
                            val itemPath = item.name!!.replace(".*filedb/".toRegex(), "")
                            try {
                                apiService.deleteImage(itemPath)
                                selectedItems.remove(item)
                            } catch (e: Exception) {
                                println("$itemPath - $item")
                                Log.e("myapp", "Delete image error", e)
                            }
                        }
                    }
                }
                runBlocking {
                    launch {
                        loadData()
                        setEditMode(false)
                    }
                }
            }
        }

        val adapter = ImageAdapter(listOf())
        binding.rcvImages.layoutManager = GridLayoutManager(this, 3)
        binding.rcvImages.adapter = adapter

        runBlocking {
            launch {
                loadData()
            }
        }
    }

    private suspend fun loadData() {
        try {
            val response = apiService.getAllImages()
            val url = BASE_URL
            val files = response.files.map { ImgFile("${url}${it.name}", it.isDir) }
            val adapter = ImageAdapter(files)
            binding.rcvImages.adapter = adapter
        } catch (e: Exception) {
            Log.e("myapp", "getAllImages", e)
        }
    }

    private fun openImageChooser() {
        Intent(Intent.ACTION_PICK).also {
            it.type = "image/*"
            val mimeTypes = arrayOf("image/jpeg", "image/png")
            it.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
            startActivityForResult(it, REQUEST_CODE_PICK_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_PICK_IMAGE -> {
                    selectedImageUri = data?.data
                    selectedImageUri?.let {
                        val file = File(getRealPathFromURI(it))
                        val requestBody =
                            RequestBody.create(MediaType.parse("application/octet-stream"), file)
                        val image =
                            MultipartBody.Part.createFormData("image", file.name, requestBody)
                        Log.e("myapp", "File size: ${file.totalSpace}")

                        runBlocking {
                            launch {
                                try {
                                    apiService.uploadImage(file.name, requestBody)
                                } catch (e: Exception) {
                                    Log.e("myapp", "Upload image error", e)
                                }
                                loadData()
                            }
                        }
                    }
                }
            }
        }
    }

    private fun enableEditMode() {
        val drawable = resources.getDrawable(R.drawable.baseline_delete_24)
        binding.fab.setImageDrawable(drawable)
    }

    private fun disableEditMode() {
        val drawable = resources.getDrawable(R.drawable.baseline_add_24)
        binding.fab.setImageDrawable(drawable)
    }

    private var doEdit = false
    fun setEditMode(v: Boolean) {
        doEdit = v
        if (v) {
            enableEditMode()
        } else {
            disableEditMode()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        val id: Int = item.itemId
        if (id == R.id.action_load) {
            runBlocking {
                launch {
                    loadData()
                }
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun getRealPathFromURI(uri: Uri): String {
        val projection = arrayOf(MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, projection, null, null, null)

        val columnIndex = cursor?.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
        cursor?.moveToFirst()
        val path = columnIndex?.let { cursor.getString(it) }
        cursor?.close()
        return path ?: ""
    }

    // Storage Permissions
    private val REQUEST_EXTERNAL_STORAGE = 1
    private val PERMISSIONS_STORAGE = arrayOf<String>(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    /**
     * Checks if the app has permission to write to device storage
     *
     * If the app does not has permission then the user will be prompted to grant permissions
     *
     * @param activity
     */
    fun verifyStoragePermissions(activity: Activity?) {
        // Check if we have write permission
        val permission = ActivityCompat.checkSelfPermission(
            activity!!,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        )
        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS_STORAGE,
                REQUEST_EXTERNAL_STORAGE
            )
        }
    }
}
package com.nasahacker.convertit.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import org.thebytearray.taglib_android.Picture
import org.thebytearray.taglib_android.TagLib
import com.nasahacker.convertit.domain.model.Metadata
import com.nasahacker.convertit.domain.repository.MetadataRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import javax.inject.Inject

class MetadataRepositoryImpl
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
    ) : MetadataRepository {
        
        companion object {
            private const val TAG = "MetadataRepositoryImpl"
        }
        override suspend fun loadMetadata(audioUri: Uri): Metadata {
            return withContext(Dispatchers.IO) {
                try {
                    var parcelFileDescriptor = try {
                        context.contentResolver.openFileDescriptor(audioUri, "rw")
                    } catch (e: SecurityException) {
                        Log.w(TAG, "Failed to open file in rw mode, trying read-only: ${e.message}")
                        null
                    } catch (e: Exception) {
                        Log.w(TAG, "Failed to open file in rw mode, trying read-only: ${e.message}")
                        null
                    }
                    
                    if (parcelFileDescriptor == null) {
                        parcelFileDescriptor = try {
                            context.contentResolver.openFileDescriptor(audioUri, "r")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to open file in read-only mode: ${e.message}")
                            return@withContext Metadata()
                        }
                    }
                    
                    if (parcelFileDescriptor == null) {
                        Log.e(TAG, "Could not open file descriptor for URI: $audioUri")
                        return@withContext Metadata()
                    }

                    parcelFileDescriptor.use { fd ->
                        val taglibMetadata =
                            TagLib.getMetadata(fd.dup().detachFd(), readPictures = true)
                                ?: run {
                                    Log.w(TAG, "TagLib returned null metadata for URI: $audioUri")
                                    return@withContext Metadata()
                                }

                        Metadata.fromPropertyMap(
                            taglibMetadata.propertyMap,
                            taglibMetadata.pictures.toList(),
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error loading metadata: ${e.message}", e)
                    e.printStackTrace()
                    Metadata()
                }
            }
        }

        override suspend fun saveMetadata(
            audioUri: Uri,
            metadata: Metadata,
        ): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    val parcelFileDescriptor =
                        context.contentResolver.openFileDescriptor(audioUri, "rw")
                            ?: run {
                                Log.e(TAG, "Could not open file descriptor for writing: $audioUri")
                                return@withContext false
                            }

                    parcelFileDescriptor.use { fd ->
                        val propertyMap = HashMap(metadata.toPropertyMap())
                        TagLib.savePropertyMap(fd.dup().detachFd(), propertyMap)
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception saving metadata (permission issue): ${e.message}", e)
                    e.printStackTrace()
                    false
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving metadata: ${e.message}", e)
                    e.printStackTrace()
                    false
                }
            }
        }

        override suspend fun saveCoverArt(
            audioUri: Uri,
            bitmap: Bitmap?,
        ): Boolean {
            return withContext(Dispatchers.IO) {
                try {
                    val parcelFileDescriptor =
                        context.contentResolver.openFileDescriptor(audioUri, "rw")
                            ?: run {
                                Log.e(TAG, "Could not open file descriptor for writing cover art: $audioUri")
                                return@withContext false
                            }

                    parcelFileDescriptor.use { fd ->
                        if (bitmap != null) {
                            val byteArrayOutputStream = ByteArrayOutputStream()
                            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream)
                            val imageData = byteArrayOutputStream.toByteArray()

                            val picture =
                                Picture(
                                    data = imageData,
                                    description = "Front Cover",
                                    pictureType = "Front Cover",
                                    mimeType = "image/jpeg",
                                )

                            TagLib.savePictures(fd.dup().detachFd(), arrayOf(picture))
                        } else {
                            TagLib.savePictures(fd.dup().detachFd(), arrayOf())
                        }
                    }
                } catch (e: SecurityException) {
                    Log.e(TAG, "Security exception saving cover art (permission issue): ${e.message}", e)
                    e.printStackTrace()
                    false
                } catch (e: Exception) {
                    Log.e(TAG, "Error saving cover art: ${e.message}", e)
                    e.printStackTrace()
                    false
                }
            }
        }
    }

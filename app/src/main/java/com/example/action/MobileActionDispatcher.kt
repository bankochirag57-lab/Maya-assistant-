package com.example.action

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.provider.Settings
import java.net.URLEncoder

sealed class ActionResult {
    data class Success(val message: String) : ActionResult()
    data class Failure(val reason: String) : ActionResult()
    data class RequiresConfirmation(
        val target: String,
        val payload: String,
        val prompt: String
    ) : ActionResult()
}

class MobileActionDispatcher(private val context: Context) {

    /**
     * Opens WhatsApp application
     */
    fun openWhatsApp(): ActionResult {
        return try {
            val pm = context.packageManager
            val launchIntent = pm.getLaunchIntentForPackage("com.whatsapp")
                ?: pm.getLaunchIntentForPackage("com.whatsapp.w4b")

            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                ActionResult.Success("WhatsApp is now open.")
            } else {
                // Fallback to web WhatsApp or store
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://api.whatsapp.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                ActionResult.Success("Opening WhatsApp via web...")
            }
        } catch (e: Exception) {
            ActionResult.Failure("Unable to open WhatsApp: ${e.message}")
        }
    }

    /**
     * Dispatches confirmed WhatsApp message
     */
    fun sendConfirmedWhatsAppMessage(messageText: String, phoneNumber: String? = null): ActionResult {
        return try {
            val encoded = URLEncoder.encode(messageText, "UTF-8")
            val uri = if (!phoneNumber.isNullOrBlank()) {
                val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
                Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=$encoded")
            } else {
                Uri.parse("https://api.whatsapp.com/send?text=$encoded")
            }

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                ActionResult.Success("WhatsApp opened with prepared message.")
            } else {
                // Try general share intent or browser
                val fallbackIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                ActionResult.Success("Sending WhatsApp message...")
            }
        } catch (e: Exception) {
            ActionResult.Failure("Failed to send WhatsApp message: ${e.message}")
        }
    }

    /**
     * Opens YouTube app or homepage
     */
    fun openYouTube(): ActionResult {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.google.android.youtube")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                ActionResult.Success("YouTube is now open.")
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                ActionResult.Success("Opening YouTube...")
            }
        } catch (e: Exception) {
            ActionResult.Failure("Unable to open YouTube: ${e.message}")
        }
    }

    /**
     * Searches YouTube for videos or songs
     */
    fun searchYouTube(query: String): ActionResult {
        return try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                setPackage("com.google.android.youtube")
            }
            val pm = context.packageManager
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent)
                ActionResult.Success("Searching YouTube for '$query'...")
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.youtube.com/results?search_query=$encoded")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(browserIntent)
                ActionResult.Success("Playing '$query' on YouTube...")
            }
        } catch (e: Exception) {
            ActionResult.Failure("Could not search YouTube: ${e.message}")
        }
    }

    /**
     * Opens Instagram app
     */
    fun openInstagram(): ActionResult {
        return try {
            val pm = context.packageManager
            val intent = pm.getLaunchIntentForPackage("com.instagram.android")
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
                ActionResult.Success("Instagram is now open.")
            } else {
                val webIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(webIntent)
                ActionResult.Success("Opening Instagram in browser...")
            }
        } catch (e: Exception) {
            ActionResult.Failure("Unable to open Instagram: ${e.message}")
        }
    }

    /**
     * Opens Camera app
     */
    fun openCamera(): ActionResult {
        return try {
            val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                ActionResult.Success("Camera activated.")
            } else {
                val fallback = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallback)
                ActionResult.Success("Camera opened.")
            }
        } catch (e: Exception) {
            ActionResult.Failure("Unable to launch Camera: ${e.message}")
        }
    }

    /**
     * Opens Phone dialer
     */
    fun openPhoneDialer(number: String? = null): ActionResult {
        return try {
            val uri = if (!number.isNullOrBlank()) {
                Uri.parse("tel:${Uri.encode(number)}")
            } else {
                Uri.parse("tel:")
            }
            val intent = Intent(Intent.ACTION_DIAL, uri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("Phone dialer opened.")
        } catch (e: Exception) {
            ActionResult.Failure("Unable to open dialer: ${e.message}")
        }
    }

    /**
     * Opens Device Settings
     */
    fun openSettings(): ActionResult {
        return try {
            val intent = Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            ActionResult.Success("System Settings opened.")
        } catch (e: Exception) {
            ActionResult.Failure("Unable to open Settings: ${e.message}")
        }
    }

    /**
     * Opens generic installed app by name
     */
    fun openAppByName(appName: String): ActionResult {
        val pm = context.packageManager
        val cleanName = appName.trim().lowercase()

        val installedPackages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (appInfo in installedPackages) {
            val label = pm.getApplicationLabel(appInfo).toString().lowercase()
            if (label.contains(cleanName) || cleanName.contains(label)) {
                val launchIntent = pm.getLaunchIntentForPackage(appInfo.packageName)
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(launchIntent)
                    return ActionResult.Success("Opening $label...")
                }
            }
        }
        return ActionResult.Failure("Application '$appName' not found on this device.")
    }
}

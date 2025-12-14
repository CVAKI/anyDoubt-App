package com.humangodcvaki.anydoubt

import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object NetworkUtils {

    /**
     * Check if internet is available
     */
    fun isInternetAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return false
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false

            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                    capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnected
        }
    }

    /**
     * Check internet and navigate to InternetCheckActivity if not connected
     * Returns true if internet is available, false otherwise
     */
    fun checkInternetOrNavigate(context: Context): Boolean {
        return if (isInternetAvailable(context)) {
            true
        } else {
            context.startActivity(Intent(context, InternetCheckActivity::class.java))
            false
        }
    }

    /**
     * Get connection type (WiFi, Mobile, None)
     */
    fun getConnectionType(context: Context): ConnectionType {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
            val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return ConnectionType.NONE

            when {
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
                else -> ConnectionType.NONE
            }
        } else {
            @Suppress("DEPRECATION")
            val networkInfo = connectivityManager.activeNetworkInfo
            when (networkInfo?.type) {
                ConnectivityManager.TYPE_WIFI -> ConnectionType.WIFI
                ConnectivityManager.TYPE_MOBILE -> ConnectionType.MOBILE
                else -> ConnectionType.NONE
            }
        }
    }

    enum class ConnectionType {
        WIFI, MOBILE, NONE
    }
}

/**
 * Extension function for easy usage
 */
fun Context.isInternetAvailable(): Boolean {
    return NetworkUtils.isInternetAvailable(this)
}

/**
 * Extension function to check internet or navigate
 */
fun Context.requireInternet(): Boolean {
    return NetworkUtils.checkInternetOrNavigate(this)
}
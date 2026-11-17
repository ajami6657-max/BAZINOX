package com.bazinox.app

/**
 * Public client configuration for BAZARINO.
 *
 * The publishable key is designed for client-side use. Never put a
 * Supabase secret/service_role key in this Android application.
 * Database access must be protected by Row Level Security (RLS).
 */
object SupabaseConfig {
    const val PROJECT_URL = "https://tnzrocysxpihzlgxjhfo.supabase.co"
    const val PUBLISHABLE_KEY = "sb_publishable_BJev2vovy7ffQ2ZR2hufxg_unRDfvrb"
    const val REST_URL = "$" + "PROJECT_URL/rest/v1/"
}

# BAZARINO Supabase connection

Project URL:
`https://tnzrocysxpihzlgxjhfo.supabase.co`

The Android app's public client configuration is stored in
`app/src/main/java/com/bazinox/app/SupabaseConfig.kt`.

## Key safety
- The configured key is a Supabase publishable key and is intended for client apps.
- Never add a `service_role`, secret, database password, or provider payment secret to the APK or repository.
- A publishable key does not grant safe access by itself. Enable and verify Row Level Security (RLS) policies before exposing user data or allowing writes.

## Database setup still required
The SQL files in `backend/migrations/` are source migrations, not automatically applied to the hosted Supabase project. Apply and verify them from the Supabase SQL Editor in order. Before production, add and test RLS policies for every exposed table, link application profiles to `auth.users`, and configure Auth, Storage buckets, and Realtime publication.

## Current implementation status
- Project URL and publishable key configured in the Android source.
- Android INTERNET permission is present.
- Supabase Auth, live database operations, file uploads, Realtime messaging, payment processing, and production RLS policies are not yet wired or verified.
- Supabase connectivity from Iranian networks must be tested on the target device; availability is not guaranteed.

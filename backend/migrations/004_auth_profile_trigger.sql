-- BAZARINO migration 004: link Supabase Auth identities to public profiles.
-- Email-based signup requires phone_e164 to be optional.
ALTER TABLE public.users ALTER COLUMN phone_e164 DROP NOT NULL;
ALTER TABLE public.users ADD COLUMN IF NOT EXISTS email TEXT UNIQUE;

CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
BEGIN
  INSERT INTO public.users (id, phone_e164, email, display_name)
  VALUES (
    NEW.id,
    NEW.phone,
    NEW.email,
    COALESCE(
      NULLIF(NEW.raw_user_meta_data ->> 'display_name', ''),
      NULLIF(split_part(COALESCE(NEW.email, ''), '@', 1), ''),
      'کاربر بازارینو'
    )
  )
  ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        display_name = COALESCE(public.users.display_name, EXCLUDED.display_name);
  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created_bazarino ON auth.users;
CREATE TRIGGER on_auth_user_created_bazarino
AFTER INSERT ON auth.users
FOR EACH ROW EXECUTE FUNCTION public.handle_new_auth_user();

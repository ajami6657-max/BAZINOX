-- BAZARINO migration 008: admin roles, moderation reports, and safe chat creation.
-- Admin roles are granted only by trusted service-role operations, never by clients.

CREATE TABLE IF NOT EXISTS public.user_roles (
  user_id UUID PRIMARY KEY REFERENCES public.users(id) ON DELETE CASCADE,
  role TEXT NOT NULL CHECK (role IN ('admin','moderator','support')),
  granted_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  granted_by UUID REFERENCES public.users(id)
);

ALTER TABLE public.user_roles ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS user_roles_read_self ON public.user_roles;
CREATE POLICY user_roles_read_self ON public.user_roles
  FOR SELECT TO authenticated USING (user_id = (SELECT auth.uid()));
-- No client INSERT/UPDATE/DELETE policies: role assignment is server-only.

CREATE OR REPLACE FUNCTION public.bazarino_is_admin()
RETURNS BOOLEAN
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
  SELECT EXISTS (
    SELECT 1 FROM public.user_roles r
    WHERE r.user_id = (SELECT auth.uid())
      AND r.role IN ('admin','moderator')
  );
$$;
REVOKE ALL ON FUNCTION public.bazarino_is_admin() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.bazarino_is_admin() TO authenticated;

CREATE TABLE IF NOT EXISTS public.content_reports (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  reporter_id UUID NOT NULL REFERENCES public.users(id) ON DELETE CASCADE,
  target_type TEXT NOT NULL CHECK (target_type IN ('listing','user','social_post','message')),
  listing_id UUID REFERENCES public.listings(id) ON DELETE CASCADE,
  target_user_id UUID REFERENCES public.users(id) ON DELETE CASCADE,
  social_post_id UUID REFERENCES public.social_posts(id) ON DELETE CASCADE,
  message_id UUID REFERENCES public.messages(id) ON DELETE CASCADE,
  reason TEXT NOT NULL CHECK (char_length(trim(reason)) BETWEEN 3 AND 1000),
  status TEXT NOT NULL DEFAULT 'open' CHECK (status IN ('open','reviewing','resolved','rejected')),
  moderator_id UUID REFERENCES public.users(id),
  moderator_note TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  resolved_at TIMESTAMPTZ,
  CHECK (
    (target_type = 'listing' AND listing_id IS NOT NULL AND target_user_id IS NULL AND social_post_id IS NULL AND message_id IS NULL)
    OR (target_type = 'user' AND target_user_id IS NOT NULL AND listing_id IS NULL AND social_post_id IS NULL AND message_id IS NULL)
    OR (target_type = 'social_post' AND social_post_id IS NOT NULL AND listing_id IS NULL AND target_user_id IS NULL AND message_id IS NULL)
    OR (target_type = 'message' AND message_id IS NOT NULL AND listing_id IS NULL AND target_user_id IS NULL AND social_post_id IS NULL)
  )
);
CREATE INDEX IF NOT EXISTS idx_content_reports_status_created
  ON public.content_reports(status, created_at DESC);
ALTER TABLE public.content_reports ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS reports_insert_self ON public.content_reports;
CREATE POLICY reports_insert_self ON public.content_reports
  FOR INSERT TO authenticated WITH CHECK (reporter_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS reports_read_own ON public.content_reports;
CREATE POLICY reports_read_own ON public.content_reports
  FOR SELECT TO authenticated USING (reporter_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS reports_admin_read ON public.content_reports;
CREATE POLICY reports_admin_read ON public.content_reports
  FOR SELECT TO authenticated USING (public.bazarino_is_admin());
DROP POLICY IF EXISTS reports_admin_update ON public.content_reports;
CREATE POLICY reports_admin_update ON public.content_reports
  FOR UPDATE TO authenticated USING (public.bazarino_is_admin())
  WITH CHECK (public.bazarino_is_admin());

-- Admin moderation permissions; ordinary users retain their existing owner policies.
DROP POLICY IF EXISTS listings_admin_moderate ON public.listings;
CREATE POLICY listings_admin_moderate ON public.listings
  FOR UPDATE TO authenticated USING (public.bazarino_is_admin())
  WITH CHECK (public.bazarino_is_admin());

DROP POLICY IF EXISTS social_posts_admin_moderate ON public.social_posts;
CREATE POLICY social_posts_admin_moderate ON public.social_posts
  FOR UPDATE TO authenticated USING (public.bazarino_is_admin())
  WITH CHECK (public.bazarino_is_admin());

DROP POLICY IF EXISTS messages_admin_read ON public.messages;
CREATE POLICY messages_admin_read ON public.messages
  FOR SELECT TO authenticated USING (public.bazarino_is_admin());

CREATE TABLE IF NOT EXISTS public.admin_audit_logs (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_id UUID NOT NULL REFERENCES public.users(id),
  action TEXT NOT NULL,
  target_type TEXT NOT NULL,
  target_id UUID,
  details JSONB NOT NULL DEFAULT '{}'::jsonb,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
ALTER TABLE public.admin_audit_logs ENABLE ROW LEVEL SECURITY;
DROP POLICY IF EXISTS admin_audit_read_admin ON public.admin_audit_logs;
CREATE POLICY admin_audit_read_admin ON public.admin_audit_logs
  FOR SELECT TO authenticated USING (public.bazarino_is_admin());
-- Audit writes are reserved for trusted server functions.

-- A buyer can start a conversation only for a currently published listing.
-- Seller and buyer are added atomically; duplicate conversations are reused.
CREATE OR REPLACE FUNCTION public.create_listing_conversation(p_listing_id UUID)
RETURNS UUID
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_user UUID := auth.uid();
  v_seller UUID;
  v_conversation UUID;
BEGIN
  IF v_user IS NULL THEN
    RAISE EXCEPTION 'authentication_required';
  END IF;

  SELECT seller_id INTO v_seller
  FROM public.listings
  WHERE id = p_listing_id AND status = 'published';

  IF v_seller IS NULL THEN
    RAISE EXCEPTION 'listing_not_available';
  END IF;
  IF v_seller = v_user THEN
    RAISE EXCEPTION 'cannot_message_self';
  END IF;

  SELECT c.id INTO v_conversation
  FROM public.conversations c
  JOIN public.conversation_members buyer
    ON buyer.conversation_id = c.id AND buyer.user_id = v_user
  JOIN public.conversation_members seller
    ON seller.conversation_id = c.id AND seller.user_id = v_seller
  WHERE c.listing_id = p_listing_id
  LIMIT 1;

  IF v_conversation IS NOT NULL THEN
    RETURN v_conversation;
  END IF;

  INSERT INTO public.conversations(id, listing_id)
  VALUES (gen_random_uuid(), p_listing_id)
  RETURNING id INTO v_conversation;

  INSERT INTO public.conversation_members(conversation_id, user_id)
  VALUES (v_conversation, v_user), (v_conversation, v_seller);

  RETURN v_conversation;
END;
$$;
REVOKE ALL ON FUNCTION public.create_listing_conversation(UUID) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.create_listing_conversation(UUID) TO authenticated;

COMMENT ON TABLE public.content_reports IS 'User-submitted moderation reports; admin review is role-gated.';
COMMENT ON TABLE public.admin_audit_logs IS 'Append-only audit history written by trusted server functions.';

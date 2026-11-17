-- BAZARINO migration 003: conservative Row Level Security defaults.
-- Assumption: public.users.id is exactly auth.uid() for each signed-in user.
-- Do not expose this schema until these policies have been applied and tested.

ALTER TABLE public.users ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listings ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.listing_fee_payments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.orders ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.payment_events ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.commission_ledger ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversations ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.conversation_members ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.messages ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_posts ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_media ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_comments ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_likes ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_follows ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.social_stories ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.user_blocks ENABLE ROW LEVEL SECURITY;

-- Profiles: users can only read/update their own row. This avoids exposing phone_e164.
DROP POLICY IF EXISTS users_select_self ON public.users;
CREATE POLICY users_select_self ON public.users
  FOR SELECT TO authenticated USING (id = (SELECT auth.uid()));
DROP POLICY IF EXISTS users_update_self ON public.users;
CREATE POLICY users_update_self ON public.users
  FOR UPDATE TO authenticated
  USING (id = (SELECT auth.uid()))
  WITH CHECK (id = (SELECT auth.uid()));

-- Listings: published listings are readable; owners can read and manage their own.
DROP POLICY IF EXISTS listings_read_published_or_owner ON public.listings;
CREATE POLICY listings_read_published_or_owner ON public.listings
  FOR SELECT TO anon, authenticated
  USING (status = 'published' OR seller_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS listings_insert_owner ON public.listings;
CREATE POLICY listings_insert_owner ON public.listings
  FOR INSERT TO authenticated WITH CHECK (seller_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS listings_update_owner ON public.listings;
CREATE POLICY listings_update_owner ON public.listings
  FOR UPDATE TO authenticated
  USING (seller_id = (SELECT auth.uid()))
  WITH CHECK (seller_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS listings_delete_owner ON public.listings;
CREATE POLICY listings_delete_owner ON public.listings
  FOR DELETE TO authenticated USING (seller_id = (SELECT auth.uid()));

-- Financial records are deliberately not writable/readable from client roles.
-- Use trusted server functions/webhooks for payments, orders and commission ledger.

-- Conversations and messages: members only.
DROP POLICY IF EXISTS conversations_member_read ON public.conversations;
CREATE POLICY conversations_member_read ON public.conversations
  FOR SELECT TO authenticated
  USING (EXISTS (
    SELECT 1 FROM public.conversation_members cm
    WHERE cm.conversation_id = id AND cm.user_id = (SELECT auth.uid())
  ));

DROP POLICY IF EXISTS conversation_members_read_self_conversation ON public.conversation_members;
CREATE POLICY conversation_members_read_self_conversation ON public.conversation_members
  FOR SELECT TO authenticated USING (user_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS messages_member_read ON public.messages;
CREATE POLICY messages_member_read ON public.messages
  FOR SELECT TO authenticated
  USING (EXISTS (
    SELECT 1 FROM public.conversation_members cm
    WHERE cm.conversation_id = messages.conversation_id
      AND cm.user_id = (SELECT auth.uid())
  ));
DROP POLICY IF EXISTS messages_member_insert ON public.messages;
CREATE POLICY messages_member_insert ON public.messages
  FOR INSERT TO authenticated
  WITH CHECK (
    sender_id = (SELECT auth.uid())
    AND EXISTS (
      SELECT 1 FROM public.conversation_members cm
      WHERE cm.conversation_id = messages.conversation_id
        AND cm.user_id = (SELECT auth.uid())
    )
  );

-- Social posts: public posts are readable by everyone; follower-only posts by followers.
DROP POLICY IF EXISTS social_posts_read_visible ON public.social_posts;
CREATE POLICY social_posts_read_visible ON public.social_posts
  FOR SELECT TO anon, authenticated
  USING (
    (status = 'published' AND visibility = 'public')
    OR author_id = (SELECT auth.uid())
    OR (
      status = 'published' AND visibility = 'followers'
      AND EXISTS (
        SELECT 1 FROM public.user_follows f
        WHERE f.follower_id = (SELECT auth.uid())
          AND f.followed_id = social_posts.author_id
      )
    )
  );
DROP POLICY IF EXISTS social_posts_insert_author ON public.social_posts;
CREATE POLICY social_posts_insert_author ON public.social_posts
  FOR INSERT TO authenticated WITH CHECK (author_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS social_posts_update_author ON public.social_posts;
CREATE POLICY social_posts_update_author ON public.social_posts
  FOR UPDATE TO authenticated
  USING (author_id = (SELECT auth.uid()))
  WITH CHECK (author_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS social_posts_delete_author ON public.social_posts;
CREATE POLICY social_posts_delete_author ON public.social_posts
  FOR DELETE TO authenticated USING (author_id = (SELECT auth.uid()));

-- Media follows visibility of its parent post.
DROP POLICY IF EXISTS social_media_read_visible_post ON public.social_media;
CREATE POLICY social_media_read_visible_post ON public.social_media
  FOR SELECT TO anon, authenticated
  USING (EXISTS (
    SELECT 1 FROM public.social_posts p
    WHERE p.id = social_media.post_id
      AND (
        (p.status = 'published' AND p.visibility = 'public')
        OR p.author_id = (SELECT auth.uid())
        OR (p.status = 'published' AND p.visibility = 'followers'
          AND EXISTS (SELECT 1 FROM public.user_follows f
            WHERE f.follower_id = (SELECT auth.uid()) AND f.followed_id = p.author_id))
      )
  ));
DROP POLICY IF EXISTS social_media_insert_post_owner ON public.social_media;
CREATE POLICY social_media_insert_post_owner ON public.social_media
  FOR INSERT TO authenticated WITH CHECK (EXISTS (
    SELECT 1 FROM public.social_posts p
    WHERE p.id = social_media.post_id AND p.author_id = (SELECT auth.uid())
  ));

-- Comments and likes.
DROP POLICY IF EXISTS social_comments_read ON public.social_comments;
CREATE POLICY social_comments_read ON public.social_comments
  FOR SELECT TO anon, authenticated USING (EXISTS (
    SELECT 1 FROM public.social_posts p
    WHERE p.id = social_comments.post_id AND p.status = 'published'
      AND (p.visibility = 'public' OR p.author_id = (SELECT auth.uid())
        OR EXISTS (SELECT 1 FROM public.user_follows f
          WHERE f.follower_id = (SELECT auth.uid()) AND f.followed_id = p.author_id))
  ));
DROP POLICY IF EXISTS social_comments_insert_self ON public.social_comments;
CREATE POLICY social_comments_insert_self ON public.social_comments
  FOR INSERT TO authenticated WITH CHECK (author_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS social_comments_delete_self ON public.social_comments;
CREATE POLICY social_comments_delete_self ON public.social_comments
  FOR DELETE TO authenticated USING (author_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS social_likes_read ON public.social_likes;
CREATE POLICY social_likes_read ON public.social_likes
  FOR SELECT TO anon, authenticated USING (true);
DROP POLICY IF EXISTS social_likes_insert_self ON public.social_likes;
CREATE POLICY social_likes_insert_self ON public.social_likes
  FOR INSERT TO authenticated WITH CHECK (user_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS social_likes_delete_self ON public.social_likes;
CREATE POLICY social_likes_delete_self ON public.social_likes
  FOR DELETE TO authenticated USING (user_id = (SELECT auth.uid()));

-- Follow graph and stories.
DROP POLICY IF EXISTS follows_read ON public.user_follows;
CREATE POLICY follows_read ON public.user_follows
  FOR SELECT TO authenticated USING (true);
DROP POLICY IF EXISTS follows_insert_self ON public.user_follows;
CREATE POLICY follows_insert_self ON public.user_follows
  FOR INSERT TO authenticated WITH CHECK (follower_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS follows_delete_self ON public.user_follows;
CREATE POLICY follows_delete_self ON public.user_follows
  FOR DELETE TO authenticated USING (follower_id = (SELECT auth.uid()));

DROP POLICY IF EXISTS stories_read_unexpired ON public.social_stories;
CREATE POLICY stories_read_unexpired ON public.social_stories
  FOR SELECT TO authenticated
  USING (expires_at > now() AND (
    author_id = (SELECT auth.uid())
    OR EXISTS (SELECT 1 FROM public.user_follows f
      WHERE f.follower_id = (SELECT auth.uid()) AND f.followed_id = social_stories.author_id)
  ));
DROP POLICY IF EXISTS stories_insert_self ON public.social_stories;
CREATE POLICY stories_insert_self ON public.social_stories
  FOR INSERT TO authenticated WITH CHECK (author_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS stories_delete_self ON public.social_stories;
CREATE POLICY stories_delete_self ON public.social_stories
  FOR DELETE TO authenticated USING (author_id = (SELECT auth.uid()));

-- Block lists are private to the blocker.
DROP POLICY IF EXISTS blocks_read_self ON public.user_blocks;
CREATE POLICY blocks_read_self ON public.user_blocks
  FOR SELECT TO authenticated USING (blocker_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS blocks_insert_self ON public.user_blocks;
CREATE POLICY blocks_insert_self ON public.user_blocks
  FOR INSERT TO authenticated WITH CHECK (blocker_id = (SELECT auth.uid()));
DROP POLICY IF EXISTS blocks_delete_self ON public.user_blocks;
CREATE POLICY blocks_delete_self ON public.user_blocks
  FOR DELETE TO authenticated USING (blocker_id = (SELECT auth.uid()));

-- Intentionally no client policies for listing_fee_payments, orders,
-- payment_events or commission_ledger. Supabase RLS denies access by default.

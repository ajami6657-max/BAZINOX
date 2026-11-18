-- BAZARINO migration 009: public social image bucket with owner-only uploads.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('social-media', 'social-media', true, 10485760, array['image/jpeg','image/png','image/webp'])
on conflict (id) do update
set public = true,
    file_size_limit = 10485760,
    allowed_mime_types = array['image/jpeg','image/png','image/webp'];

drop policy if exists social_media_public_read on storage.objects;
create policy social_media_public_read
on storage.objects for select
to anon, authenticated
using (bucket_id = 'social-media');

drop policy if exists social_media_owner_upload on storage.objects;
create policy social_media_owner_upload
on storage.objects for insert
to authenticated
with check (
  bucket_id = 'social-media'
  and (storage.foldername(name))[1] = (select auth.uid())::text
);

drop policy if exists social_media_owner_delete on storage.objects;
create policy social_media_owner_delete
on storage.objects for delete
to authenticated
using (
  bucket_id = 'social-media'
  and (storage.foldername(name))[1] = (select auth.uid())::text
);

-- Private chat media: only conversation members may read or upload.
insert into storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
values ('chat-media', 'chat-media', false, 10485760, array['image/jpeg','image/png','image/webp'])
on conflict (id) do update set public = false, file_size_limit = 10485760,
  allowed_mime_types = array['image/jpeg','image/png','image/webp'];

drop policy if exists chat_media_member_upload on storage.objects;
create policy chat_media_member_upload on storage.objects
for insert to authenticated
with check (
  bucket_id = 'chat-media'
  and (storage.foldername(name))[1] = (select auth.uid())::text
  and exists (
    select 1 from public.conversation_members cm
    where cm.conversation_id::text = (storage.foldername(name))[2]
      and cm.user_id = (select auth.uid())
  )
);

drop policy if exists chat_media_member_read on storage.objects;
create policy chat_media_member_read on storage.objects
for select to authenticated
using (
  bucket_id = 'chat-media'
  and exists (
    select 1 from public.conversation_members cm
    where cm.conversation_id::text = (storage.foldername(name))[2]
      and cm.user_id = (select auth.uid())
  )
);

drop policy if exists chat_media_owner_delete on storage.objects;
create policy chat_media_owner_delete on storage.objects
for delete to authenticated
using (
  bucket_id = 'chat-media'
  and (storage.foldername(name))[1] = (select auth.uid())::text
);

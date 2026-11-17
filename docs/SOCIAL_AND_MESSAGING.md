# BAZARINO social and messaging modules

## Product modules
### Messaging
- Private buyer/seller conversations linked to a listing or order.
- Text, image, audio and file message types; server-side media validation and signed uploads.
- Read receipts, online/last-seen presence, push notifications and offer messages.
- Report/block controls, rate limits, spam detection and moderation.
- Enforce conversation membership and listing/order ownership on every API call.

### Social feed
- Public/follower-only posts with image and video media.
- 24-hour stories, short-video feed, likes, comments, follows and user/shop profiles.
- Posts can link to a paid listing; commerce actions always go through the listing/order/payment system.
- Reporting, moderation, privacy controls, upload limits and content safety review.

## API outline
- POST /v1/conversations — create or return conversation for listing/order.
- GET /v1/conversations — list current user's conversations.
- GET /v1/conversations/{id}/messages — paginated messages.
- POST /v1/conversations/{id}/messages — send message or offer.
- POST /v1/media/upload-intents — issue constrained signed upload URL.
- POST /v1/social/posts — create post.
- GET /v1/social/feed — cursor-paginated feed.
- POST /v1/social/posts/{id}/likes — like/unlike.
- POST /v1/social/posts/{id}/comments — add comment.
- POST /v1/users/{id}/follow — follow/unfollow.
- POST /v1/social/stories — create story; expire automatically after 24 hours.
- POST /v1/reports — report user, post or message.

## Android status
The current branch adds interactive local UI previews for chat composition, feed likes/follows/comments and navigation. They do not send messages or upload content to a server yet. Real-time messaging, push notifications, media storage, social APIs and moderation remain backend work.

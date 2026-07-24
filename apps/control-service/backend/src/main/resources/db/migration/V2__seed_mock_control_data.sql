INSERT INTO control_records (
  id, section, public_id, title, status, payload, created_by, created_at, updated_at
) VALUES
  ('10000000-0000-0000-0000-000000000001', 'applications', 'rec_app_amber_001', 'Amber game application', 'DRAFT', '{"contact":"player@example.test","seats":2,"source":"mock"}', 'system', now(), now()),
  ('10000000-0000-0000-0000-000000000002', 'services', 'rec_srv_event_001', 'Corporate DnD evening', 'PUBLISHED', '{"price":"from 350 BYN","duration":"4h","source":"mock"}', 'system', now(), now()),
  ('10000000-0000-0000-0000-000000000003', 'masters', 'rec_mst_stanislav_001', 'Stanislav', 'PUBLISHED', '{"role":"master","timezone":"Europe/Minsk","source":"mock"}', 'system', now(), now()),
  ('10000000-0000-0000-0000-000000000004', 'players', 'rec_plr_maria_001', 'Maria', 'PUBLISHED', '{"rating":120,"games":8,"source":"mock"}', 'system', now(), now()),
  ('10000000-0000-0000-0000-000000000005', 'rating', 'rec_rate_correction_001', 'Manual score correction', 'REVIEW', '{"delta":-2,"reason":"mock correction","source":"mock"}', 'system', now(), now()),
  ('10000000-0000-0000-0000-000000000006', 'gallery', 'rec_gal_hall_001', 'Hall photo set', 'DRAFT', '{"files":3,"category":"tavern","source":"mock"}', 'system', now(), now()),
  ('10000000-0000-0000-0000-000000000007', 'stories', 'rec_story_diary_001', 'Diary draft', 'DRAFT', '{"autosave":true,"source":"mock"}', 'system', now(), now()),
  ('10000000-0000-0000-0000-000000000008', 'notifications', 'rec_not_backup_001', 'Backup completed', 'PUBLISHED', '{"channel":"admin","source":"mock"}', 'system', now(), now())
ON CONFLICT (public_id) DO NOTHING;

INSERT INTO subscription_status (code) VALUES ('ACTIVE') ON CONFLICT (code) DO NOTHING;
INSERT INTO subscription_status (code) VALUES ('EXPIRED') ON CONFLICT (code) DO NOTHING;
INSERT INTO subscription_status (code) VALUES ('PENDING_RENEWAL') ON CONFLICT (code) DO NOTHING;
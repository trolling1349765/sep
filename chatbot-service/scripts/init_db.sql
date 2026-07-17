-- Chay MOT LAN voi quyen root de tao schema + user rieng cho chatbot-service.
-- PowerShell (tu repo root, container mysql-capstone dang chay):
--   Get-Content .\chatbot-service\scripts\init_db.sql -Raw | docker exec -i mysql-capstone mysql -uroot -p123456
CREATE DATABASE IF NOT EXISTS chatbot_service
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'chatbot_user'@'%' IDENTIFIED BY 'chatbot_pass_490';
GRANT ALL PRIVILEGES ON chatbot_service.* TO 'chatbot_user'@'%';
FLUSH PRIVILEGES;

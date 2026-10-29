-- Navigation catalogue upgrade for installations created before the changelog (MySQL and H2).
-- Menu IDs, role assignments and custom entries are preserved.
UPDATE metaforge_menu SET url = CONCAT('/admin', SUBSTRING(url, 8)) WHERE LOWER(url) = '/system' OR LOWER(url) LIKE '/system/%' OR LOWER(url) LIKE '/system?%' OR LOWER(url) LIKE '/system#%';
UPDATE metaforge_menu SET url = CONCAT('/account/profile', SUBSTRING(url, 20)) WHERE LOWER(url) = '/admin/user/profile' OR LOWER(url) LIKE '/admin/user/profile/%' OR LOWER(url) LIKE '/admin/user/profile?%' OR LOWER(url) LIKE '/admin/user/profile#%';
UPDATE metaforge_menu SET url = CONCAT('/account/info', SUBSTRING(url, 17)) WHERE LOWER(url) = '/admin/user/info' OR LOWER(url) LIKE '/admin/user/info/%' OR LOWER(url) LIKE '/admin/user/info?%' OR LOWER(url) LIKE '/admin/user/info#%';
UPDATE metaforge_menu SET url = CONCAT('/account/log', SUBSTRING(url, 16)) WHERE LOWER(url) = '/admin/user/log' OR LOWER(url) LIKE '/admin/user/log/%' OR LOWER(url) LIKE '/admin/user/log?%' OR LOWER(url) LIKE '/admin/user/log#%';
UPDATE metaforge_menu SET url = CONCAT('/account/editor', SUBSTRING(url, 19)) WHERE LOWER(url) = '/admin/user/editor' OR LOWER(url) LIKE '/admin/user/editor/%' OR LOWER(url) LIKE '/admin/user/editor?%' OR LOWER(url) LIKE '/admin/user/editor#%';
UPDATE metaforge_menu SET url = CONCAT('/account/assistant', SUBSTRING(url, 22)) WHERE LOWER(url) = '/admin/user/assistant' OR LOWER(url) LIKE '/admin/user/assistant/%' OR LOWER(url) LIKE '/admin/user/assistant?%' OR LOWER(url) LIKE '/admin/user/assistant#%';
UPDATE metaforge_menu SET url = CONCAT('/account/username', SUBSTRING(url, 21)) WHERE LOWER(url) = '/admin/user/username' OR LOWER(url) LIKE '/admin/user/username/%' OR LOWER(url) LIKE '/admin/user/username?%' OR LOWER(url) LIKE '/admin/user/username#%';
UPDATE metaforge_menu SET url = CONCAT('/account/password', SUBSTRING(url, 21)) WHERE LOWER(url) = '/admin/user/password' OR LOWER(url) LIKE '/admin/user/password/%' OR LOWER(url) LIKE '/admin/user/password?%' OR LOWER(url) LIKE '/admin/user/password#%';
UPDATE metaforge_menu SET url = CONCAT('/account/notify', SUBSTRING(url, 19)) WHERE LOWER(url) = '/admin/user/notify' OR LOWER(url) LIKE '/admin/user/notify/%' OR LOWER(url) LIKE '/admin/user/notify?%' OR LOWER(url) LIKE '/admin/user/notify#%';
UPDATE metaforge_menu SET url = CONCAT('/query', SUBSTRING(url, 13)) WHERE LOWER(url) = '/admin/query' OR LOWER(url) LIKE '/admin/query/%' OR LOWER(url) LIKE '/admin/query?%' OR LOWER(url) LIKE '/admin/query#%';
UPDATE metaforge_menu SET url = CONCAT('/source', SUBSTRING(url, 14)) WHERE LOWER(url) = '/admin/source' OR LOWER(url) LIKE '/admin/source/%' OR LOWER(url) LIKE '/admin/source?%' OR LOWER(url) LIKE '/admin/source#%';
UPDATE metaforge_menu SET url = CONCAT('/dataset', SUBSTRING(url, 15)) WHERE LOWER(url) = '/admin/dataset' OR LOWER(url) LIKE '/admin/dataset/%' OR LOWER(url) LIKE '/admin/dataset?%' OR LOWER(url) LIKE '/admin/dataset#%';
UPDATE metaforge_menu SET url = CONCAT('/dashboard', SUBSTRING(url, 17)) WHERE LOWER(url) = '/admin/dashboard' OR LOWER(url) LIKE '/admin/dashboard/%' OR LOWER(url) LIKE '/admin/dashboard?%' OR LOWER(url) LIKE '/admin/dashboard#%';
UPDATE metaforge_menu SET url = CONCAT('/history', SUBSTRING(url, 15)) WHERE LOWER(url) = '/admin/history' OR LOWER(url) LIKE '/admin/history/%' OR LOWER(url) LIKE '/admin/history?%' OR LOWER(url) LIKE '/admin/history#%';
UPDATE metaforge_menu SET url = CONCAT('/snippet', SUBSTRING(url, 15)) WHERE LOWER(url) = '/admin/snippet' OR LOWER(url) LIKE '/admin/snippet/%' OR LOWER(url) LIKE '/admin/snippet?%' OR LOWER(url) LIKE '/admin/snippet#%';
UPDATE metaforge_menu SET url = CONCAT('/report', SUBSTRING(url, 14)) WHERE LOWER(url) = '/admin/report' OR LOWER(url) LIKE '/admin/report/%' OR LOWER(url) LIKE '/admin/report?%' OR LOWER(url) LIKE '/admin/report#%';
UPDATE metaforge_menu SET url = CONCAT('/chat', SUBSTRING(url, 12)) WHERE LOWER(url) = '/admin/chat' OR LOWER(url) LIKE '/admin/chat/%' OR LOWER(url) LIKE '/admin/chat?%' OR LOWER(url) LIKE '/admin/chat#%';
UPDATE metaforge_menu SET url = CONCAT('/workflow', SUBSTRING(url, 16)) WHERE LOWER(url) = '/admin/workflow' OR LOWER(url) LIKE '/admin/workflow/%' OR LOWER(url) LIKE '/admin/workflow?%' OR LOWER(url) LIKE '/admin/workflow#%';

INSERT INTO metaforge_menu (name, code, description, url, sorted, type, parent, active, i18n_key, icon) SELECT '数据开发', 'MANAGEMENT', '数据开发', '/workspace', 2, 'VIEW', 0, 1, 'navigation.dataDevelopment', 'Hammer' WHERE NOT EXISTS (SELECT 1 FROM metaforge_menu WHERE code = 'MANAGEMENT');
UPDATE metaforge_menu SET name = '数据开发', url = '/workspace', sorted = 2, parent = 0, i18n_key = 'navigation.dataDevelopment', icon = 'Hammer' WHERE code = 'MANAGEMENT';

INSERT INTO metaforge_menu (name, code, description, url, sorted, type, parent, active, i18n_key, icon) SELECT '数据分析', 'DATA_ANALYSIS', '数据分析', '/analysis', 3, 'VIEW', 0, 1, 'navigation.dataAnalysis', 'Gauge' WHERE NOT EXISTS (SELECT 1 FROM metaforge_menu WHERE code = 'DATA_ANALYSIS');
UPDATE metaforge_menu SET name = '数据分析', url = '/analysis', sorted = 3, parent = 0, i18n_key = 'navigation.dataAnalysis', icon = 'Gauge' WHERE code = 'DATA_ANALYSIS';

INSERT INTO metaforge_menu (name, code, description, url, sorted, type, parent, active, i18n_key, icon) SELECT '系统管理', 'SYSTEM', '系统管理', '/admin', 5, 'VIEW', 0, 1, 'navigation.systemManagement', 'SafetyCertificate' WHERE NOT EXISTS (SELECT 1 FROM metaforge_menu WHERE code = 'SYSTEM');
UPDATE metaforge_menu SET name = '系统管理', url = '/admin', sorted = 5, parent = 0, i18n_key = 'navigation.systemManagement', icon = 'SafetyCertificate' WHERE code = 'SYSTEM';

UPDATE metaforge_menu SET parent = (SELECT id FROM (SELECT id FROM metaforge_menu WHERE code = 'MANAGEMENT' ORDER BY id LIMIT 1) catalogue_root) WHERE code IN ('QUERY','DATASOURCE','DATASET','SNIPPET','HISTORY','WORKFLOW','b6669c359a7d41e581d906446366daf3');
UPDATE metaforge_menu SET parent = (SELECT id FROM (SELECT id FROM metaforge_menu WHERE code = 'DATA_ANALYSIS' ORDER BY id LIMIT 1) catalogue_root) WHERE code IN ('DASHBOARD','REPORT');
UPDATE metaforge_menu SET parent = (SELECT id FROM (SELECT id FROM metaforge_menu WHERE code = 'SYSTEM' ORDER BY id LIMIT 1) catalogue_root) WHERE code IN ('USERS','ROLE','MENU','SCHEDULE','FUNCTION','RUNTIME_CONFIGURE');
UPDATE metaforge_menu SET sorted = 1 WHERE code = 'HOME';
UPDATE metaforge_menu SET sorted = 1 WHERE code = 'QUERY';
UPDATE metaforge_menu SET sorted = 2 WHERE code = 'DATASOURCE';
UPDATE metaforge_menu SET sorted = 3 WHERE code = 'DATASET';
UPDATE metaforge_menu SET sorted = 4 WHERE code = 'b6669c359a7d41e581d906446366daf3';
UPDATE metaforge_menu SET sorted = 4 WHERE code = 'WORKFLOW';
UPDATE metaforge_menu SET sorted = 5 WHERE code = 'SNIPPET';
UPDATE metaforge_menu SET sorted = 6 WHERE code = 'HISTORY';
UPDATE metaforge_menu SET sorted = 1 WHERE code = 'DASHBOARD';
UPDATE metaforge_menu SET sorted = 2 WHERE code = 'REPORT';
UPDATE metaforge_menu SET sorted = 4 WHERE code = 'STORE';
UPDATE metaforge_menu SET sorted = 1 WHERE code = 'USERS';
UPDATE metaforge_menu SET sorted = 2 WHERE code = 'ROLE';
UPDATE metaforge_menu SET sorted = 3 WHERE code = 'MENU';
UPDATE metaforge_menu SET sorted = 4 WHERE code = 'SCHEDULE';
UPDATE metaforge_menu SET sorted = 5 WHERE code = 'FUNCTION';
UPDATE metaforge_menu SET sorted = 6 WHERE code = 'RUNTIME_CONFIGURE';

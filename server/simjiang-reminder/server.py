#!/usr/bin/env python3
import json, sqlite3, time, threading, ssl, smtplib, urllib.parse, urllib.request, secrets, re, uuid, os
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from email.mime.text import MIMEText
from email.header import Header
from email.utils import formataddr
from pathlib import Path
from datetime import date

DEFAULT_BASE = Path('/opt/simjiang-reminder')
BASE = Path(os.getenv('SIMJ_BASE') or DEFAULT_BASE)
DB = BASE / 'data.db'
HOST = '0.0.0.0'
PORT = 8787
KEY_RE = re.compile(r'^[A-Za-z0-9_-]{24,80}$')
SCHEMA_VERSION = '3'


def clean_key(k):
    return ''.join(str(k or '').strip().split())


def new_key():
    return secrets.token_urlsafe(24)


def commit_and_checkpoint(conn):
    conn.commit()
    try:
        conn.execute("PRAGMA wal_checkpoint(TRUNCATE)")
    except Exception as e:
        print("checkpoint error", e, flush=True)


def ensure_tables(conn):
    conn.execute("PRAGMA journal_mode=WAL")
    conn.execute("PRAGMA wal_autocheckpoint=1")
    conn.execute("""create table if not exists users(
        api_key text primary key,
        nickname text not null default '',
        enabled integer not null default 1,
        created_at integer not null,
        updated_at integer not null
    )""")
    conn.execute("""create table if not exists user_settings(
        api_key text primary key,
        remind_days integer not null default 7,
        remind_hour integer not null default 9,
        remind_minute integer not null default 0,
        tg_enabled integer not null default 0,
        bot_token text not null default '',
        chat_id text not null default '',
        cloud_tg_enabled integer not null default 1,
        smtp_enabled integer not null default 0,
        smtp_host text not null default '',
        smtp_port integer not null default 465,
        smtp_user text not null default '',
        smtp_pass text not null default '',
        smtp_from text not null default '',
        smtp_to text not null default '',
        cloud_email_enabled integer not null default 1,
        updated_at integer not null,
        data_json text not null default '{}'
    )""")
    conn.execute("""create table if not exists user_records(
        api_key text not null,
        record_id text not null,
        country_code text not null default '',
        number text not null default '',
        number_digits text not null default '',
        operator text not null default '',
        expire_date text not null default '',
        updated_at integer not null,
        data_json text not null default '{}',
        primary key(api_key, record_id)
    )""")
    conn.execute("""create table if not exists sent_log(
        api_key text not null,
        record_id text not null,
        channel text not null,
        day text not null,
        primary key(api_key, record_id, channel, day)
    )""")
    conn.execute("""create table if not exists sync_backups(
        id integer primary key autoincrement,
        api_key text not null,
        payload text not null,
        reason text not null default '',
        records_count integer not null default 0,
        created_at integer not null
    )""")
    conn.execute("""create table if not exists schema_meta(
        key text primary key,
        value text not null
    )""")
    conn.execute("insert or ignore into schema_meta(key,value) values(?,?)", ('version', SCHEMA_VERSION))
    conn.commit()


def migrate_legacy(conn):
    def table_exists(name):
        return bool(conn.execute("select 1 from sqlite_master where type='table' and name=?", (name,)).fetchone())

    def has_column(table, col):
        return any(r['name'] == col for r in conn.execute("pragma table_info('%s')" % table).fetchall())

    def ensure_column(table, col, typedef):
        if table_exists(table) and not has_column(table, col):
            conn.execute("ALTER TABLE %s ADD COLUMN %s %s" % (table, col, typedef))

    # Rename legacy raw-payload users table out of the way so new schema can be created.
    if table_exists('users') and has_column('users', 'payload'):
        conn.execute('DROP TABLE IF EXISTS users_legacy_tmp')
        conn.execute('ALTER TABLE users RENAME TO users_legacy_tmp')
    if table_exists('user_meta') and table_exists('user_meta_legacy'):
        conn.execute('DROP TABLE IF EXISTS user_meta_legacy')
    if table_exists('user_meta'):
        conn.execute('ALTER TABLE user_meta RENAME TO user_meta_legacy')

    # Preserve old backup table if it lacks new columns.
    if table_exists('sync_backups') and (not has_column('sync_backups', 'reason') or not has_column('sync_backups', 'records_count')):
        conn.execute('DROP TABLE IF EXISTS sync_backups_legacy_tmp')
        conn.execute('ALTER TABLE sync_backups RENAME TO sync_backups_legacy_tmp')

    ensure_tables(conn)
    ensure_column('sync_backups', 'reason', "text not null default ''")
    ensure_column('sync_backups', 'records_count', "integer not null default 0")

    # Migrate payload-based users into structured tables.
    src_users = None
    if table_exists('users_legacy_tmp'):
        src_users = 'users_legacy_tmp'
    elif table_exists('users_legacy'):
        src_users = 'users_legacy'
    elif table_exists('users') and has_column('users', 'payload'):
        src_users = 'users'
    if src_users:
        meta_map = {}
        meta_src = 'user_meta_legacy' if table_exists('user_meta_legacy') else None
        if meta_src:
            for r in conn.execute('SELECT api_key, nickname, created_at, enabled FROM %s' % meta_src).fetchall():
                meta_map[r['api_key']] = r
        now = int(time.time())
        for r in conn.execute('SELECT api_key, payload, updated_at FROM %s' % src_users).fetchall():
            api = r['api_key']
            payload_text = r['payload'] or '{}'
            try:
                payload = json.loads(payload_text)
            except Exception:
                payload = {}
            updated_at = int(r['updated_at'] or now)
            meta = meta_map.get(api)
            nickname = (meta['nickname'] if meta else '') or ''
            enabled = int(meta['enabled']) if meta else 1
            created_at = int(meta['created_at']) if meta else updated_at
            conn.execute('INSERT OR REPLACE INTO users(api_key, nickname, enabled, created_at, updated_at) VALUES(?,?,?,?,?)', (api, nickname[:50], 1 if enabled else 0, created_at, updated_at))
            upsert_settings_from_payload(conn, api, payload.get('settings') or {})
            replace_records(conn, api, payload.get('records') or [])
            conn.execute('INSERT INTO sync_backups(api_key, payload, reason, records_count, created_at) VALUES(?,?,?,?,?)',
                         (api, payload_text, 'migrate-v3', len(payload.get('records') or []), now))
        conn.execute('DROP TABLE IF EXISTS %s' % src_users)

    # Import old backup rows if they were preserved.
    if table_exists('sync_backups_legacy_tmp'):
        rows = conn.execute('SELECT * FROM sync_backups_legacy_tmp').fetchall()
        now = int(time.time())
        for r in rows:
            api = r['api_key']
            payload_text = r['payload'] if 'payload' in r else '{}'
            created_at = int(r['backed_up_at']) if 'backed_up_at' in r else now
            records_count = 0
            if payload_text:
                try:
                    records_count = len((json.loads(payload_text) or {}).get('records') or [])
                except Exception:
                    records_count = 0
            conn.execute('INSERT INTO sync_backups(api_key, payload, reason, records_count, created_at) VALUES(?,?,?,?,?)',
                         (api, payload_text, 'legacy', records_count, created_at))
        conn.execute('DROP TABLE IF EXISTS sync_backups_legacy_tmp')

    if table_exists('user_meta_legacy'):
        conn.execute('DROP TABLE IF EXISTS user_meta_legacy')
    commit_and_checkpoint(conn)

def db():
    conn = sqlite3.connect(DB)
    conn.row_factory = sqlite3.Row
    return conn


def ensure_user(api_key, nickname=''):
    conn = db()
    now = int(time.time())
    conn.execute('INSERT OR IGNORE INTO users(api_key, nickname, enabled, created_at, updated_at) VALUES(?,?,?,?,?)', (api_key, (nickname or '')[:50], 1, now, now))
    commit_and_checkpoint(conn)
    conn.close()


def user_enabled(api_key):
    api_key = clean_key(api_key)
    if not KEY_RE.match(api_key):
        return False
    conn = db()
    row = conn.execute('SELECT enabled FROM users WHERE api_key=?', (api_key,)).fetchone()
    conn.close()
    return bool(row and int(row['enabled']) == 1)


def upsert_settings_from_payload(conn, api_key, settings):
    if settings is None:
        settings = {}
    now = int(time.time())
    remind_days = int(settings.get('remindDays') or settings.get('remind天') or 7)
    remind_hour = int(settings.get('remindHour') or 9)
    remind_minute = int(settings.get('remindMinute') or 0)
    tg_enabled = 1 if settings.get('tgEnabled') else 0
    bot_token = settings.get('botToken', '')
    chat_id = settings.get('chatId', '')
    cloud_tg_enabled = 1 if settings.get('cloudTelegramEnabled', True) else 0
    smtp_enabled = 1 if settings.get('smtpEnabled') else 0
    smtp_host = settings.get('smtpHost', '')
    smtp_port = int(settings.get('smtpPort') or 465)
    smtp_user = settings.get('smtpUser', '')
    smtp_pass = settings.get('smtpPass', '')
    smtp_from = settings.get('smtpFrom', '')
    smtp_to = settings.get('smtpTo', '')
    cloud_email_enabled = 1 if settings.get('cloudEmailEnabled', True) else 0
    conn.execute('''INSERT OR REPLACE INTO user_settings(
        api_key, remind_days, remind_hour, remind_minute, tg_enabled, bot_token, chat_id, cloud_tg_enabled, smtp_enabled, smtp_host, smtp_port, smtp_user, smtp_pass, smtp_from, smtp_to, cloud_email_enabled, updated_at, data_json
    ) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)''',
    (api_key, remind_days, remind_hour, remind_minute, tg_enabled, bot_token, chat_id, cloud_tg_enabled, smtp_enabled, smtp_host, smtp_port, smtp_user, smtp_pass, smtp_from, smtp_to, cloud_email_enabled, now, json.dumps(settings, ensure_ascii=False)))


def replace_records(conn, api_key, records):
    conn.execute('DELETE FROM user_records WHERE api_key=?', (api_key,))
    now = int(time.time())
    for r in records or []:
        rec_id = r.get('id') or str(uuid.uuid4())
        cc = r.get('countryCode', '')
        num = r.get('number', '')
        num_digits = ''.join(ch for ch in str(num) if ch.isdigit())
        operator = r.get('operator', '')
        expire = str(r.get('expireDate', '') or '')
        conn.execute('INSERT OR REPLACE INTO user_records(api_key, record_id, country_code, number, number_digits, operator, expire_date, updated_at, data_json) VALUES(?,?,?,?,?,?,?,?,?)',
                     (api_key, rec_id, cc, num, num_digits, operator, expire, now, json.dumps(r, ensure_ascii=False)))


def load_settings_dict(conn, api_key):
    row = conn.execute('SELECT * FROM user_settings WHERE api_key=?', (api_key,)).fetchone()
    if not row:
        return {}
    try:
        data = json.loads(row['data_json']) if row['data_json'] else {}
    except Exception:
        data = {}
    data.setdefault('remindDays', row['remind_days'])
    data.setdefault('remind天', row['remind_days'])
    data.setdefault('remindHour', row['remind_hour'])
    data.setdefault('remindMinute', row['remind_minute'])
    data.setdefault('tgEnabled', bool(row['tg_enabled']))
    data.setdefault('botToken', row['bot_token'])
    data.setdefault('chatId', row['chat_id'])
    data.setdefault('cloudTelegramEnabled', bool(row['cloud_tg_enabled']))
    data.setdefault('smtpEnabled', bool(row['smtp_enabled']))
    data.setdefault('smtpHost', row['smtp_host'])
    data.setdefault('smtpPort', row['smtp_port'])
    data.setdefault('smtpUser', row['smtp_user'])
    data.setdefault('smtpPass', row['smtp_pass'])
    data.setdefault('smtpFrom', row['smtp_from'])
    data.setdefault('smtpTo', row['smtp_to'])
    data.setdefault('cloudEmailEnabled', bool(row['cloud_email_enabled']))
    return data


def load_records_list(conn, api_key):
    out = []
    for row in conn.execute('SELECT * FROM user_records WHERE api_key=?', (api_key,)).fetchall():
        try:
            rec = json.loads(row['data_json']) if row['data_json'] else {}
        except Exception:
            rec = {}
        if not rec.get('id'):
            rec['id'] = row['record_id']
        out.append(rec)
    return out


def assemble_payload(conn, api_key):
    return {
        'settings': load_settings_dict(conn, api_key),
        'records': load_records_list(conn, api_key)
    }


def fetch_key_counts(conn, api_key):
    settings = load_settings_dict(conn, api_key)
    records = load_records_list(conn, api_key)
    return settings, records


def purge_old_backups(conn, api_key, keep=30):
    rows = conn.execute('SELECT id FROM sync_backups WHERE api_key=? ORDER BY created_at DESC, id DESC', (api_key,)).fetchall()
    if len(rows) <= keep:
        return
    ids = [r['id'] for r in rows[keep:]]
    conn.execute('DELETE FROM sync_backups WHERE api_key=? AND id IN (%s)' % ','.join(['?'] * len(ids)), [api_key] + ids)


def insert_backup_from_current(conn, api_key, reason, prev_record_count=None, prev_settings_signature=None):
    settings, records = fetch_key_counts(conn, api_key)
    new_record_count = len(records)
    new_settings_signature = json.dumps(settings, sort_keys=True, ensure_ascii=False)
    if reason == 'merge' and prev_record_count is not None and prev_settings_signature is not None:
        if new_record_count == prev_record_count and new_settings_signature == prev_settings_signature:
            return
    payload = {'settings': settings, 'records': records}
    conn.execute('INSERT INTO sync_backups(api_key, payload, reason, records_count, created_at) VALUES(?,?,?,?,?)',
                 (api_key, json.dumps(payload, ensure_ascii=False), reason, new_record_count, int(time.time())))
    purge_old_backups(conn, api_key)


def merge_records(conn, api_key, incoming_records):
    cur_rows = conn.execute('SELECT record_id, country_code, number, data_json FROM user_records WHERE api_key=?', (api_key,)).fetchall()
    merged = {}
    num_index = {}

    def rec_fresh(rec):
        vals = [rec.get('activatedAt'), rec.get('createdAt'), rec.get('expireDate')]
        vals = [v for v in vals if v]
        return max(vals) if vals else ''

    for row in cur_rows:
        try:
            rec = json.loads(row['data_json']) if row['data_json'] else {}
        except Exception:
            rec = {}
        if not rec.get('id'):
            rec['id'] = row['record_id']
        merged[row['record_id']] = rec
        nd = ''.join(ch for ch in str(rec.get('number', '')) if ch.isdigit())
        if nd:
            num_index[(row['country_code'].strip(), nd)] = row['record_id']
    for rec_in in incoming_records or []:
        rid = rec_in.get('id') or str(uuid.uuid4())
        rec_in['id'] = rid
        nd = ''.join(ch for ch in str(rec_in.get('number', '')) if ch.isdigit())
        cc = rec_in.get('countryCode', '')
        key_primary = rid
        key_num = (cc, nd) if nd else None
        existing_key = None
        if key_primary in merged:
            existing_key = key_primary
        elif key_num and key_num in num_index:
            existing_key = num_index[key_num]
        if existing_key:
            existing = merged[existing_key]
            chosen = rec_in if rec_fresh(rec_in) >= rec_fresh(existing) else existing
            final_id = chosen.get('id') or existing_key
            chosen['id'] = final_id
            merged[final_id] = chosen
            nd_ch = ''.join(ch for ch in str(chosen.get('number', '')) if ch.isdigit())
            if nd_ch:
                num_index[(chosen.get('countryCode', ''), nd_ch)] = final_id
        else:
            merged[rid] = rec_in
            if key_num:
                num_index[key_num] = rid
    return list(merged.values())


def mask_number(n):
    ds = ''.join(ch for ch in str(n) if ch.isdigit())
    if len(ds) <= 4:
        return ds or str(n)
    return ds[:3] + '****' + ds[-4:]


def days_left(exp):
    try:
        d = date.fromisoformat(str(exp)[:10])
        return (d - date.today()).days
    except Exception:
        return None


def send_tg(token, chat_id, text):
    if not token or not chat_id:
        return False, 'Telegram 未配置'
    try:
        url = 'https://api.telegram.org/bot%s/sendMessage' % token
        data = urllib.parse.urlencode({'chat_id': chat_id, 'text': text}).encode()
        with urllib.request.urlopen(url, data=data, timeout=15) as r:
            body = r.read().decode('utf-8', 'ignore')
        return True, body[:160]
    except urllib.error.HTTPError as e:
        err_body = ''
        try:
            err_body = e.read().decode('utf-8', 'ignore')[:200]
        except Exception:
            pass
        return False, 'Telegram HTTP %d: %s' % (e.code, err_body or e.reason)
    except Exception as e:
        return False, 'Telegram 发送失败: %s: %s' % (type(e).__name__, str(e))


def send_mail(cfg, subject, body):
    host = cfg.get('smtpHost', '')
    port = int(cfg.get('smtpPort') or 465)
    user = cfg.get('smtpUser', '')
    pwd = cfg.get('smtpPass', '')
    to = cfg.get('smtpTo', '')
    sender = cfg.get('smtpFrom') or user
    if not (host and user and pwd and to):
        return False, 'SMTP 未配置完整'
    msg = MIMEText(body, 'plain', 'utf-8')
    msg['Subject'] = Header(subject, 'utf-8')
    msg['From'] = formataddr(('simJ', sender))
    msg['To'] = to
    try:
        ctx = ssl.create_default_context()
        with smtplib.SMTP_SSL(host, port, context=ctx, timeout=25) as s:
            s.login(user, pwd)
            s.sendmail(sender, [to], msg.as_string())
        return True, 'OK'
    except Exception as e:
        return False, '邮件发送失败: %s: %s' % (type(e).__name__, str(e))


def reminder_text(r, left):
    op = r.get('operator') or r.get('countryName') or 'SIM'
    num = mask_number(r.get('number', ''))
    exp = r.get('expireDate', '')
    return "⏰ simJ 到期提醒\n%s %s %s %s\n到期日期：%s\n剩余天数：%s 天" % (r.get('flag', ''), op, r.get('countryCode', ''), num, exp, left)


def check_once(only_key=None):
    conn = db()
    today = date.today().isoformat()
    if only_key:
        rows = conn.execute('SELECT api_key FROM users WHERE api_key=? AND enabled=1', (clean_key(only_key),)).fetchall()
    else:
        rows = conn.execute('SELECT api_key FROM users WHERE enabled=1').fetchall()
    stats = {'users': len(rows), 'tg': 0, 'mail': 0, 'due': 0}
    for row in rows:
        api = row['api_key']
        settings = load_settings_dict(conn, api)
        records = load_records_list(conn, api)
        remind = int(settings.get('remindDays') or settings.get('remind天') or 7)
        cloud_tg = bool(settings.get('cloudTelegramEnabled', True))
        cloud_mail = bool(settings.get('cloudEmailEnabled', True))
        for r in records:
            rid = str(r.get('id') or r.get('number') or '')
            left = days_left(str(r.get('expireDate', '')))
            if left is None or left < 0 or left > remind:
                continue
            stats['due'] += 1
            text = reminder_text(r, left)
            subject = 'simJ 到期提醒：' + mask_number(r.get('number', ''))
            if cloud_tg and settings.get('tgEnabled') and settings.get('botToken') and settings.get('chatId'):
                if not conn.execute('select 1 from sent_log where api_key=? and record_id=? and channel=? and day=?', (api, rid, 'tg', today)).fetchone():
                    try:
                        send_tg(settings.get('botToken'), settings.get('chatId'), text)
                        conn.execute('insert or ignore into sent_log values(?,?,?,?)', (api, rid, 'tg', today))
                        commit_and_checkpoint(conn)
                        stats['tg'] += 1
                    except Exception as e:
                        print('tg error', api, e, flush=True)
            if cloud_mail and settings.get('smtpEnabled') and settings.get('smtpTo'):
                if not conn.execute('select 1 from sent_log where api_key=? and record_id=? and channel=? and day=?', (api, rid, 'mail', today)).fetchone():
                    try:
                        send_mail(settings, subject, text)
                        conn.execute('insert or ignore into sent_log values(?,?,?,?)', (api, rid, 'mail', today))
                        commit_and_checkpoint(conn)
                        stats['mail'] += 1
                    except Exception as e:
                        print('mail error', api, e, flush=True)
    conn.close()
    return stats


def loop():
    while True:
        try:
            print('check', check_once(), flush=True)
        except Exception as e:
            print('check error', e, flush=True)
        time.sleep(1800)


class H(BaseHTTPRequestHandler):
    def _json(self, code, obj):
        data = json.dumps(obj, ensure_ascii=False).encode()
        self.send_response(code)
        self.send_header('Content-Type', 'application/json; charset=utf-8')
        self.send_header('Content-Length', str(len(data)))
        self.end_headers()
        self.wfile.write(data)

    def _read_json(self):
        n = int(self.headers.get('Content-Length', '0') or 0)
        body = self.rfile.read(n).decode('utf-8', 'ignore')
        try:
            return json.loads(body or '{}')
        except Exception:
            return None

    def _auth_key(self):
        k = clean_key(self.headers.get('X-API-Key', ''))
        return k if user_enabled(k) else ''

    def do_GET(self):
        if self.path.startswith('/api/status'):
            conn = db()
            users = conn.execute('select count(*) c from users where enabled=1').fetchone()['c']
            records = conn.execute('select count(*) c from user_records').fetchone()['c']
            settings = conn.execute('select count(*) c from user_settings').fetchone()['c']
            backups = conn.execute('select count(*) c from sync_backups').fetchone()['c']
            schema_ver = None
            try:
                schema_ver = conn.execute("select value from schema_meta where key='version'").fetchone()['value']
            except Exception:
                schema_ver = None
            conn.close()
            return self._json(200, {'ok': True, 'service': 'simjiang-reminder', 'version': 'v3-structured', 'users': users, 'records': records, 'settings': settings, 'backups': backups, 'dbPath': str(DB), 'schemaVersion': schema_ver, 'time': int(time.time()), 'features': {'structured': True, 'wal_checkpoint': True, 'merge': True, 'backup': True, 'backup_restore': True}})
        if self.path.startswith('/api/sync') or self.path.startswith('/api/pull'):
            api_key = self._auth_key()
            if not api_key:
                return self._json(401, {'ok': False, 'error': 'bad api key'})
            conn = db()
            row = conn.execute('SELECT updated_at FROM users WHERE api_key=?', (api_key,)).fetchone()
            if not row:
                conn.close()
                return self._json(401, {'ok': False, 'error': 'bad api key'})
            count_records = conn.execute('SELECT count(*) c FROM user_records WHERE api_key=?', (api_key,)).fetchone()['c']
            settings_row = conn.execute('SELECT 1 FROM user_settings WHERE api_key=?', (api_key,)).fetchone()
            if count_records == 0 and not settings_row:
                conn.close()
                return self._json(404, {'ok': False, 'error': 'no cloud data', 'message': '当前 Key 暂无云端数据'})
            payload = assemble_payload(conn, api_key)
            resp = {'ok': True, 'payload': payload, 'records': len(payload.get('records') or []), 'updatedAt': row['updated_at'], 'apiKeyTail': api_key[-6:]}
            conn.close()
            return self._json(200, resp)
        if self.path.startswith('/api/meta'):
            api_key = self._auth_key()
            if not api_key:
                return self._json(401, {'ok': False, 'error': 'bad api key'})
            conn = db()
            row = conn.execute('SELECT updated_at FROM users WHERE api_key=?', (api_key,)).fetchone()
            if not row:
                conn.close()
                return self._json(401, {'ok': False, 'error': 'bad api key'})
            count_records = conn.execute('SELECT count(*) c FROM user_records WHERE api_key=?', (api_key,)).fetchone()['c']
            has_settings = bool(conn.execute('SELECT 1 FROM user_settings WHERE api_key=?', (api_key,)).fetchone())
            conn.close()
            return self._json(200, {'ok': True, 'records': count_records, 'hasSettings': has_settings, 'updatedAt': row['updated_at'], 'apiKeyTail': api_key[-6:]})
        if self.path.startswith('/api/key-info'):
            api_key = self._auth_key()
            if not api_key:
                return self._json(401, {'ok': False, 'error': 'bad api key'})
            conn = db()
            row = conn.execute('SELECT created_at, updated_at FROM users WHERE api_key=?', (api_key,)).fetchone()
            if not row:
                conn.close()
                return self._json(401, {'ok': False, 'error': 'bad api key'})
            count_records = conn.execute('SELECT count(*) c FROM user_records WHERE api_key=?', (api_key,)).fetchone()['c']
            has_settings = bool(conn.execute('SELECT 1 FROM user_settings WHERE api_key=?', (api_key,)).fetchone())
            conn.close()
            return self._json(200, {'ok': True, 'apiKeyTail': api_key[-6:], 'records': count_records, 'hasSettings': has_settings, 'createdAt': row['created_at'], 'updatedAt': row['updated_at']})
        if self.path.startswith('/api/backups'):
            api_key = self._auth_key()
            if not api_key:
                return self._json(401, {'ok': False, 'error': 'bad api key'})
            path_only = urllib.parse.urlparse(self.path).path
            parts = [p for p in path_only.split('/') if p]
            if len(parts) == 3 and parts[2].isdigit():
                bid = int(parts[2])
                conn = db()
                row = conn.execute('SELECT id, reason, records_count, created_at, payload FROM sync_backups WHERE id=? AND api_key=?', (bid, api_key)).fetchone()
                conn.close()
                if not row:
                    return self._json(404, {'ok': False, 'error': 'backup not found'})
                payload_text = row['payload'] or '{}'
                summary = {'settingsKeys': [], 'recordSamples': [], 'payloadPreview': payload_text[:1000]}
                try:
                    payload_obj = json.loads(payload_text)
                    summary['settingsKeys'] = sorted(list((payload_obj.get('settings') or {}).keys()))
                    recs = payload_obj.get('records') or []
                    summary['recordSamples'] = [
                        {'id': r.get('id'), 'countryCode': r.get('countryCode'), 'number': str(r.get('number', ''))[-4:], 'operator': r.get('operator'), 'expireDate': r.get('expireDate')} for r in recs[:5]
                    ]
                except Exception:
                    pass
                return self._json(200, {'ok': True, 'backup': {'id': row['id'], 'reason': row['reason'], 'records_count': row['records_count'], 'created_at': row['created_at']}, 'summary': summary, 'apiKeyTail': api_key[-6:]})
            conn = db()
            total = conn.execute('SELECT count(*) c FROM sync_backups WHERE api_key=?', (api_key,)).fetchone()['c']
            qs = urllib.parse.urlparse(self.path).query
            params = urllib.parse.parse_qs(qs)
            raw_limit = (params.get('limit', ['200'])[0] or '200').strip()
            limit = 200
            try:
                limit = max(1, min(1000, int(raw_limit)))
            except Exception:
                limit = 200
            rows = conn.execute('SELECT id, reason, records_count, created_at FROM sync_backups WHERE api_key=? ORDER BY created_at DESC, id DESC LIMIT ?', (api_key, limit)).fetchall()
            conn.close()
            return self._json(200, {'ok': True, 'apiKeyTail': api_key[-6:], 'total': total, 'returned': len(rows), 'limit': limit, 'backups': [dict(r) for r in rows]})
        return self._json(404, {'ok': False, 'error': 'not found'})

    def do_POST(self):
        if self.path.startswith('/api/register'):
            payload = self._read_json() or {}
            k = new_key()
            nickname = str(payload.get('nickname') or '').strip()[:50]
            ensure_user(k, nickname)
            return self._json(200, {'ok': True, 'apiKey': k, 'message': '已生成独立 API Key'})
        api_key = self._auth_key()
        if not api_key:
            return self._json(401, {'ok': False, 'error': 'bad api key'})
        payload = self._read_json()
        if payload is None:
            return self._json(400, {'ok': False, 'error': 'bad json'})
        if self.path.startswith('/api/sync'):
            qs = urllib.parse.urlparse(self.path).query
            params = urllib.parse.parse_qs(qs)
            mode = (params.get('mode', ['merge'])[0] or 'merge').lower()
            if mode not in ('merge', 'replace'):
                mode = 'merge'
            settings_in = payload.get('settings') or {}
            records_in = payload.get('records') or []
            conn = db()
            now = int(time.time())
            if mode == 'replace':
                current_payload = assemble_payload(conn, api_key)
                current_records = current_payload.get('records') or []
                if current_records or (current_payload.get('settings') or {}):
                    conn.execute('INSERT INTO sync_backups(api_key, payload, reason, records_count, created_at) VALUES(?,?,?,?,?)',
                                 (api_key, json.dumps(current_payload, ensure_ascii=False), 'replace', len(current_records), now))
                    purge_old_backups(conn, api_key)
                replace_records(conn, api_key, records_in)
                upsert_settings_from_payload(conn, api_key, settings_in)
            else:
                merged_records = merge_records(conn, api_key, records_in)
                prev_settings, prev_records = fetch_key_counts(conn, api_key)
                insert_backup_from_current(conn, api_key, 'merge', prev_record_count=len(prev_records), prev_settings_signature=json.dumps(prev_settings, sort_keys=True, ensure_ascii=False))
                replace_records(conn, api_key, merged_records)
                upsert_settings_from_payload(conn, api_key, settings_in)
                records_in = merged_records
            conn.execute('UPDATE users SET updated_at=? WHERE api_key=?', (now, api_key))
            commit_and_checkpoint(conn)
            count = conn.execute('SELECT count(*) c FROM user_records WHERE api_key=?', (api_key,)).fetchone()['c']
            conn.close()
            return self._json(200, {'ok': True, 'records': count, 'message': ('覆盖' if mode == 'replace' else '合并') + '同步成功', 'apiKeyTail': api_key[-6:]})
        if self.path.startswith('/api/backups/clear'):
            keep = 50
            raw_keep = payload.get('keep') if payload else None
            if raw_keep not in (None, ''):
                try:
                    keep = max(0, min(5000, int(raw_keep)))
                except Exception:
                    keep = 50
            conn = db()
            total_before = conn.execute('SELECT count(*) c FROM sync_backups WHERE api_key=?', (api_key,)).fetchone()['c']
            keep_ids = [r['id'] for r in conn.execute('SELECT id FROM sync_backups WHERE api_key=? ORDER BY created_at DESC, id DESC LIMIT ?', (api_key, keep)).fetchall()]
            if keep_ids:
                conn.execute('DELETE FROM sync_backups WHERE api_key=? AND id NOT IN (%s)' % ','.join(['?'] * len(keep_ids)), [api_key] + keep_ids)
            else:
                conn.execute('DELETE FROM sync_backups WHERE api_key=?', (api_key,))
            total_after = conn.execute('SELECT count(*) c FROM sync_backups WHERE api_key=?', (api_key,)).fetchone()['c']
            commit_and_checkpoint(conn)
            conn.close()
            return self._json(200, {'ok': True, 'message': '已清理旧备份', 'before': total_before, 'after': total_after, 'keep': keep, 'apiKeyTail': api_key[-6:]})
        if self.path.startswith('/api/restore-backup'):
            backup_id = payload.get('backupId')
            if not backup_id:
                return self._json(400, {'ok': False, 'error': 'missing backupId', 'message': '缺少备份 ID'})
            conn = db()
            row = conn.execute('SELECT id, api_key, payload FROM sync_backups WHERE id=? AND api_key=?', (int(backup_id), api_key)).fetchone()
            if not row:
                conn.close()
                return self._json(404, {'ok': False, 'error': 'backup not found', 'message': '未找到可恢复的备份'})
            try:
                payload_data = json.loads(row['payload'])
            except Exception:
                payload_data = {}
            insert_backup_from_current(conn, api_key, 'pre-restore')
            replace_records(conn, api_key, payload_data.get('records') or [])
            upsert_settings_from_payload(conn, api_key, payload_data.get('settings') or {})
            conn.execute('UPDATE users SET updated_at=? WHERE api_key=?', (int(time.time()), api_key))
            commit_and_checkpoint(conn)
            count = conn.execute('SELECT count(*) c FROM user_records WHERE api_key=?', (api_key,)).fetchone()['c']
            conn.close()
            return self._json(200, {'ok': True, 'records': count, 'message': '已恢复指定备份', 'apiKeyTail': api_key[-6:], 'backupId': int(backup_id)})
        if self.path.startswith('/api/test-telegram'):
            s = payload.get('settings') or payload
            ok, msg = send_tg(s.get('botToken'), s.get('chatId'), '✅ simJ 云端 Telegram 测试成功\nKey: ****' + api_key[-6:])
            return self._json(200, {'ok': ok, 'message': msg})
        if self.path.startswith('/api/test-email'):
            s = payload.get('settings') or payload
            ok, msg = send_mail(s, 'simJ 云端邮件测试', '✅ simJ 云端邮件测试成功。\nKey: ****' + api_key[-6:])
            return self._json(200, {'ok': ok, 'message': msg})
        if self.path.startswith('/api/check-now'):
            stats = check_once(api_key)
            conn = db()
            settings, records = fetch_key_counts(conn, api_key)
            conn.close()
            enriched = {
                'users': 1,
                'settings': 1 if settings else 0,
                'records': len(records),
                'due': stats.get('due', 0),
                'tg': stats.get('tg', 0),
                'mail': stats.get('mail', 0),
            }
            return self._json(200, {'ok': True, 'message': '已触发当前 Key 检查', 'stats': enriched})
        return self._json(404, {'ok': False, 'error': 'not found'})

    def log_message(self, fmt, *args):
        print('%s - %s' % (self.address_string(), fmt % args), flush=True)


if __name__ == '__main__':
    BASE.mkdir(parents=True, exist_ok=True)
    _init_conn = db()
    ensure_tables(_init_conn)
    migrate_legacy(_init_conn)
    _init_conn.close()
    threading.Thread(target=loop, daemon=True).start()
    print(f'simJ reminder v3 structured listening on {HOST}:{PORT}', flush=True)
    ThreadingHTTPServer((HOST, PORT), H).serve_forever()

import json
import os
import platform
import re
import sys
import traceback
import subprocess
import urllib.request

import uyts
import yt_dlp

try:
    import mutagen  # noqa: F401
    from mutagen.flac import FLAC, Picture
    from mutagen.id3 import ID3, APIC, TIT2, TPE1
    from mutagen.mp3 import MP3
    from mutagen.mp4 import MP4, MP4Cover
    from mutagen.oggopus import OggOpus
except Exception:
    mutagen = None


PAGE_SIZE = 10
PROGRESS_RE = re.compile(r"\[download\]\s+(\d+(?:\.\d+)?)%")
MAX_LOG_LINES = 240
EXPORTABLE_EXTENSIONS = (
    ".mp3", ".m4a", ".aac", ".wav", ".ogg", ".opus", ".flac", ".webm",
    ".mp4", ".mkv", ".mov", ".avi",
)

_REMOVABLE_TERMS = [
    "clipe oficial",
    "clipe",
    "official video",
    "official music video",
    "official audio",
    "official lyric video",
    "lyric video",
    "lyrics video",
    "audio oficial",
    "vídeo oficial",
    "video oficial",
    "music video",
    "visualizer",
    "vizualizer",
    "official visualizer",
    "performance video",
    "live session",
    "ao vivo",
    "live",
    "remastered",
    "hq",
    "hd",
    "4k",
]


def _normalize_cmp(value):
    return re.sub(r"\s{2,}", " ", (value or "").strip().lower())


def _sanitize_title_for_output(raw_title):
    """
    Mantem o mesmo comportamento do sanitizeDownloadTitle do Kotlin, mas aplicado ao TITLE da tag.
    Objetivo: salvar filename e tag sem lixo tipo [Official Video], (Official Music Video), #hashtags, etc.
    """
    if not raw_title:
        return ""

    text = str(raw_title)
    text = re.sub(r"#\S+", " ", text)
    text = re.sub(r"\[[^\]]*]", " ", text)

    def _paren_repl(match):
        inner = _normalize_cmp(match.group(1))
        if any(term in inner for term in _REMOVABLE_TERMS):
            return " "
        return match.group(0)

    text = re.sub(r"\(([^)]*)\)", _paren_repl, text)

    for term in _REMOVABLE_TERMS:
        text = re.sub(r"(?i)\b" + re.escape(term) + r"\b", " ", text)

    text = re.sub(r"\s{2,}", " ", text).strip().strip("-_ .")
    return text


def _is_url(value):
    lower = value.lower()
    return lower.startswith("http://") or lower.startswith("https://")


def _build_result_url(result_type, result_id):
    if result_type == "video":
        return f"https://www.youtube.com/watch?v={result_id}"
    if result_type == "playlist":
        return f"https://www.youtube.com/playlist?list={result_id}"
    if result_type == "channel":
        return f"https://www.youtube.com/channel/{result_id}"
    return result_id


def _aria2c_args():
    return [
        "-x",
        "4",
        "-s",
        "4",
        "-k",
        "1M",
        "--max-tries=8",
        "--retry-wait=2",
        "--timeout=30",
        "--connect-timeout=15",
        "--summary-interval=0",
    ]


def _extract_thumbnail(item):
    direct = (
        getattr(item, "thumbnail_src", "")
        or getattr(item, "thumbnail", "")
        or getattr(item, "thumbnailUrl", "")
    )
    if isinstance(direct, str) and direct:
        return direct.replace("http://", "https://", 1)

    thumbnails = getattr(item, "thumbnails", None)
    if isinstance(thumbnails, list) and thumbnails:
        first = thumbnails[0]
        if isinstance(first, str):
            return first.replace("http://", "https://", 1)
        if isinstance(first, dict):
            return first.get("url", "").replace("http://", "https://", 1)
    return ""


def search(query, page=0):
    page = int(page)
    min_results = max((page + 1) * PAGE_SIZE, PAGE_SIZE)
    result = uyts.Search(query, minResults=min_results)

    normalized = []
    for item in result.results:
        result_type = getattr(item, "resultType", "unknown")
        # Channels ficam fora da listagem por enquanto para manter o fluxo focado em downloads.
        if result_type == "channel":
            continue
        normalized.append(
            {
                "id": getattr(item, "id", ""),
                "title": getattr(item, "title", ""),
                "author": getattr(item, "author", getattr(item, "title", "")),
                "result_type": result_type,
                "extra": getattr(
                    item,
                    "duration",
                    getattr(item, "length", getattr(item, "subscriber_count", "")),
                ),
                "url": _build_result_url(result_type, getattr(item, "id", "")),
                "thumbnail_url": _extract_thumbnail(item),
            }
        )

    start = page * PAGE_SIZE
    end = start + PAGE_SIZE
    page_items = normalized[start:end]
    has_more = len(normalized) > end or len(normalized) >= min_results

    return json.dumps({"items": page_items, "has_more": has_more})


def list_playlist(url, page=0, page_size=20):
    """
    Lista itens de uma playlist de forma paginada e leve (extract_flat),
    para evitar uso excessivo de memoria.
    """
    if not _is_url(url):
        raise ValueError("list_playlist espera uma URL valida")

    page = int(page)
    page_size = int(page_size)
    if page_size <= 0:
        page_size = 20

    start = page * page_size
    end = start + page_size

    opts = {
        "quiet": True,
        "no_warnings": True,
        "extract_flat": True,
        "skip_download": True,
        "cachedir": False,
        "playliststart": start + 1,  # yt-dlp usa indices 1-based
        "playlistend": end,
        "noplaylist": False,
    }

    info = {}
    try:
        with yt_dlp.YoutubeDL(opts) as ydl:
            info = ydl.extract_info(url, download=False) or {}
    except Exception:
        info = {}

    title = info.get("title") or info.get("playlist_title") or "Playlist"
    entries = info.get("entries") or []
    items = []

    for entry in entries:
        if not isinstance(entry, dict):
            continue

        vid = entry.get("id") or ""
        entry_title = entry.get("title") or ""
        uploader = entry.get("uploader") or entry.get("channel") or entry.get("uploader_id") or ""

        webpage_url = entry.get("webpage_url") or entry.get("url") or ""
        if isinstance(webpage_url, str) and not _is_url(webpage_url):
            # Para YouTube, o extract_flat costuma trazer url como id.
            if vid and "youtube" in (info.get("extractor_key") or "").lower():
                webpage_url = f"https://www.youtube.com/watch?v={vid}"
            elif vid:
                webpage_url = _build_result_url("video", vid)

        thumb = (entry.get("thumbnail") or "").replace("http://", "https://", 1)
        if not thumb and vid:
            thumb = f"https://i.ytimg.com/vi/{vid}/hqdefault.jpg"

        items.append(
            {
                "id": vid,
                "title": entry_title,
                "author": uploader,
                "result_type": "video",
                "extra": "",
                "url": webpage_url,
                "thumbnail_url": thumb,
            }
        )

    playlist_count = info.get("playlist_count")
    has_more = False
    if isinstance(playlist_count, int) and playlist_count > 0:
        has_more = (start + len(items)) < playlist_count
    else:
        # fallback: se voltou cheio, provavelmente ainda tem mais.
        has_more = len(items) >= page_size

    return json.dumps({"title": title, "items": items, "has_more": has_more})


def list_video_qualities(url):
    if not _is_url(url):
        raise ValueError("list_video_qualities espera uma URL valida")

    info = _extract_info(url)
    formats = info.get("formats") or []
    heights = set()
    for fmt in formats:
        if not isinstance(fmt, dict):
            continue
        height = fmt.get("height")
        vcodec = fmt.get("vcodec") or ""
        if isinstance(height, int) and height > 0 and vcodec != "none":
            heights.add(height)

    items = [
        {
            "height": height,
            "label": f"{height}p" + (" (melhor disponivel)" if height == max(heights) else ""),
        }
        for height in sorted(heights, reverse=True)
    ] if heights else []
    return json.dumps({"items": items})


def preview_stream_url(url):
    if not _is_url(url):
        raise ValueError("preview_stream_url espera uma URL valida")

    try:
        with yt_dlp.YoutubeDL(
            {
                "quiet": True,
                "no_warnings": True,
                "skip_download": True,
                "cachedir": False,
                "noplaylist": True,
                # O preview precisa de uma URL unica que o ExoPlayer abra com audio.
                # Evita formatos DASH separados (video-only + audio-only).
                "format": (
                    "best[protocol*=m3u8][vcodec!=none][acodec!=none]/"
                    "best[vcodec!=none][acodec!=none]/best"
                ),
            }
        ) as ydl:
            info = ydl.extract_info(url, download=False) or {}
    except Exception:
        info = _extract_info(url)

    selected_url = info.get("url") or ""
    selected_vcodec = info.get("vcodec") or ""
    selected_acodec = info.get("acodec") or ""
    if selected_url and selected_vcodec != "none" and selected_acodec != "none":
        return json.dumps({"url": selected_url.replace("http://", "https://", 1)})

    formats = info.get("formats") or []
    candidates = []
    for fmt in formats:
        if not isinstance(fmt, dict):
            continue
        stream_url = fmt.get("url") or ""
        vcodec = fmt.get("vcodec") or ""
        acodec = fmt.get("acodec") or ""
        if not stream_url or vcodec == "none" or acodec == "none":
            continue
        protocol = (fmt.get("protocol") or "").lower()
        ext = (fmt.get("ext") or "").lower()
        height = int(fmt.get("height") or 0)
        preference = 0
        if "m3u8" in protocol or ext == "m3u8":
            preference += 1000
        if height > 0:
            preference += min(height, 1080)
        candidates.append((preference, stream_url))

    if candidates:
        stream_url = max(candidates, key=lambda item: item[0])[1]
    else:
        stream_url = info.get("url") or ""

    return json.dumps({"url": stream_url.replace("http://", "https://", 1)})


def inspect_url(url):
    if not _is_url(url):
        raise ValueError("inspect_url espera uma URL valida")

    info = {}
    try:
        with yt_dlp.YoutubeDL(
            {
                "quiet": True,
                "no_warnings": True,
                "skip_download": True,
                "cachedir": False,
                "noplaylist": True,
            }
        ) as ydl:
            info = ydl.extract_info(url, download=False) or {}
    except Exception:
        info = {}

    result_id = str(info.get("id") or url)
    title = info.get("title") or url
    author = info.get("uploader") or info.get("channel") or info.get("uploader_id") or "Link direto"
    duration = info.get("duration_string") or ""
    webpage_url = info.get("webpage_url") or info.get("original_url") or url
    thumb = (info.get("thumbnail") or "").replace("http://", "https://", 1)
    if not thumb:
        thumbs = info.get("thumbnails") or []
        for item in reversed(thumbs):
            thumb = (item.get("url") or "").replace("http://", "https://", 1)
            if thumb:
                break

    return json.dumps(
        {
            "id": result_id,
            "title": title,
            "author": author,
            "result_type": "video" if info.get("_type") != "audio" else "audio",
            "extra": duration,
            "url": webpage_url,
            "thumbnail_url": thumb,
        }
    )


def _video_format(video_min_height):
    target_height = int(video_min_height or 0)
    if target_height > 0:
        return (
            f"bestvideo*[height<={target_height}]+bestaudio/best[height<={target_height}]/"
            f"best[height<={target_height}]/bestvideo*+bestaudio/best"
        )
    return "bestvideo*+bestaudio/best"


def _build_command(url, output_dir, yt_dlp_path, ffmpeg_path, aria2c_path, audio_only, video_min_height=0):
    output_template = os.path.join(output_dir, "%(title)s.%(ext)s")
    command = []

    if yt_dlp_path and os.path.exists(yt_dlp_path):
        command.extend([yt_dlp_path])
    else:
        command.extend([sys.executable, "-m", "yt_dlp"])

    command.extend(
        [
            "--newline",
            "--progress",
            "--no-cache-dir",
            "--no-config",
            "--concurrent-fragments",
            "3",
            "--retries",
            "10",
            "--fragment-retries",
            "10",
            "--retry-sleep",
            "fragment:2",
            "--continue",
            "--no-overwrites",
            "--no-playlist",
            "--paths",
            output_dir,
            "-o",
            output_template,
        ]
    )

    if ffmpeg_path and os.path.exists(ffmpeg_path):
        command.extend(["--ffmpeg-location", os.path.dirname(ffmpeg_path)])

    if aria2c_path and os.path.exists(aria2c_path):
        command.extend(
            [
                "--downloader",
                aria2c_path,
                "--downloader-args",
                "aria2c:" + " ".join(_aria2c_args()),
            ]
        )

    if audio_only:
        command.extend([
            "-f",
            "bestaudio[ext=m4a]/bestaudio/best",
        ])
    else:
        command.extend(["-f", _video_format(video_min_height)])

    command.append(url)
    return command


def _yt_dlp_api_options(temp_dir, ffmpeg_path, aria2c_path, audio_only, callback, video_min_height=0):
    output_template = os.path.join(temp_dir, "%(title)s.%(ext)s")
    progress_state = {"last": 0.0}

    def progress_hook(data):
        status = data.get("status", "")
        if status == "downloading":
            downloaded = float(data.get("downloaded_bytes") or 0)
            total = float(
                data.get("total_bytes")
                or data.get("total_bytes_estimate")
                or 0
            )
            progress = (downloaded / total) if total > 0 else 0.0
            progress_state["last"] = max(progress_state["last"], min(progress, 0.99))
            line = data.get("_percent_str", "").strip() or "baixando"
            _notify(callback, progress_state["last"], f"Baixando... {line}", repr(data))
        elif status == "finished":
            progress_state["last"] = max(progress_state["last"], 0.99)
            _notify(callback, progress_state["last"], "Download concluído, ajustando arquivo...", repr(data))
        elif status:
            _notify(callback, progress_state["last"], status, repr(data))

    options = {
        "outtmpl": output_template,
        "paths": {"home": temp_dir},
        "noplaylist": True,
        "cachedir": False,
        "concurrent_fragment_downloads": 3,
        "retries": 10,
        "fragment_retries": 10,
        "continuedl": True,
        "overwrites": False,
        "progress_hooks": [progress_hook],
        "logger": _YtDlpLogger(callback),
        "nopart": False,
        "restrictfilenames": False,
        "noprogress": False,
    }

    if aria2c_path and os.path.exists(aria2c_path):
        options["external_downloader"] = aria2c_path
        options["external_downloader_args"] = _aria2c_args()

    if ffmpeg_path and os.path.exists(ffmpeg_path):
        options["ffmpeg_location"] = os.path.dirname(ffmpeg_path)

    if audio_only:
        options["format"] = "bestaudio[ext=m4a]/bestaudio/best"
    else:
        options["format"] = _video_format(video_min_height)

    return options


def _extract_info(url):
    try:
        with yt_dlp.YoutubeDL({"quiet": True, "no_warnings": True}) as ydl:
            return ydl.extract_info(url, download=False) or {}
    except Exception:
        return {}


def _best_thumbnail(info):
    thumb = info.get("thumbnail") or ""
    if thumb:
        return thumb.replace("http://", "https://", 1)

    thumbs = info.get("thumbnails") or []
    if isinstance(thumbs, list) and thumbs:
        last = thumbs[-1]
        if isinstance(last, dict):
            url = last.get("url") or ""
            return url.replace("http://", "https://", 1)
        if isinstance(last, str):
            return last.replace("http://", "https://", 1)
    return ""


def _download_bytes(url, timeout=8):
    if not url:
        return None
    try:
        req = urllib.request.Request(url, headers={"User-Agent": "Mozilla/5.0"})
        with urllib.request.urlopen(req, timeout=timeout) as r:
            return r.read()
    except Exception:
        return None


def _pick_primary_audio_file(files):
    audio_exts = (".m4a", ".mp3", ".aac", ".opus", ".ogg", ".wav", ".flac", ".webm")
    audio_files = [f for f in files if f.lower().endswith(audio_exts)]
    if not audio_files:
        return None
    return max(audio_files, key=lambda p: os.path.getsize(p) if os.path.exists(p) else 0)


def _write_audio_tags(file_path, title, artist, cover_bytes):
    if not mutagen or not file_path or not os.path.exists(file_path):
        return False

    ext = os.path.splitext(file_path)[1].lower()
    safe_title = (title or "").strip()
    safe_artist = (artist or "").strip()

    try:
        if ext == ".mp3":
            audio = MP3(file_path, ID3=ID3)
            try:
                audio.add_tags()
            except Exception:
                pass
            if safe_title:
                audio.tags.add(TIT2(encoding=3, text=safe_title))
            if safe_artist:
                audio.tags.add(TPE1(encoding=3, text=safe_artist))
            if cover_bytes:
                audio.tags.add(
                    APIC(
                        encoding=3,
                        mime="image/jpeg",
                        type=3,
                        desc="Cover",
                        data=cover_bytes,
                    )
                )
            audio.save()
            return True

        if ext in (".m4a", ".mp4", ".aac"):
            mp4 = MP4(file_path)
            if safe_title:
                mp4["\xa9nam"] = [safe_title]
            if safe_artist:
                mp4["\xa9ART"] = [safe_artist]
            if cover_bytes:
                mp4["covr"] = [MP4Cover(cover_bytes, imageformat=MP4Cover.FORMAT_JPEG)]
            mp4.save()
            return True

        if ext in (".opus", ".ogg"):
            ogg = OggOpus(file_path)
            if safe_title:
                ogg["title"] = [safe_title]
            if safe_artist:
                ogg["artist"] = [safe_artist]
            ogg.save()
            return True

        if ext == ".flac":
            flac = FLAC(file_path)
            if safe_title:
                flac["title"] = [safe_title]
            if safe_artist:
                flac["artist"] = [safe_artist]
            if cover_bytes:
                picture = Picture()
                picture.type = 3
                picture.mime = "image/jpeg"
                picture.desc = "Cover"
                picture.data = cover_bytes
                flac.clear_pictures()
                flac.add_picture(picture)
            flac.save()
            return True

        return False
    except Exception:
        return False


def _notify(callback, progress, status, line):
    if callback is not None:
        callback.onProgress(float(progress), str(status), str(line))


def _append_log(lines, line):
    lines.append(str(line))
    if len(lines) > MAX_LOG_LINES:
        del lines[: len(lines) - MAX_LOG_LINES]


class _YtDlpLogger:
    def __init__(self, callback):
        self.callback = callback

    def debug(self, msg):
        _notify(self.callback, 0.0, "Processando...", msg)

    def warning(self, msg):
        _notify(self.callback, 0.0, "Aviso", msg)

    def error(self, msg):
        _notify(self.callback, 0.0, "Erro", msg)


def _env_snapshot(env):
    interesting_keys = [
        "HOME",
        "TMPDIR",
        "TMP",
        "TEMP",
        "XDG_CACHE_HOME",
        "XDG_CONFIG_HOME",
        "XDG_DATA_HOME",
        "PYTHONPYCACHEPREFIX",
        "YTDLP_NO_UPDATE",
    ]
    return {key: env.get(key, "") for key in interesting_keys}


def _debug_header(url, temp_dir, yt_dlp_path, ffmpeg_path, aria2c_path, audio_only, video_min_height, command, env):
    return [
        "=== DLSaver Debug ===",
        f"python_executable={sys.executable}",
        f"python_version={sys.version}",
        f"platform={platform.platform()}",
        f"url={url}",
        f"audio_only={audio_only}",
        f"video_min_height={video_min_height}",
        f"temp_dir={temp_dir}",
        f"yt_dlp_path={yt_dlp_path or '<python module>'}",
        f"aria2c_path={aria2c_path or '<none>'}",
        f"cwd={os.getcwd()}",
        f"command={' '.join(command)}",
        f"env={json.dumps(_env_snapshot(env), ensure_ascii=False)}",
        "=== Process Output ===",
    ]


def _collect_exportable_files(temp_dir):
    files = []
    for root, _, filenames in os.walk(temp_dir):
        for filename in filenames:
            if filename.lower().endswith(EXPORTABLE_EXTENSIONS):
                files.append(os.path.join(root, filename))
    files.sort()
    return files


def _convert_audio_to_flac(input_file, ffmpeg_path, lines, callback):
    if not input_file or not os.path.exists(input_file):
        return None
    if input_file.lower().endswith(".flac"):
        return input_file
    if not ffmpeg_path or not os.path.exists(ffmpeg_path):
        _append_log(lines, "=== FLAC Conversion ===")
        _append_log(lines, "ffmpeg indisponivel; mantendo audio original")
        return None

    output_file = os.path.splitext(input_file)[0] + ".flac"
    command = [
        ffmpeg_path,
        "-y",
        "-hide_banner",
        "-i",
        input_file,
        "-vn",
        "-map",
        "0:a:0",
        "-map_metadata",
        "0",
        "-c:a",
        "flac",
        "-compression_level",
        "12",
        output_file,
    ]

    _append_log(lines, "=== FLAC Conversion ===")
    _append_log(lines, "command=" + " ".join(command))
    _notify(callback, 0.99, "Convertendo audio para FLAC...", "Iniciando conversao FLAC")

    try:
        process = subprocess.run(
            command,
            stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT,
            text=True,
        )
        if process.stdout:
            for line in process.stdout.splitlines():
                _append_log(lines, line)
        _append_log(lines, f"flac_exit_code={process.returncode}")
        if process.returncode == 0 and os.path.exists(output_file) and os.path.getsize(output_file) > 0:
            try:
                os.remove(input_file)
            except Exception as exc:
                _append_log(lines, f"original_delete_failed={repr(exc)}")
            _notify(callback, 0.99, "Audio convertido para FLAC", output_file)
            return output_file
    except Exception as exc:
        _append_log(lines, f"flac_exception={repr(exc)}")
        _append_log(lines, traceback.format_exc())

    try:
        if os.path.exists(output_file):
            os.remove(output_file)
    except Exception:
        pass
    _notify(callback, 0.99, "Mantendo audio original", "Conversao FLAC falhou")
    return None


def run_download(url, temp_dir, yt_dlp_path="", ffmpeg_path="", aria2c_path="", audio_only=False, video_min_height=0, callback=None):
    if not _is_url(url):
        raise ValueError("run_download espera uma URL valida")

    os.makedirs(temp_dir, exist_ok=True)

    env = os.environ.copy()
    env["HOME"] = temp_dir
    env["TMPDIR"] = temp_dir
    env["TMP"] = temp_dir
    env["TEMP"] = temp_dir
    env["XDG_CACHE_HOME"] = temp_dir
    env["XDG_CONFIG_HOME"] = temp_dir
    env["XDG_DATA_HOME"] = temp_dir
    env["PYTHONPYCACHEPREFIX"] = os.path.join(temp_dir, "pycache")
    env["YTDLP_NO_UPDATE"] = "1"

    video_min_height = int(video_min_height or 0)
    use_external_yt_dlp = bool(yt_dlp_path and os.path.exists(yt_dlp_path))
    command = _build_command(url, temp_dir, yt_dlp_path, ffmpeg_path, aria2c_path, audio_only, video_min_height)
    mode = "external-binary" if use_external_yt_dlp else "python-api"
    lines = _debug_header(url, temp_dir, yt_dlp_path, ffmpeg_path, aria2c_path, audio_only, video_min_height, command, env)
    lines.insert(1, f"mode={mode}")

    try:
        info = _extract_info(url)
        info_title = info.get("title") or ""
        info_uploader = info.get("uploader") or info.get("channel") or info.get("uploader_id") or ""
        info_thumb = _best_thumbnail(info)

        if use_external_yt_dlp:
            last_progress = 0.0
            process = subprocess.Popen(
                command,
                stdout=subprocess.PIPE,
                stderr=subprocess.STDOUT,
                text=True,
                env=env,
            )

            _notify(callback, 0.0, "Baixando...", f"Comando: {' '.join(command)}")

            for line in process.stdout:
                line = line.rstrip()
                _append_log(lines, line)
                status = "Baixando..."
                progress = last_progress

                match = PROGRESS_RE.search(line)
                if match:
                    progress = min(float(match.group(1)) / 100.0, 0.99)
                    last_progress = max(last_progress, progress)
                    progress = last_progress
                    status = f"Baixando... {match.group(1)}%"
                elif "Destination:" in line:
                    status = "Salvando arquivo..."
                elif "Merging formats" in line:
                    status = "Mesclando áudio e vídeo..."
                elif "Deleting original file" in line:
                    status = "Limpando arquivos temporários..."

                _notify(callback, progress, status, line)

            exit_code = process.wait()
        else:
            options = _yt_dlp_api_options(temp_dir, ffmpeg_path, aria2c_path, audio_only, callback, video_min_height)
            _append_log(lines, "=== Python API Options ===")
            _append_log(lines, json.dumps({
                "outtmpl": options["outtmpl"],
                "paths": options["paths"],
                "cachedir": options["cachedir"],
                "concurrent_fragment_downloads": options["concurrent_fragment_downloads"],
                "audio_only": audio_only,
                "external_downloader": options.get("external_downloader", ""),
                "external_downloader_args": options.get("external_downloader_args", {}),
                "format": options.get("format", ""),
                "ffmpeg_location": options.get("ffmpeg_location", ""),
                "merge_output_format": options.get("merge_output_format", ""),
            }, ensure_ascii=False))
            _notify(callback, 0.0, "Baixando...", "Executando yt-dlp pela API Python")
            with yt_dlp.YoutubeDL(options) as ydl:
                ydl.extract_info(url, download=True)
            exit_code = 0

        if (exit_code != 0 or not _collect_exportable_files(temp_dir)) and aria2c_path:
            _append_log(lines, "=== Retry Without aria2c ===")
            _notify(callback, 0.0, "Tentando novamente sem acelerador...", "Retry sem aria2c")
            if use_external_yt_dlp:
                retry_command = _build_command(url, temp_dir, yt_dlp_path, ffmpeg_path, "", audio_only, video_min_height)
                _append_log(lines, "command=" + " ".join(retry_command))
                retry = subprocess.run(
                    retry_command,
                    stdout=subprocess.PIPE,
                    stderr=subprocess.STDOUT,
                    text=True,
                    env=env,
                )
                if retry.stdout:
                    for line in retry.stdout.splitlines():
                        _append_log(lines, line)
                exit_code = retry.returncode
            else:
                options = _yt_dlp_api_options(temp_dir, ffmpeg_path, "", audio_only, callback, video_min_height)
                _append_log(lines, json.dumps({
                    "outtmpl": options["outtmpl"],
                    "paths": options["paths"],
                    "audio_only": audio_only,
                    "format": options.get("format", ""),
                    "external_downloader": options.get("external_downloader", ""),
                }, ensure_ascii=False))
                with yt_dlp.YoutubeDL(options) as ydl:
                    ydl.extract_info(url, download=True)
                exit_code = 0
    except Exception as exc:
        _append_log(lines, "=== Exception ===")
        _append_log(lines, repr(exc))
        _append_log(lines, traceback.format_exc())
        exit_code = -1

    files = _collect_exportable_files(temp_dir)
    if exit_code == 0 and not files:
        _append_log(lines, "Nenhum arquivo exportavel foi produzido.")
        exit_code = -2

    lines.append("=== Files In Temp Dir ===")
    if files:
        lines.extend(files)
    else:
        lines.append("<no files>")
    lines.append(f"exit_code={exit_code}")

    if exit_code == 0 and audio_only:
        primary_audio = _pick_primary_audio_file(files)
        cover = _download_bytes(info_thumb) if info_thumb else None
        if primary_audio:
            clean_title = _sanitize_title_for_output(info_title)
            ok = _write_audio_tags(primary_audio, clean_title, info_uploader, cover)
            _append_log(lines, "=== Tagging ===")
            _append_log(lines, f"audio_file={primary_audio}")
            _append_log(lines, f"title={clean_title}")
            _append_log(lines, f"artist={info_uploader}")
            _append_log(lines, f"thumb_url={info_thumb}")
            _append_log(lines, f"tag_write_ok={ok}")
            converted = _convert_audio_to_flac(primary_audio, ffmpeg_path, lines, callback)
            if converted:
                _write_audio_tags(converted, clean_title, info_uploader, cover)
                files = _collect_exportable_files(temp_dir)
                _append_log(lines, f"flac_file={converted}")

    final_progress = 1.0 if exit_code == 0 else 0.0
    _notify(
        callback,
        final_progress,
        "Concluido" if exit_code == 0 else "Falha no download",
        f"Processo finalizado com codigo {exit_code}",
    )

    payload = {
        "success": exit_code == 0,
        "exit_code": exit_code,
        "log": "\n".join(lines).strip(),
        "temp_dir": temp_dir,
        "files": files,
    }
    return json.dumps(payload)

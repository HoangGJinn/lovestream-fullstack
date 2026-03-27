const video = document.getElementById('video');
    const player = document.getElementById('player');
    const playPauseBtn = document.getElementById('playPauseBtn');
    const rewindBtn = document.getElementById('rewindBtn');
    const forwardBtn = document.getElementById('forwardBtn');
    const nextBtn = document.getElementById('nextBtn');
    const muteBtn = document.getElementById('muteBtn');
    const volumeSlider = document.getElementById('volumeSlider');
    const progressBarContainer = document.getElementById('progressBarContainer');
    const currentProgress = document.getElementById('currentProgress');
    const currentTimeDisplay = document.getElementById('currentTime');
    const durationDisplay = document.getElementById('duration');
    const fullscreenBtn = document.getElementById('fullscreenBtn');
    const settingsBtn = document.getElementById('settingsBtn');
    const controlsOverlay = document.getElementById('controlsOverlay');
    const settingsControl = document.getElementById('settingsControl');
    const settingsMenuRoot = document.getElementById('settingsMenuRoot');
    const settingsSpeedMenu = document.getElementById('settingsSpeedMenu');
    const settingsQualityMenu = document.getElementById('settingsQualityMenu');
    const openSpeedMenuBtn = document.getElementById('openSpeedMenuBtn');
    const openQualityMenuBtn = document.getElementById('openQualityMenuBtn');
    const backFromSpeedBtn = document.getElementById('backFromSpeedBtn');
    const backFromQualityBtn = document.getElementById('backFromQualityBtn');
    const speedSummary = document.getElementById('speedSummary');
    const speedValueLabel = document.getElementById('speedValueLabel');
    const qualitySummary = document.getElementById('qualitySummary');
    const qualityValueLabel = document.getElementById('qualityValueLabel');
    const qualityOptionsContainer = document.getElementById('qualityOptions');
    const speedOptions = document.querySelectorAll('.settings-option[data-speed]');
    const roomMeta = document.getElementById('roomMeta');
    const roomCodeLabel = document.getElementById('roomCodeLabel');
    const roomRoleLabel = document.getElementById('roomRoleLabel');
    const participantCountLabel = document.getElementById('participantCount');
    const backToOriginBtn = document.getElementById('backToOriginBtn');
    const remotePlayUnlockBtn = document.getElementById('remotePlayUnlockBtn');

    const spinner = document.getElementById('loadingSpinner');
    const runtimeConfig = window.watchMovieConfig || {};
    const fallbackVideoId = runtimeConfig.videoId || '';
    const roomStateBootstrap = runtimeConfig.roomState || null;
    const queryParams = new URLSearchParams(window.location.search);
    const videoContentId = queryParams.get('id') || fallbackVideoId;
    const roomCode = (queryParams.get('roomCode') || '').trim().toUpperCase();
    let isRoomMode = !!roomCode;
    let roomState = roomStateBootstrap;
    let isHost = false;
    let isRemoteAction = false;
    let remoteActionTimerId = null;
    let wsConnected = false;
    let stompClient = null;
    let syncTimeIntervalId = null;
    const WS_SYNC_INTERVAL_MS = 5000;
    const startSecondsParam = Number(queryParams.get('start'));
    let resumeStartSeconds = Number.isFinite(startSecondsParam) && startSecondsParam > 0 ? startSecondsParam : 0;
    let hls; // ✅ FIX: đưa ra global
    let selectedManualLevel = null;
    let isAutoQuality = true;
    const CONTROL_HIDE_DELAY_MS = 2500;
    let controlsHideTimer = null;
    let lastHistorySyncAt = 0;
    const HISTORY_SYNC_INTERVAL_MS = 12000;
    let hasAppliedResume = false;
    let roomPlaybackStatus = null;
    let hasRealtimePlaybackEvent = false;
    const PLAYBACK_ACTIONS = new Set(['PLAY', 'PAUSE', 'SEEK', 'STOP']);
    let suppressNextSeekSync = false;
    let suppressNextPlaySync = false;
    let suppressNextPauseSync = false;

    function toPositiveNumber(value, fallback = 0) {
        const numeric = Number(value);
        if (!Number.isFinite(numeric) || numeric < 0) {
            return fallback;
        }
        return numeric;
    }

    function setControlDisabled(control, disabled) {
        if (!control) {
            return;
        }
        control.disabled = disabled;
        control.classList.toggle('disabled-control', disabled);
    }

    function canControlPlayback() {
        return !isRoomMode || isHost;
    }

    function shouldEmitHostSync() {
        return isRoomMode && isHost && wsConnected && !isRemoteAction;
    }

    function updateParticipantCount(count) {
        if (!participantCountLabel) {
            return;
        }
        participantCountLabel.textContent = String(toPositiveNumber(count, 0));
    }

    function hideRemotePlayUnlock() {
        if (!remotePlayUnlockBtn) {
            return;
        }
        remotePlayUnlockBtn.classList.add('hidden');
    }

    function showRemotePlayUnlock() {
        if (!remotePlayUnlockBtn) {
            return;
        }
        remotePlayUnlockBtn.classList.remove('hidden');
    }

    function markRemoteActionWindow() {
        isRemoteAction = true;
        if (remoteActionTimerId) {
            clearTimeout(remoteActionTimerId);
        }
        remoteActionTimerId = setTimeout(() => {
            isRemoteAction = false;
            remoteActionTimerId = null;
        }, 450);
    }

    function sendRoomSync(action, currentTime = null) {
        if (!wsConnected || !stompClient || !isRoomMode) {
            return;
        }
        const payload = {
            roomCode,
            action,
            currentTime: currentTime == null ? null : toPositiveNumber(currentTime, 0)
        };
        stompClient.send('/app/room.sync', {}, JSON.stringify(payload));
    }

    function applyRemotePlayback(action, currentTime, status, options = {}) {
        const suppressLocalEmit = options.suppressLocalEmit !== false;
        markRemoteActionWindow();

        const targetTime = toPositiveNumber(currentTime, video.currentTime || 0);
        if (suppressLocalEmit) {
            if (action === 'SEEK' || action === 'STOP') {
                suppressNextSeekSync = true;
            }
            if (action === 'PLAY') {
                suppressNextPlaySync = true;
            }
            if (action === 'PAUSE' || action === 'STOP') {
                suppressNextPauseSync = true;
            }
        }
        // Keep the latest server time so loadedmetadata does not jump back to stale bootstrap time.
        resumeStartSeconds = targetTime;
        if (Math.abs((video.currentTime || 0) - targetTime) > 0.6 || action === 'SEEK' || action === 'STOP') {
            video.currentTime = targetTime;
            currentTimeDisplay.textContent = formatTime(targetTime);
        }

        if (status) {
            roomPlaybackStatus = status;
        }

        if (action === 'PLAY') {
            const playPromise = video.play();
            if (playPromise && typeof playPromise.then === 'function') {
                playPromise
                    .then(() => hideRemotePlayUnlock())
                    .catch((error) => {
                        console.warn('Remote PLAY bi chan boi autoplay policy', error);
                        showRemotePlayUnlock();
                    });
            }
        } else if (action === 'PAUSE' || action === 'STOP') {
            video.pause();
            hideRemotePlayUnlock();
        }

        syncPauseState();
    }

    function handleRoomMessage(message) {
        if (!message) {
            return;
        }

        if (message.currentParticipants !== undefined && message.currentParticipants !== null) {
            updateParticipantCount(message.currentParticipants);
        }

        const action = String(message.action || '').toUpperCase();
        if (!action) {
            return;
        }

        // Prevent host from re-applying its own echoed playback command, which can create seek loops.
        if (isHost && PLAYBACK_ACTIONS.has(action)) {
            return;
        }

        if (PLAYBACK_ACTIONS.has(action)) {
            hasRealtimePlaybackEvent = true;
            applyRemotePlayback(action, message.currentTime, message.status, { suppressLocalEmit: true });
            return;
        }

        if (action === 'LEAVE' && message.status && String(message.status).toUpperCase() !== 'PLAYING') {
            hasRealtimePlaybackEvent = true;
            applyRemotePlayback('PAUSE', message.currentTime, message.status, { suppressLocalEmit: true });
        }
    }

    function startHostSyncHeartbeat() {
        if (!isRoomMode || !isHost) {
            return;
        }
        if (syncTimeIntervalId) {
            clearInterval(syncTimeIntervalId);
        }
        syncTimeIntervalId = setInterval(() => {
            if (!video || video.readyState <= 0) {
                return;
            }
            sendRoomSync('SYNC_TIME', video.currentTime);
        }, WS_SYNC_INTERVAL_MS);
    }

    function stopHostSyncHeartbeat() {
        if (syncTimeIntervalId) {
            clearInterval(syncTimeIntervalId);
            syncTimeIntervalId = null;
        }
    }

    function connectRoomSocket() {
        if (!isRoomMode || typeof SockJS === 'undefined' || typeof Stomp === 'undefined') {
            return;
        }

        const socket = new SockJS('/ws-lovestream');
        stompClient = Stomp.over(socket);
        stompClient.debug = () => {};
        stompClient.connect({}, () => {
            wsConnected = true;
            stompClient.subscribe('/topic/room/' + roomCode, (payload) => {
                try {
                    handleRoomMessage(JSON.parse(payload.body));
                } catch (error) {
                    console.warn('Invalid room message payload', error);
                }
            });
            sendRoomSync('JOIN', video.currentTime);
            startHostSyncHeartbeat();
        }, (error) => {
            console.warn('Room websocket disconnected', error);
            wsConnected = false;
            stopHostSyncHeartbeat();
        });
    }

    function buildBackUrl() {
        if (isRoomMode) {
            return '/watch-together';
        }
        if (videoContentId) {
            return '/movies/' + encodeURIComponent(videoContentId);
        }
        return '/movies';
    }

    function setupBackButton() {
        if (!backToOriginBtn) {
            return;
        }
        const backUrl = buildBackUrl();
        const backLabel = isRoomMode ? 'Ve phong xem chung' : 'Ve chi tiet phim';
        backToOriginBtn.title = backLabel;
        backToOriginBtn.setAttribute('aria-label', backLabel);
        backToOriginBtn.addEventListener('click', () => {
            window.location.href = backUrl;
        });
    }

    function setupRemotePlayUnlock() {
        if (!remotePlayUnlockBtn) {
            return;
        }
        remotePlayUnlockBtn.addEventListener('click', async () => {
            try {
                await video.play();
                hideRemotePlayUnlock();
            } catch (error) {
                console.warn('Khong the tiep tuc phat sau khi user click', error);
            }
        });
    }

    async function bootstrapRoomMode() {
        if (!isRoomMode) {
            return true;
        }

        if (!roomState) {
            try {
                const response = await fetch('/watch-together/api/rooms/' + encodeURIComponent(roomCode) + '/state');
                if (!response.ok) {
                    throw new Error('Room state request failed');
                }
                roomState = await response.json();
            } catch (error) {
                window.location.href = '/watch-together';
                return false;
            }
        }

        isHost = !!roomState.host;
        roomPlaybackStatus = String(roomState.status || 'WAITING').toUpperCase();
        resumeStartSeconds = toPositiveNumber(roomState.currentVideoTime, resumeStartSeconds);

        if (roomMeta) {
            roomMeta.style.display = 'inline-flex';
        }
        if (roomCodeLabel) {
            roomCodeLabel.textContent = roomCode;
        }
        if (roomRoleLabel) {
            roomRoleLabel.textContent = isHost ? 'Host' : 'Viewer';
        }
        updateParticipantCount(roomState.participantCount);

        setControlDisabled(nextBtn, true);
        if (!isHost) {
            setControlDisabled(playPauseBtn, true);
            setControlDisabled(rewindBtn, true);
            setControlDisabled(forwardBtn, true);
            setControlDisabled(settingsBtn, true);
            setControlDisabled(openSpeedMenuBtn, true);
            progressBarContainer.style.pointerEvents = 'none';
        }

        return true;
    }

    // Khi đang load dữ liệu
    video.addEventListener('waiting', () => {
        spinner.classList.remove('hidden');
    });

    // Khi đã load xong và có thể play
    video.addEventListener('playing', () => {
        spinner.classList.add('hidden');
    });

    function clearControlsHideTimer() {
        if (controlsHideTimer) {
            clearTimeout(controlsHideTimer);
            controlsHideTimer = null;
        }
    }

    function hideControlsIfIdle() {
        if (settingsControl.classList.contains('open') || video.paused) {
            return;
        }
        player.classList.remove('user-active');
    }

    function scheduleControlsHide() {
        clearControlsHideTimer();
        controlsHideTimer = setTimeout(hideControlsIfIdle, CONTROL_HIDE_DELAY_MS);
    }

    function showControls() {
        player.classList.add('user-active');
        if (!video.paused && !settingsControl.classList.contains('open')) {
            scheduleControlsHide();
        }
    }

    function syncPauseState() {
        if (video.paused) {
            player.classList.add('paused-state');
            player.classList.add('user-active');
            clearControlsHideTimer();
        } else {
            player.classList.remove('paused-state');
            scheduleControlsHide();
        }
    }

    player.addEventListener('mousemove', showControls);
    player.addEventListener('mouseenter', showControls);
    player.addEventListener('mouseleave', () => {
        clearControlsHideTimer();
        if (!video.paused) {
            player.classList.remove('user-active');
        }
    });
    controlsOverlay.addEventListener('mousemove', showControls);


    // Khi tua xong
    video.addEventListener('seeked', () => {
        spinner.classList.add('hidden');
        if (suppressNextSeekSync) {
            suppressNextSeekSync = false;
            return;
        }
        if (shouldEmitHostSync()) {
            sendRoomSync('SEEK', video.currentTime);
        }
    });

    // --- Play/Pause ---
    function togglePlay() {
        if (!canControlPlayback()) {
            return;
        }
        if (video.paused) {
            video.play();
            playPauseBtn.innerHTML = '<i class="fas fa-pause"></i>';
        } else {
            video.pause();
            playPauseBtn.innerHTML = '<i class="fas fa-play"></i>';
        }
        syncPauseState();
    }

    player.addEventListener('click', (e) => {
        if ((e.target === player || e.target === video) && canControlPlayback()) {
            togglePlay();
        }
    });
    playPauseBtn.addEventListener('click', togglePlay);

    // --- Tua 10s ---
    rewindBtn.addEventListener('click', () => {
        if (!canControlPlayback()) {
            return;
        }
        video.currentTime -= 10;
    });
    forwardBtn.addEventListener('click', () => {
        if (!canControlPlayback()) {
            return;
        }
        video.currentTime += 10;
    });

    // --- Volume ---
    volumeSlider.addEventListener('input', (e) => {
        video.volume = e.target.value;
        video.muted = false;
        muteBtn.innerHTML = video.volume > 0
            ? '<i class="fas fa-volume-up"></i>'
            : '<i class="fas fa-volume-mute"></i>';
    });

    muteBtn.addEventListener('click', () => {
        video.muted = !video.muted;
        muteBtn.innerHTML = video.muted
            ? '<i class="fas fa-volume-mute"></i>'
            : '<i class="fas fa-volume-up"></i>';
    });

    // --- Fullscreen ---
    fullscreenBtn.addEventListener('click', () => {
        if (!document.fullscreenElement) {
            player.requestFullscreen();
        } else {
            document.exitFullscreen();
        }
    });

    // --- Settings menu ---
    const allSettingsPanels = [settingsMenuRoot, settingsSpeedMenu, settingsQualityMenu];

    function showSettingsPanel(panel) {
        allSettingsPanels.forEach(item => item.classList.remove('active'));
        panel.classList.add('active');
    }

    function openSettingsMenu(panel = settingsMenuRoot) {
        settingsControl.classList.add('open');
        showSettingsPanel(panel);
        showControls();
        clearControlsHideTimer();
    }

    function closeSettingsMenu() {
        settingsControl.classList.remove('open');
        showSettingsPanel(settingsMenuRoot);
        if (!video.paused) {
            scheduleControlsHide();
        }
    }

    settingsBtn.addEventListener('click', () => {
        if (!canControlPlayback()) {
            return;
        }
        if (settingsControl.classList.contains('open')) {
            closeSettingsMenu();
            return;
        }
        openSettingsMenu(settingsMenuRoot);
    });

    openSpeedMenuBtn.addEventListener('click', () => {
        if (!canControlPlayback()) {
            return;
        }
        showSettingsPanel(settingsSpeedMenu);
    });

    openQualityMenuBtn.addEventListener('click', () => {
        if (openQualityMenuBtn.classList.contains('disabled')) {
            return;
        }
        showSettingsPanel(settingsQualityMenu);
    });

    backFromSpeedBtn.addEventListener('click', () => showSettingsPanel(settingsMenuRoot));
    backFromQualityBtn.addEventListener('click', () => showSettingsPanel(settingsMenuRoot));

    document.addEventListener('click', (event) => {
        if (!settingsControl.contains(event.target)) {
            closeSettingsMenu();
        }
    });

    // --- Speed ---
    function setPlaybackSpeed(speed) {
        video.playbackRate = speed;
        const speedText = speed + 'x';
        speedSummary.textContent = speedText;
        speedValueLabel.textContent = speedText;
        speedOptions.forEach(option => {
            option.classList.toggle('active', Number(option.dataset.speed) === speed);
        });
    }

    speedOptions.forEach(option => {
        option.addEventListener('click', () => {
            if (!canControlPlayback()) {
                return;
            }
            setPlaybackSpeed(Number(option.dataset.speed));
            closeSettingsMenu();
        });
    });

    setPlaybackSpeed(1);
    syncPauseState();

    // --- Quality ---
    function getQualityLabel(level) {
        if (!level) return 'Unknown';
        if (level.height) return `${level.height}p`;
        if (level.attrs && level.attrs.RESOLUTION) {
            const resolution = String(level.attrs.RESOLUTION).split('x');
            if (resolution.length === 2) return `${resolution[1]}p`;
        }
        if (level.bitrate) return `${Math.round(level.bitrate / 1000)} kbps`;
        return 'Unknown';
    }

    function updateQualityDisplay(currentLevelIndex = null) {
        if (isAutoQuality) {
            if (currentLevelIndex !== null && hls && hls.levels[currentLevelIndex]) {
                const autoLabel = getQualityLabel(hls.levels[currentLevelIndex]);
                qualitySummary.textContent = `Auto (${autoLabel})`;
            } else {
                qualitySummary.textContent = 'Auto';
            }
            qualityValueLabel.textContent = 'Auto';
            return;
        }

        const manualLabel = selectedManualLevel !== null && hls && hls.levels[selectedManualLevel]
            ? getQualityLabel(hls.levels[selectedManualLevel])
            : 'Auto';
        qualitySummary.textContent = manualLabel;
        qualityValueLabel.textContent = manualLabel;
    }

    function refreshQualityActiveState() {
        const allQualityButtons = qualityOptionsContainer.querySelectorAll('.settings-option[data-quality]');
        allQualityButtons.forEach(button => {
            if (button.dataset.quality === 'auto') {
                button.classList.toggle('active', isAutoQuality);
            } else {
                const buttonLevel = Number(button.dataset.level);
                button.classList.toggle('active', !isAutoQuality && buttonLevel === selectedManualLevel);
            }
        });
    }

    function setQualityAvailability(isAvailable) {
        openQualityMenuBtn.classList.toggle('disabled', !isAvailable);
        openQualityMenuBtn.setAttribute('aria-disabled', String(!isAvailable));
        if (!isAvailable) {
            qualitySummary.textContent = 'N/A';
            qualityValueLabel.textContent = 'Không hỗ trợ';
        }
    }

    function applyQualitySelection(levelIndex) {
        if (!hls) return;

        if (levelIndex === -1) {
            isAutoQuality = true;
            selectedManualLevel = null;
            hls.currentLevel = -1;
            hls.nextLevel = -1;
        } else {
            isAutoQuality = false;
            selectedManualLevel = levelIndex;
            hls.currentLevel = levelIndex;
            hls.nextLevel = levelIndex;
        }

        updateQualityDisplay();
        refreshQualityActiveState();
    }

    function renderQualityOptionsFromHls() {
        if (!hls || !hls.levels || hls.levels.length === 0) {
            setQualityAvailability(false);
            return;
        }

        const dedupByHeight = new Map();
        hls.levels.forEach((level, index) => {
            const height = level.height || 0;
            const current = dedupByHeight.get(height);
            if (!current || (level.bitrate || 0) > (current.bitrate || 0)) {
                dedupByHeight.set(height, {
                    index,
                    bitrate: level.bitrate || 0,
                    label: getQualityLabel(level),
                    height
                });
            }
        });

        const qualityLevels = Array.from(dedupByHeight.values())
            .sort((a, b) => b.height - a.height);

        const html = [
            '<button class="settings-option active" data-quality="auto" data-level="-1" type="button">Auto</button>',
            ...qualityLevels.map(item => (
                `<button class="settings-option" data-quality="manual" data-level="${item.index}" type="button">${item.label}</button>`
            ))
        ].join('');

        qualityOptionsContainer.innerHTML = html;
        setQualityAvailability(true);

        qualityOptionsContainer.querySelectorAll('.settings-option[data-quality]').forEach(button => {
            button.addEventListener('click', () => {
                if (!canControlPlayback()) {
                    return;
                }
                const levelIndex = Number(button.dataset.level);
                applyQualitySelection(levelIndex);
                closeSettingsMenu();
            });
        });

        refreshQualityActiveState();
        updateQualityDisplay();
    }

    // --- Time format ---
    function formatTime(seconds) {
        let m = Math.floor(seconds / 60);
        let s = Math.floor(seconds % 60);
        return `${m < 10 ? '0'+m : m}:${s < 10 ? '0'+s : s}`;
    }

    function applyResumePosition(force = false) {
        if (hasAppliedResume || resumeStartSeconds <= 0) {
            return;
        }

        const hasDuration = Number.isFinite(video.duration) && video.duration > 0;
        if (!hasDuration && !force) {
            return;
        }

        const target = hasDuration
            ? Math.min(resumeStartSeconds, Math.max(video.duration - 1, 0))
            : resumeStartSeconds;

        if (target > 0) {
            video.currentTime = target;
            currentTimeDisplay.textContent = formatTime(target);
        }

        hasAppliedResume = true;
    }

    video.addEventListener('loadedmetadata', () => {
        durationDisplay.textContent = formatTime(video.duration);
        applyResumePosition(true);
        if (isRoomMode && roomPlaybackStatus && !hasRealtimePlaybackEvent) {
            if (roomPlaybackStatus === 'PLAYING') {
                applyRemotePlayback('PLAY', resumeStartSeconds, roomPlaybackStatus, { suppressLocalEmit: false });
            } else {
                applyRemotePlayback('PAUSE', resumeStartSeconds, roomPlaybackStatus, { suppressLocalEmit: false });
            }
        }
        syncWatchProgress();
    });

    video.addEventListener('durationchange', () => applyResumePosition());
    video.addEventListener('canplay', () => applyResumePosition(true));

    // --- Update progress (giảm lag UI) ---
    let lastUpdate = 0;
    video.addEventListener('timeupdate', () => {
        const now = Date.now();
        if (now - lastUpdate < 200) return;
        lastUpdate = now;

        const percent = (video.currentTime / video.duration) * 100;
        currentProgress.style.width = percent + '%';
        currentTimeDisplay.textContent = formatTime(video.currentTime);

        if (now - lastHistorySyncAt >= HISTORY_SYNC_INTERVAL_MS) {
            syncWatchProgress();
        }
    });

    video.addEventListener('play', () => {
        playPauseBtn.innerHTML = '<i class="fas fa-pause"></i>';
        syncPauseState();
        if (suppressNextPlaySync) {
            suppressNextPlaySync = false;
            return;
        }
        if (shouldEmitHostSync()) {
            sendRoomSync('PLAY', video.currentTime);
        }
    });

    video.addEventListener('pause', () => {
        playPauseBtn.innerHTML = '<i class="fas fa-play"></i>';
        syncPauseState();
        syncWatchProgress();
        if (suppressNextPauseSync) {
            suppressNextPauseSync = false;
            return;
        }
        if (shouldEmitHostSync()) {
            sendRoomSync('PAUSE', video.currentTime);
        }
    });

    video.addEventListener('ended', () => syncWatchProgress(true));

    // --- Seek tối ưu ---
    video.addEventListener('seeking', () => {
        if (hls) {
            hls.stopLoad();
            hls.startLoad(video.currentTime); // 🔥 load ngay vị trí mới
        }
    });

    video.preload = "auto";

    // --- Click progress ---
    let seekingLock = false;

    progressBarContainer.addEventListener('click', (e) => {
        if (!canControlPlayback()) return;
        if (seekingLock) return;
        seekingLock = true;

        const rect = progressBarContainer.getBoundingClientRect();
        const percent = (e.clientX - rect.left) / rect.width;
        const newTime = percent * video.duration;

        // ✅ Update UI ngay
        currentProgress.style.width = (percent * 100) + '%';
        currentTimeDisplay.textContent = formatTime(newTime);

        // ✅ Set video time
        video.currentTime = newTime;

        setTimeout(() => seekingLock = false, 200);
    });

    // --- Load video ---
    async function loadVideo() {
        try {
            if (!videoContentId) {
                console.error('Thieu videoContentId de tai video');
                return;
            }

            const res = await fetch(`/api/video/watch/${videoContentId}`);
            const videoSrc = await res.text();

            if (Hls.isSupported()) {
                hls = new Hls({
                    enableWorker: true,
                    lowLatencyMode: true,

                    maxBufferLength: 4,
                    maxMaxBufferLength: 8,
                    backBufferLength: 0,

                    maxBufferHole: 0.3,
                    startLevel: 0 // 🔥 load nhanh hơn
                });

                hls.on(Hls.Events.MANIFEST_PARSED, () => {
                    isAutoQuality = true;
                    selectedManualLevel = null;
                    renderQualityOptionsFromHls();
                    updateQualityDisplay();
                    applyResumePosition(true);
                });

                hls.on(Hls.Events.LEVEL_SWITCHED, (_event, data) => {
                    updateQualityDisplay(data.level);
                    refreshQualityActiveState();
                });

                hls.loadSource(videoSrc);
                hls.attachMedia(video);

            } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
                video.src = videoSrc;
                setQualityAvailability(false);
            }

        } catch (err) {
            console.error("Lỗi load video:", err);
        }
    }

    async function syncWatchProgress(isFinalSync = false) {
        if (!videoContentId) {
            return;
        }

        const currentTime = Number.isFinite(video.currentTime) ? video.currentTime : 0;
        const duration = Number.isFinite(video.duration) ? video.duration : 0;
        const payload = {
            videoContentId,
            currentTimeSeconds: currentTime,
            durationSeconds: duration
        };

        lastHistorySyncAt = Date.now();

        if (isFinalSync && navigator.sendBeacon) {
            const body = new Blob([JSON.stringify(payload)], { type: 'application/json' });
            navigator.sendBeacon('/api/v1/history/progress', body);
            return;
        }

        try {
            await fetch('/api/v1/history/progress', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload),
                keepalive: isFinalSync
            });
        } catch (error) {
            console.warn('Khong the dong bo lich su xem', error);
        }
    }

    window.addEventListener('beforeunload', () => {
        syncWatchProgress(true);
        stopHostSyncHeartbeat();
        if (stompClient && wsConnected) {
            try {
                stompClient.disconnect(() => {});
            } catch (error) {
                console.warn('Cannot disconnect room socket', error);
            }
        }
    });

    (async () => {
        const roomReady = await bootstrapRoomMode();
        if (!roomReady) {
            return;
        }
        setupBackButton();
        setupRemotePlayUnlock();
        loadVideo();
        if (isRoomMode) {
            connectRoomSocket();
        }
    })();

const _movieId = new URLSearchParams(window.location.search).get('id') || ((window.watchMovieConfig || {}).videoId || '');
    let _selectedScore = 0;

    // ========== Tabs ==========
    function switchTab(tab) {
        const btns = document.querySelectorAll('.tab-btn');
        btns[0].classList.toggle('active', tab === 'comment');
        btns[1].classList.toggle('active', tab === 'rating');

        document.getElementById('tabComment').classList.toggle('active', tab === 'comment');
        document.getElementById('tabRating').classList.toggle('active', tab === 'rating');

        const title = document.getElementById('tabTitle');
        if (tab === 'comment') {
            title.innerHTML = 'Bình luận (<span id="commentCount">' + (document.getElementById('commentCount')?.innerText || '0') + '</span>)';
        } else {
            title.innerHTML = 'Đánh giá (<span id="ratingTabCount">0</span>)';
            loadRatings();
        }
    }

    // ========== Cuộn xuống bình luận ==========
    function scrollToCommentZone() {
        const zone = document.getElementById('commentZone');
        if (zone) {
            zone.scrollIntoView({ behavior: 'smooth', block: 'start' });
            setTimeout(() => document.getElementById('commentInput').focus(), 400);
        }
    }

    // ========== Fetch thông tin phim ==========
    document.addEventListener('DOMContentLoaded', function () {
        if (_movieId) {
            fetch('/api/movies/' + _movieId)
                .then(res => res.ok ? res.json() : Promise.reject(res.status))
                .then(data => {
                    document.getElementById('m-title').innerText = data.title || 'Không có tên';
                    document.getElementById('m-rating').innerText = data.rating ? data.rating.toFixed(1) : '0';
                })
                .catch(() => {
                    document.getElementById('m-title').innerText = 'Không tải được thông tin phim';
                });

            loadComments();
        }
    });

    // ========== Đếm ký tự ==========
    const commentInput = document.getElementById('commentInput');
    const charCount = document.getElementById('charCount');
    commentInput.addEventListener('input', () => {
        charCount.innerText = commentInput.value.length;
    });

    // ========== Load bình luận ==========
    async function loadComments() {
        if (!_movieId) return;
        try {
            const res = await fetch('/api/v1/comments?videoContentId=' + _movieId);
            const data = await res.json();
            const container = document.getElementById('commentList');
            const countEl = document.getElementById('commentCount');
            if (countEl) countEl.innerText = data.length;

            if (data.length === 0) {
                container.innerHTML = '<p class="no-data-msg">Chưa có bình luận nào. Hãy là người đầu tiên!</p>';
                return;
            }

            container.innerHTML = data.map(c => {
                const repliesHtml = (c.replies && c.replies.length > 0)
                    ? '<div class="reply-list">' + c.replies.map(r => `
                        <div class="reply-item">
                            <div class="reply-avatar">${(r.userName || 'A').charAt(0).toUpperCase()}</div>
                            <div class="reply-body">
                                <div class="reply-user-name">${escHtml(r.userName)}</div>
                                <div class="reply-text">${escHtml(r.content)}</div>
                                <div class="reply-date">${formatDate(r.createdAt)}</div>
                            </div>
                        </div>
                    `).join('') + '</div>'
                    : '';

                return `
                <div class="comment-item">
                    <div class="comment-avatar">${(c.userName || 'A').charAt(0).toUpperCase()}</div>
                    <div class="comment-body">
                        <div class="comment-user-name">${escHtml(c.userName)}</div>
                        <div class="comment-text">${escHtml(c.content)}</div>
                        <div class="comment-actions">
                            <span class="comment-date">${formatDate(c.createdAt)}</span>
                            <button class="reply-btn" onclick="toggleReplyBox('${c.id}')">
                                <i class="fa-solid fa-reply"></i> Trả lời
                            </button>
                        </div>
                        <div class="reply-box" id="replyBox-${c.id}">
                            <textarea id="replyInput-${c.id}" placeholder="Viết phản hồi..." maxlength="500"></textarea>
                            <div class="reply-box-footer">
                                <button class="btn-cancel-reply" onclick="toggleReplyBox('${c.id}')">Hủy</button>
                                <button class="btn-send-reply" onclick="sendReply('${c.id}')">Gửi</button>
                            </div>
                        </div>
                        ${repliesHtml}
                    </div>
                </div>
                `;
            }).join('');
        } catch (e) {
            console.error('Lỗi load bình luận:', e);
        }
    }

    // ========== Gửi bình luận ==========
    async function sendComment() {
        const content = commentInput.value.trim();
        const errorEl = document.getElementById('commentError');
        const successEl = document.getElementById('commentSuccess');
        errorEl.style.display = 'none';
        successEl.style.display = 'none';

        if (!content) {
            errorEl.innerText = 'Bình luận không được để trống';
            errorEl.style.display = 'block';
            return;
        }

        try {
            const res = await fetch('/api/v1/comments', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content, videoContentId: _movieId })
            });

            if (res.ok) {
                successEl.innerText = 'Gửi bình luận thành công!';
                successEl.style.display = 'block';
                commentInput.value = '';
                charCount.innerText = '0';
                loadComments();
            } else {
                const msg = await res.text();
                errorEl.innerText = msg || 'Gửi thất bại. Vui lòng đăng nhập.';
                errorEl.style.display = 'block';
            }
        } catch (err) {
            errorEl.innerText = 'Lỗi kết nối, vui lòng thử lại.';
            errorEl.style.display = 'block';
        }
    }

    // ========== Load đánh giá ==========
    async function loadRatings() {
        if (!_movieId) return;
        try {
            const res = await fetch('/api/v1/ratings?videoContentId=' + _movieId);
            const data = await res.json();
            const container = document.getElementById('ratingList');
            const tabCount = document.getElementById('ratingTabCount');
            if (tabCount) tabCount.innerText = data.totalCount || 0;

            // Cập nhật rating badge trên thanh phim
            document.getElementById('m-rating').innerText = data.averageScore ? data.averageScore.toFixed(1) : '0';

            if (!data.ratings || data.ratings.length === 0) {
                container.innerHTML = '<p class="no-data-msg">Chưa có đánh giá nào.</p>';
                return;
            }

            container.innerHTML = data.ratings.map(r => `
                <div class="rating-item">
                    <div class="comment-avatar">${(r.userName || 'A').charAt(0).toUpperCase()}</div>
                    <div class="comment-body">
                        <div class="comment-user-name">${escHtml(r.userName)}</div>
                        <div class="rating-stars-display">${'★'.repeat(r.score)}${'☆'.repeat(5 - r.score)}</div>
                        ${r.review ? '<div class="review-text">' + escHtml(r.review) + '</div>' : ''}
                        <div class="comment-date">${formatDate(r.createdAt)}</div>
                    </div>
                </div>
            `).join('');
        } catch (e) {
            console.error('Lỗi load đánh giá:', e);
        }
    }

    // ========== Modal Đánh giá ==========
    document.getElementById('btnRating').addEventListener('click', openRatingModal);

    function openRatingModal() {
        const title = document.getElementById('m-title').innerText;
        document.getElementById('ratingModalTitle').innerText = title;
        document.getElementById('ratingModalMsg').innerText = '';
        document.getElementById('reviewInput').value = '';

        // Lấy avg + count
        fetch('/api/v1/ratings?videoContentId=' + _movieId)
            .then(r => r.json())
            .then(data => {
                document.getElementById('ratingModalAvg').innerText = data.averageScore ? data.averageScore.toFixed(1) : '0';
                document.getElementById('ratingModalCount').innerText = data.totalCount || 0;
            }).catch(() => {});

        _selectedScore = 0;
        document.querySelectorAll('.star-picker-item').forEach(el => el.classList.remove('selected'));
        document.getElementById('ratingModalOverlay').classList.add('show');
    }

    function closeRatingModal() {
        document.getElementById('ratingModalOverlay').classList.remove('show');
    }

    function pickStar(score) {
        _selectedScore = score;
        document.querySelectorAll('.star-picker-item').forEach(el => {
            const s = Number(el.dataset.score);
            el.classList.toggle('selected', s <= score);
        });
    }

    async function submitRating() {
        const msgEl = document.getElementById('ratingModalMsg');
        if (_selectedScore < 1) {
            msgEl.style.color = '#ff4444';
            msgEl.innerText = 'Vui lòng chọn số sao!';
            return;
        }

        try {
            const reviewText = document.getElementById('reviewInput').value.trim();
            const res = await fetch('/api/v1/ratings', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ videoContentId: _movieId, score: _selectedScore, review: reviewText || null })
            });

            if (res.ok) {
                const msg = await res.text();
                msgEl.style.color = '#4caf50';
                msgEl.innerText = msg || 'Đánh giá thành công!';
                setTimeout(() => {
                    closeRatingModal();
                    loadRatings();
                }, 1000);
            } else {
                const msg = await res.text();
                msgEl.style.color = '#ff4444';
                msgEl.innerText = msg || 'Đánh giá thất bại. Vui lòng đăng nhập.';
            }
        } catch (err) {
            msgEl.style.color = '#ff4444';
            msgEl.innerText = 'Lỗi kết nối.';
        }
    }
    // ========== Phản hồi bình luận ==========
    function toggleReplyBox(commentId) {
        const box = document.getElementById('replyBox-' + commentId);
        if (box) box.classList.toggle('show');
    }

    async function sendReply(parentCommentId) {
        const input = document.getElementById('replyInput-' + parentCommentId);
        const content = input.value.trim();
        if (!content) return;

        try {
            const res = await fetch('/api/v1/comments/' + parentCommentId + '/replies', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ content: content, videoContentId: _movieId })
            });

            if (res.ok) {
                input.value = '';
                toggleReplyBox(parentCommentId);
                loadComments(); // Reload để hiện phản hồi mới
            } else {
                const msg = await res.text();
                alert(msg || 'Gửi phản hồi thất bại. Vui lòng đăng nhập và có gói dịch vụ.');
            }
        } catch (err) {
            alert('Lỗi kết nối, vui lòng thử lại.');
        }
    }

    // ========== Utils ==========
    function escHtml(str) {
        if (!str) return '';
        return str.replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');
    }

    function formatDate(isoStr) {
        if (!isoStr) return '';
        try {
            const d = new Date(isoStr);
            return d.toLocaleDateString('vi-VN') + ' ' + d.toLocaleTimeString('vi-VN', {hour:'2-digit',minute:'2-digit'});
        } catch { return isoStr; }
    }

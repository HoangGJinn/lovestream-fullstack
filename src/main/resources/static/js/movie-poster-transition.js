(function () {
    if (window.__lsMoviePosterTransitionInit) {
        return;
    }
    window.__lsMoviePosterTransitionInit = true;

    var STORAGE_KEY = "ls_movie_poster_transition_v1";
    var STYLE_ID = "ls-movie-transition-style";
    var LEAVE_CLASS = "ls-movie-transition-leave";
    var WAITING_CLASS = "ls-movie-transition-waiting";
    var LIGHT_SURFACE_CLASS = "ls-page-transition-light";
    var CLICKED_CLASS = "ls-movie-card-clicked";
    var LINK_CLICKED_CLASS = "ls-page-transition-link-clicked";
    var BLOCKER_CLASS = "ls-movie-transition-blocker";
    var PROGRESS_CLASS = "ls-movie-transition-progress";
    var MAX_DATA_AGE_MS = 15000;
    var LEAVE_NAVIGATE_DELAY_MS = 130;
    var SHOW_WAITING_STATE_DELAY_MS = 260;

    var isNavigating = false;
    var activeBlocker = null;
    var waitingStateTimer = null;

    function now() {
        return Date.now();
    }

    function prefersReducedMotion() {
        return window.matchMedia && window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    }

    function ensureTransitionStyle() {
        if (document.getElementById(STYLE_ID)) {
            return;
        }

        var style = document.createElement("style");
        style.id = STYLE_ID;
        style.textContent = [
            "body > *:not(." + BLOCKER_CLASS + ") {",
            "  transform-origin: center top;",
            "}",
            "body." + LEAVE_CLASS + " > *:not(." + BLOCKER_CLASS + ") {",
            "  transition: opacity 170ms ease, transform 210ms cubic-bezier(0.2, 0.8, 0.2, 1);",
            "  will-change: opacity, transform;",
            "}",
            "body." + LEAVE_CLASS + " > .navbar {",
            "  opacity: 0.94;",
            "  transform: scale(0.997);",
            "}",
            "body." + LEAVE_CLASS + " > *:not(.navbar):not(." + BLOCKER_CLASS + ") {",
            "  opacity: 0.28;",
            "  transform: scale(0.97) translateY(4px);",
            "}",
            "body." + WAITING_CLASS + " > *:not(." + BLOCKER_CLASS + ") {",
            "  filter: saturate(0.92) blur(0.4px);",
            "}",
            "html." + LIGHT_SURFACE_CLASS + " ." + BLOCKER_CLASS + " ." + PROGRESS_CLASS + " {",
            "  background: linear-gradient(90deg, #111111, rgba(17, 17, 17, 0.65));",
            "  box-shadow: 0 0 14px rgba(0, 0, 0, 0.22);",
            "}",
            "html." + LIGHT_SURFACE_CLASS + " body." + WAITING_CLASS + " > *:not(." + BLOCKER_CLASS + ") {",
            "  filter: saturate(0.92) blur(0.25px);",
            "}",
            "a.movie-card." + CLICKED_CLASS + " {",
            "  transition: opacity 150ms ease, transform 190ms cubic-bezier(0.2, 0.8, 0.2, 1);",
            "  opacity: 0.74;",
            "  transform: scale(0.97);",
            "}",
            "a." + LINK_CLICKED_CLASS + " {",
            "  transition: opacity 150ms ease, transform 190ms cubic-bezier(0.2, 0.8, 0.2, 1);",
            "  opacity: 0.76;",
            "  transform: translateY(-1px);",
            "}",
            "." + BLOCKER_CLASS + " {",
            "  position: fixed;",
            "  inset: 0;",
            "  pointer-events: all;",
            "  cursor: progress;",
            "  z-index: 2147483590;",
            "  background: transparent;",
            "  opacity: 0;",
            "  transition: opacity 170ms ease;",
            "}",
            "body." + WAITING_CLASS + " ." + BLOCKER_CLASS + " {",
            "  opacity: 1;",
            "}",
            "." + BLOCKER_CLASS + " ." + PROGRESS_CLASS + " {",
            "  position: fixed;",
            "  top: 0;",
            "  left: 0;",
            "  width: 42vw;",
            "  max-width: 320px;",
            "  height: 3px;",
            "  border-radius: 0 999px 999px 0;",
            "  background: linear-gradient(90deg, #ffffff, rgba(255, 255, 255, 0.68));",
            "  box-shadow: 0 0 14px rgba(255, 255, 255, 0.28);",
            "  transform-origin: left center;",
            "  animation: lsMovieTransitionProgress 1s ease-in-out infinite;",
            "}",
            "@keyframes lsMovieTransitionProgress {",
            "  0% { transform: translateX(-58%) scaleX(0.35); opacity: 0.4; }",
            "  55% { transform: translateX(72vw) scaleX(1); opacity: 1; }",
            "  100% { transform: translateX(112vw) scaleX(0.65); opacity: 0.2; }",
            "}",
            "@media (prefers-reduced-motion: reduce) {",
            "  body." + LEAVE_CLASS + " > *:not(." + BLOCKER_CLASS + "),",
            "  a.movie-card." + CLICKED_CLASS + ",",
            "  a." + LINK_CLICKED_CLASS + " {",
            "    transition: none !important;",
            "    transform: none !important;",
            "  }",
            "  ." + BLOCKER_CLASS + " ." + PROGRESS_CLASS + " {",
            "    animation: none !important;",
            "  }",
            "}"
        ].join("\n");

        document.head.appendChild(style);
    }

    function isPrimaryClick(event) {
        return event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey;
    }

    function isLightSurfacePage() {
        var path = window.location.pathname || "";
        return path.startsWith("/account") || path.startsWith("/profile") || path.startsWith("/notifications");
    }

    function applySurfaceTheme() {
        document.documentElement.classList.toggle(LIGHT_SURFACE_CLASS, isLightSurfacePage());
    }

    function isInternalNavigation(anchor) {
        if (!anchor || !anchor.href) {
            return false;
        }

        if (anchor.target === "_blank" || anchor.hasAttribute("download")) {
            return false;
        }

        try {
            var url = new URL(anchor.href, window.location.href);
            return url.origin === window.location.origin;
        } catch (error) {
            return false;
        }
    }

    function parseInternalTarget(anchor) {
        if (!isInternalNavigation(anchor)) {
            return null;
        }

        try {
            var url = new URL(anchor.href, window.location.href);
            var currentPath = window.location.pathname + window.location.search;
            var targetPath = url.pathname + url.search;

            if (targetPath === currentPath) {
                return null;
            }

            return {
                href: url.href,
                pathWithQuery: targetPath,
                path: url.pathname
            };
        } catch (error) {
            return null;
        }
    }

    function parseMovieTarget(anchor) {
        var target = parseInternalTarget(anchor);
        if (!target || !target.path.startsWith("/movies/")) {
            return null;
        }

        return target;
    }

    function isTransitionableLink(anchor) {
        if (!anchor || anchor.tagName !== "A") {
            return false;
        }

        if (anchor.hasAttribute("download") || anchor.target === "_blank") {
            return false;
        }

        if (anchor.getAttribute("href") === "#") {
            return false;
        }

        return parseInternalTarget(anchor) !== null;
    }

    function createInteractionBlocker() {
        var blocker = document.createElement("div");
        blocker.className = BLOCKER_CLASS;
        blocker.setAttribute("aria-hidden", "true");

        var progress = document.createElement("div");
        progress.className = PROGRESS_CLASS;
        blocker.appendChild(progress);
        return blocker;
    }

    function lockUserInteraction() {
        if (activeBlocker && activeBlocker.isConnected) {
            return;
        }

        activeBlocker = createInteractionBlocker();
        document.body.appendChild(activeBlocker);
    }

    function unlockUserInteraction() {
        if (waitingStateTimer) {
            window.clearTimeout(waitingStateTimer);
            waitingStateTimer = null;
        }

        document.body.classList.remove(WAITING_CLASS);
        document.body.classList.remove(LEAVE_CLASS);

        if (activeBlocker && activeBlocker.isConnected) {
            activeBlocker.remove();
        }
        activeBlocker = null;

        // Remove any stray blockers that might exist
        var strayBlockers = document.querySelectorAll("." + BLOCKER_CLASS);
        strayBlockers.forEach(function (el) {
            if (el.isConnected) {
                el.remove();
            }
        });
    }

    function resetNavigationState() {
        isNavigating = false;
        unlockUserInteraction();

        document.querySelectorAll("a." + CLICKED_CLASS + ", a." + LINK_CLICKED_CLASS).forEach(function (el) {
            el.classList.remove(CLICKED_CLASS);
            el.classList.remove(LINK_CLICKED_CLASS);
        });
    }

    function persistTransitionData(target) {
        try {
            sessionStorage.setItem(STORAGE_KEY, JSON.stringify({
                href: target.href,
                pathWithQuery: target.pathWithQuery,
                ts: now()
            }));
        } catch (error) {
            // Ignore storage errors.
        }
    }

    function readTransitionData(removeAfterRead) {
        var raw;

        try {
            raw = sessionStorage.getItem(STORAGE_KEY);
            if (removeAfterRead) {
                sessionStorage.removeItem(STORAGE_KEY);
            }
        } catch (error) {
            return null;
        }

        if (!raw) {
            return null;
        }

        try {
            return JSON.parse(raw);
        } catch (error) {
            return null;
        }
    }

    function isFreshData(data) {
        return data && typeof data.ts === "number" && now() - data.ts <= MAX_DATA_AGE_MS;
    }

    function clearDetailLoadingState() {
        document.documentElement.classList.remove("ls-movie-detail-loading");
    }

    function startLeaveTransition(anchor) {
        if (isNavigating) {
            return true;
        }

        var target = parseInternalTarget(anchor);
        if (!target) {
            return false;
        }

        isNavigating = true;
        lockUserInteraction();
        if (target.path.startsWith("/movies/")) {
            persistTransitionData(target);
            anchor.classList.add(CLICKED_CLASS);
        } else {
            anchor.classList.add(LINK_CLICKED_CLASS);
        }

        window.requestAnimationFrame(function () {
            window.requestAnimationFrame(function () {
                document.body.classList.add(LEAVE_CLASS);
            });
        });

        waitingStateTimer = window.setTimeout(function () {
            document.body.classList.add(WAITING_CLASS);
        }, SHOW_WAITING_STATE_DELAY_MS);

        window.setTimeout(function () {
            window.location.assign(target.href);
        }, LEAVE_NAVIGATE_DELAY_MS);

        return true;
    }

    function bindPosterClickTransition() {
        document.addEventListener("click", function (event) {
            if (prefersReducedMotion()) {
                return;
            }

            if (!isPrimaryClick(event)) {
                return;
            }

            if (isNavigating) {
                event.preventDefault();
                return;
            }

            var anchor = event.target.closest("a[href]");
            if (!isTransitionableLink(anchor)) {
                return;
            }

            event.preventDefault();
            startLeaveTransition(anchor);
        }, true);
    }

    function revealDetailByFade() {
        if (!window.location.pathname.startsWith("/movies/")) {
            clearDetailLoadingState();
            return;
        }

        if (prefersReducedMotion()) {
            readTransitionData(true);
            clearDetailLoadingState();
            return;
        }

        var data = readTransitionData(true);
        if (!isFreshData(data) || !data.pathWithQuery) {
            clearDetailLoadingState();
            return;
        }

        var currentPath = window.location.pathname + window.location.search;
        if (currentPath !== data.pathWithQuery) {
            clearDetailLoadingState();
            return;
        }

        window.requestAnimationFrame(function () {
            window.requestAnimationFrame(function () {
                window.setTimeout(clearDetailLoadingState, 70);
            });
        });
    }

    ensureTransitionStyle();
    applySurfaceTheme();
    bindPosterClickTransition();

    if (document.documentElement.classList.contains("ls-movie-detail-loading")) {
        window.setTimeout(clearDetailLoadingState, 2400);
    }

    if (document.readyState === "loading") {
        document.addEventListener("DOMContentLoaded", revealDetailByFade);
    } else {
        revealDetailByFade();
    }

    window.addEventListener("pagehide", function () {
        unlockUserInteraction();
    });

    // Clean up blocker and state when returning to this page (browser back)
    window.addEventListener("pageshow", function (event) {
        if (event.persisted) {
            // Page was restored from bfcache, reset all state
            resetNavigationState();
        }
    });

    // Clean up any stray blocker and state on initial page load
    window.addEventListener("load", function () {
        resetNavigationState();
    });
})();

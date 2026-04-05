(function () {
    'use strict';

    var DEFAULT_LIMIT = 8;

    function posterOrFallback(url) {
        if (url && String(url).trim()) return url;
        return 'data:image/svg+xml;charset=UTF-8,' + encodeURIComponent(
            '<svg xmlns="http://www.w3.org/2000/svg" width="150" height="150">'
            + '<rect width="150%" height="100%" fill="#111"/>'
            + '<text x="50%" y="50%" dominant-baseline="middle" text-anchor="middle" fill="#666" font-size="12" font-family="Arial">No poster</text>'
            + '</svg>'
        );
    }

    function buildDetailUrl(item) {
        // API returns VideoContentSearchItemResponse
        // - series: /series/{id}
        // - movie : /movies/{slugOrId}
        if (item && item.type === 'series') {
            return '/series/' + encodeURIComponent(item.id);
        }
        var slugOrId = (item && (item.slugOrId || item.slug || item.id)) || '';
        return '/movies/' + encodeURIComponent(slugOrId);
    }

    function ensureSuggestionBox(form) {
        var existing = form.querySelector('.search-suggest-box');
        if (existing) return existing;

        var div = document.createElement('div');
        div.className = 'search-suggest-box';
        div.setAttribute('data-role', 'search-suggest-box');
        form.appendChild(div);
        return div;
    }

    function setupOne(form) {
        if (!form || form.getAttribute('data-suggest-ready') === 'true') return;

        var input = form.querySelector('input[name="keyword"].nav-search-input, input[name="keyword"]');
        if (!input) return;

        var toggle = form.querySelector('.nav-search-toggle');
        var suggestionBox = ensureSuggestionBox(form);

        // anchor dropdown
        if (!form.style.position) {
            form.style.position = 'relative';
        }

        var debounceTimer;
        var requestSeq = 0;
        var abortController;

        function clearSuggestions() {
            suggestionBox.innerHTML = '';
            suggestionBox.classList.remove('is-open');
        }

        function openSuggestions() {
            suggestionBox.classList.add('is-open');
        }

        function renderLoading() {
            suggestionBox.innerHTML = '';
            var div = document.createElement('div');
            div.className = 'suggest-loading';
            div.textContent = 'Đang tìm kiếm...';
            suggestionBox.appendChild(div);
            openSuggestions();
        }

        function renderEmpty() {
            suggestionBox.innerHTML = '';
            var div = document.createElement('div');
            div.className = 'suggest-empty';
            div.textContent = 'Không tìm thấy phim';
            suggestionBox.appendChild(div);
            openSuggestions();
        }

        function fetchSuggestions() {
            var value = (input.value || '').trim();
            if (value.length < 2) {
                if (abortController) abortController.abort();
                clearSuggestions();
                return;
            }

            if (abortController) abortController.abort();
            abortController = new AbortController();
            var currentSeq = ++requestSeq;
            renderLoading();

            var url = '/videocontents/search?format=json&keyword='
                + encodeURIComponent(value)
                + '&size=' + encodeURIComponent(String(DEFAULT_LIMIT));

            fetch(url, {
                headers: { 'Accept': 'application/json' },
                signal: abortController.signal
            })
                .then(function (res) { return res.json(); })
                .then(function (payload) {
                    if (currentSeq !== requestSeq) return;

                    var data = payload && payload.data ? payload.data : [];
                    if (!data.length) {
                        renderEmpty();
                        return;
                    }

                    suggestionBox.innerHTML = '';
                    var list = document.createElement('div');
                    list.className = 'suggest-list';

                    data.slice(0, DEFAULT_LIMIT).forEach(function (item) {
                        var btn = document.createElement('button');
                        btn.type = 'button';
                        btn.className = 'suggest-item';
                        btn.setAttribute('data-href', buildDetailUrl(item));

                        var thumb = document.createElement('img');
                        thumb.className = 'suggest-thumb';
                        thumb.alt = item.title || '';
                        thumb.src = posterOrFallback(item.posterUrl);
                        thumb.loading = 'lazy';
                        thumb.referrerPolicy = 'no-referrer';

                        var info = document.createElement('div');
                        info.className = 'suggest-info';

                        var title = document.createElement('div');
                        title.className = 'suggest-title';
                        title.textContent = item.title || '';

                        var meta = document.createElement('div');
                        meta.className = 'suggest-meta';
                        var year = item.releaseYear ? String(item.releaseYear) : '';
                        var typeLabel = item.type === 'series' ? 'Phim bộ' : 'Phim lẻ';
                        meta.textContent = (year ? (year + ' • ') : '') + typeLabel;

                        info.appendChild(title);
                        info.appendChild(meta);

                        btn.appendChild(thumb);
                        btn.appendChild(info);
                        list.appendChild(btn);
                    });

                    suggestionBox.appendChild(list);
                    openSuggestions();
                })
                .catch(function (err) {
                    if (err && err.name === 'AbortError') return;
                    clearSuggestions();
                });
        }

        input.addEventListener('input', function () {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(fetchSuggestions, 300);
        });

        // click to go detail
        suggestionBox.addEventListener('click', function (event) {
            var target = event.target.closest('[data-href]');
            if (!target) return;
            var href = target.getAttribute('data-href');
            if (href) window.location.href = href;
        });

        // hide when clicking outside
        document.addEventListener('click', function (event) {
            if (!form.contains(event.target) && !suggestionBox.contains(event.target)) {
                clearSuggestions();
            }
        });

        // hide on ESC
        document.addEventListener('keydown', function (event) {
            if (event.key === 'Escape') {
                clearSuggestions();
            }
        });

        // keep old toggle UX intact (open input / submit)
        if (toggle) {
            toggle.addEventListener('click', function () {
                if (!form.classList.contains('is-open')) {
                    form.classList.add('is-open');
                    input.focus();
                    return;
                }
                var term = (input.value || '').trim();
                if (term.length > 0) {
                    input.value = term;
                    form.submit();
                } else {
                    input.focus();
                }
            });
        }

        form.setAttribute('data-suggest-ready', 'true');
    }

    function setupAll() {
        var forms = document.querySelectorAll('form.nav-search');
        for (var i = 0; i < forms.length; i++) {
            setupOne(forms[i]);
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', setupAll);
    } else {
        setupAll();
    }
})();


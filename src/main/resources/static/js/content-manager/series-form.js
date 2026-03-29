(function () {
    var confirmMessage = 'Dữ liệu chưa được lưu. Bạn có chắc chắn muốn thoát?';
    var dirtyForms = Array.prototype.slice.call(document.querySelectorAll('.js-dirty-form'));
    var initialSnapshots = new Map();
    var intentionalNavigation = false;

    function serializeForm(form) {
        var data = new FormData(form);
        var entries = [];

        data.forEach(function (value, key) {
            if (value instanceof File) {
                entries.push(key + ':' + value.name + ':' + value.size);
                return;
            }
            entries.push(key + ':' + value);
        });

        return entries.join('|');
    }

    function hasUnsavedChanges() {
        if (intentionalNavigation) {
            return false;
        }

        return dirtyForms.some(function (form) {
            return serializeForm(form) !== initialSnapshots.get(form);
        });
    }

    function confirmLeave() {
        return !hasUnsavedChanges() || window.confirm(confirmMessage);
    }

    function navigateTo(url) {
        if (!url) {
            return;
        }

        if (!confirmLeave()) {
            return;
        }

        intentionalNavigation = true;
        window.location.href = url;
    }

    function setupPosterValidation() {
        var posterInput = document.getElementById('seriesFile');
        var posterError = document.getElementById('seriesPosterClientError');

        if (!posterInput || !posterError) {
            return;
        }

        var maxBytes = Number(posterInput.dataset.maxBytes || '0');
        var allowedTypes = (posterInput.dataset.allowedTypes || '')
            .split(',')
            .map(function (item) { return item.trim().toLowerCase(); })
            .filter(Boolean);
        var allowedExtensions = (posterInput.dataset.allowedExtensions || '')
            .split(',')
            .map(function (item) { return item.trim().toLowerCase(); })
            .filter(Boolean);

        function showPosterError(message) {
            posterError.hidden = false;
            posterError.textContent = message;
        }

        function clearPosterError() {
            posterError.hidden = true;
            posterError.textContent = '';
        }

        function validatePosterFile(file) {
            if (!file) {
                return '';
            }

            if (maxBytes > 0 && file.size > maxBytes) {
                return 'Poster không được vượt quá 5MB.';
            }

            var fileType = (file.type || '').toLowerCase();
            var fileName = (file.name || '').toLowerCase();
            var validType = allowedTypes.length === 0 || allowedTypes.indexOf(fileType) >= 0;
            var validExtension = allowedExtensions.length === 0 || allowedExtensions.some(function (extension) {
                return fileName.endsWith(extension);
            });

            if (!validType && !validExtension) {
                return 'Poster chỉ hỗ trợ định dạng JPG, PNG hoặc WEBP.';
            }

            return '';
        }

        posterInput.addEventListener('change', function () {
            var file = posterInput.files && posterInput.files.length > 0 ? posterInput.files[0] : null;
            var validationMessage = validatePosterFile(file);

            if (!validationMessage) {
                clearPosterError();
                return;
            }

            showPosterError(validationMessage);
            posterInput.value = '';
        });

        if (posterInput.form) {
            posterInput.form.addEventListener('submit', function (event) {
                var file = posterInput.files && posterInput.files.length > 0 ? posterInput.files[0] : null;
                var validationMessage = validatePosterFile(file);

                if (!validationMessage) {
                    clearPosterError();
                    return;
                }

                event.preventDefault();
                showPosterError(validationMessage);
            });
        }
    }

    setupPosterValidation();

    dirtyForms.forEach(function (form) {
        initialSnapshots.set(form, serializeForm(form));
        form.addEventListener('submit', function (event) {
            if (!event.defaultPrevented) {
                intentionalNavigation = true;
            }
        });
    });

    Array.prototype.slice.call(document.querySelectorAll('.js-cancel-navigation')).forEach(function (button) {
        button.addEventListener('click', function () {
            navigateTo(button.dataset.cancelUrl);
        });
    });

    Array.prototype.slice.call(document.querySelectorAll('.js-guarded-navigation')).forEach(function (link) {
        link.addEventListener('click', function (event) {
            event.preventDefault();
            navigateTo(link.getAttribute('href'));
        });
    });

    window.addEventListener('beforeunload', function (event) {
        if (!hasUnsavedChanges()) {
            return;
        }

        event.preventDefault();
        event.returnValue = '';
    });
})();

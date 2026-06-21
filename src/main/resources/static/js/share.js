document.addEventListener("DOMContentLoaded", function() {
    const popUp = document.getElementById("sharePopUp");
    const closeBtn = document.getElementById("closePopUp");
    
    // Tìm tất cả các loại nút kích hoạt có thể có
    const triggers = [
        document.getElementById("btn-share-trigger"),
        document.getElementById("btn-share-watch-trigger"),
        document.getElementById("openShareBtn")
    ];

    triggers.forEach(trigger => {
        if (trigger) {
            trigger.onclick = function() { 
                if (popUp) popUp.style.display = "flex"; 
            };
        }
    });

    if (closeBtn) {
        closeBtn.onclick = function() { popUp.style.display = "none"; };
    }

    // Đóng khi click ngoài vùng modal
    window.addEventListener("click", function(event) {
        if (event.target == popUp) { popUp.style.display = "none"; }
    });
});

// Hàm copy link
function doCopyLink() {
    let url = window.location.href;

    if (window.watchMovieConfig && window.watchMovieConfig.videoId) {
        const payload = {
            videoId: window.watchMovieConfig.videoId,
            platform: 'COPY_LINK'
        };
        fetch('/api/v1/share', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        })
        .then(res => res.json())
        .then(data => {
            if (data.success && data.shareLink) {
                url = data.shareLink;
            }
            performCopy(url);
        })
        .catch(err => {
            console.error("Error logging share activity:", err);
            performCopy(url);
        });
    } else {
        performCopy(url);
    }
}

function performCopy(url) {
    navigator.clipboard.writeText(url).then(() => {
        const toast = document.getElementById("copyToast");
        if (toast) {
            toast.className = "copy-toast show";
            setTimeout(() => { toast.className = "copy-toast"; }, 3000);
        }
        const popUp = document.getElementById("sharePopUp");
        if (popUp) popUp.style.display = "none";
    });
}

// Hàm chia sẻ mạng xã hội
function doShare(platform) {
    const popUp = document.getElementById("sharePopUp");
    if (popUp) popUp.style.display = "none";

    // Mở một tab trống trước để tránh bị trình duyệt chặn popup do gọi bất đồng bộ
    const popupWindow = window.open('about:blank', '_blank', 'width=600,height=450');
    if (popupWindow) {
        popupWindow.document.write('Đang tạo liên kết chia sẻ...');
    }

    let url = window.location.href;
    let title = document.title;

    if (window.watchMovieConfig && window.watchMovieConfig.videoId) {
        const payload = {
            videoId: window.watchMovieConfig.videoId,
            platform: platform.toUpperCase()
        };
        fetch('/api/v1/share', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        })
        .then(res => res.json())
        .then(data => {
            if (data.success && data.shareLink) {
                url = data.shareLink;
            }
            redirectShareWindow(popupWindow, platform, url, title);
        })
        .catch(err => {
            console.error("Error logging share activity:", err);
            redirectShareWindow(popupWindow, platform, url, title);
        });
    } else {
        redirectShareWindow(popupWindow, platform, url, title);
    }
}

function redirectShareWindow(popupWindow, platform, url, title) {
    const encodedUrl = encodeURIComponent(url);
    const encodedTitle = encodeURIComponent(title);
    let shareUrl = "";

    switch(platform) {
        case 'facebook': shareUrl = `https://www.facebook.com/sharer/sharer.php?u=${encodedUrl}`; break;
        case 'twitter': shareUrl = `https://twitter.com/intent/tweet?url=${encodedUrl}&text=${encodedTitle}`; break;
        case 'telegram': shareUrl = `https://t.me/share/url?url=${encodedUrl}&text=${encodedTitle}`; break;
        case 'reddit': shareUrl = `https://www.reddit.com/submit?url=${encodedUrl}&title=${encodedTitle}`; break;
        case 'messenger': 
            // KIỂM TRA NẾU LÀ DI ĐỘNG -> MỞ APP MESSENGER
            if (/Android|iPhone|iPad|iPod/i.test(navigator.userAgent)) {
                shareUrl = `fb-messenger://share/?link=${encodedUrl}`;
            } else {
                // TRÊN WEB: Sử dụng Send Dialog với App ID vừa tạo
                // Dùng trang chủ làm redirect_uri để tránh lỗi URL quá dài hoặc không hợp lệ trên localhost
                const redirectUri = encodeURIComponent('http://localhost:8080/');
                shareUrl = `https://www.facebook.com/dialog/send?link=${encodedUrl}&app_id=1443806240774598&redirect_uri=${redirectUri}`;
            }
            break;
    }

    if (shareUrl && popupWindow) {
        popupWindow.location.href = shareUrl;
    } else if (popupWindow) {
        popupWindow.close();
    }
}

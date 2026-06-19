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
    const url = window.location.href;
    navigator.clipboard.writeText(url).then(() => {
        const toast = document.getElementById("copyToast");
        if (toast) {
            toast.className = "copy-toast show";
            setTimeout(() => { toast.className = "copy-toast"; }, 3000);
        }
        logShareActivity('COPY_LINK');
        const popUp = document.getElementById("sharePopUp");
        if (popUp) popUp.style.display = "none";
    });
}

// Hàm chia sẻ mạng xã hội
function doShare(platform) {
    const url = encodeURIComponent(window.location.href);
    const title = encodeURIComponent(document.title);
    let shareUrl = "";

    switch(platform) {
        case 'facebook': shareUrl = `https://www.facebook.com/sharer/sharer.php?u=${url}`; break;
        case 'twitter': shareUrl = `https://twitter.com/intent/tweet?url=${url}&text=${title}`; break;
        case 'telegram': shareUrl = `https://t.me/share/url?url=${url}&text=${title}`; break;
        case 'reddit': shareUrl = `https://www.reddit.com/submit?url=${url}&title=${title}`; break;
        case 'messenger': 
            // KIỂM TRA NẾU LÀ DI ĐỘNG -> MỞ APP MESSENGER
            if (/Android|iPhone|iPad|iPod/i.test(navigator.userAgent)) {
                shareUrl = `fb-messenger://share/?link=${url}`;
            } else {
                // TRÊN WEB: Sử dụng Send Dialog với App ID vừa tạo
                // Dùng trang chủ làm redirect_uri để tránh lỗi URL quá dài hoặc không hợp lệ trên localhost
                const redirectUri = encodeURIComponent('http://localhost:8080/');
                shareUrl = `https://www.facebook.com/dialog/send?link=${url}&app_id=1443806240774598&redirect_uri=${redirectUri}`;
            }
            break;
    }

    if (shareUrl) {
        // Ghi nhận lịch sử xuống Backend trước khi mở popup
        logShareActivity(platform);
        
        window.open(shareUrl, '_blank', 'width=600,height=450');
        const popUp = document.getElementById("sharePopUp");
        if (popUp) popUp.style.display = "none";
    }
}

// Hàm gửi request xuống backend ghi nhận lịch sử share
function logShareActivity(platform) {
    // Chỉ ghi nhận nếu có videoId (ở trang xem phim)
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
            console.log("Log share activity:", data.message);
        })
        .catch(err => {
            console.error("Error logging share activity:", err);
        });
    }
}

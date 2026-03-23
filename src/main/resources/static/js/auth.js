async function handleLogout() {
    if (confirm('Bạn có chắc chắn muốn đăng xuất khỏi LoveStream?')) {
        try {
            const res = await fetch('/api/v1/auth/logout', { method: 'POST' });
            if (res.ok) {
                // Xóa token ở client nếu bạn có lưu trong localStorage/cookie thủ công
                window.location.href = '/login';
            } else {
                const errorData = await res.json();
                alert('Lỗi server: ' + (errorData.message || 'Không thể kết nối database'));
            }
        } catch (e) {
            alert('Lỗi mạng hoặc server không phản hồi!');
            console.error(e);
        }
    }
}